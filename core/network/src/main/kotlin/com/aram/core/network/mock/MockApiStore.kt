package com.aram.core.network.mock

import com.aram.core.network.dto.AccountDto
import com.aram.core.network.dto.AccountRefDto
import com.aram.core.network.dto.AuthResponseDto
import com.aram.core.network.dto.BudgetDto
import com.aram.core.network.dto.BudgetItemDto
import com.aram.core.network.dto.BudgetOverviewDto
import com.aram.core.network.dto.CategoryRefDto
import com.aram.core.network.dto.ChangePasswordRequest
import com.aram.core.network.dto.ContributeGoalRequest
import com.aram.core.network.dto.ConversionDto
import com.aram.core.network.dto.ConvertCurrencyRequest
import com.aram.core.network.dto.CreateGoalRequest
import com.aram.core.network.dto.CurrencyDto
import com.aram.core.network.dto.DashboardDto
import com.aram.core.network.dto.DashboardMonthDto
import com.aram.core.network.dto.DashboardOwnerDto
import com.aram.core.network.dto.DashboardStatsDto
import com.aram.core.network.dto.DashboardSyncDto
import com.aram.core.network.dto.DeleteAccountRequest
import com.aram.core.network.dto.ForgotPasswordRequest
import com.aram.core.network.dto.ForgotPasswordResponseDto
import com.aram.core.network.dto.GoalDto
import com.aram.core.network.dto.LocaleDto
import com.aram.core.network.dto.LoginRequest
import com.aram.core.network.dto.NotificationDto
import com.aram.core.network.dto.NotificationGroupDto
import com.aram.core.network.dto.NotificationListDto
import com.aram.core.network.dto.NotificationReadDto
import com.aram.core.network.dto.ProfileDto
import com.aram.core.network.dto.ProfileStatsDto
import com.aram.core.network.dto.RegisterRequest
import com.aram.core.network.dto.ResetPasswordRequest
import com.aram.core.network.dto.SafeToSpendDto
import com.aram.core.network.dto.SaveBudgetRequest
import com.aram.core.network.dto.SaveTransactionRequest
import com.aram.core.network.dto.SettingsDto
import com.aram.core.network.dto.SyncPullDto
import com.aram.core.network.dto.SyncPushRequest
import com.aram.core.network.dto.SyncPushResultDto
import com.aram.core.network.dto.SyncTransactionItem
import com.aram.core.network.dto.TransactionDayGroupDto
import com.aram.core.network.dto.TransactionDto
import com.aram.core.network.dto.TransactionListDto
import com.aram.core.network.dto.TransactionSummaryDto
import com.aram.core.network.dto.UpdateBudgetRequest
import com.aram.core.network.dto.UpdateGoalRequest
import com.aram.core.network.dto.UpdateProfileRequest
import com.aram.core.network.dto.UpdateSettingsRequest
import com.aram.core.network.dto.WeekDayDto
import com.aram.core.network.dto.WeeklyInsightDto
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/** Result of a mock operation; the engine turns it into an HTTP response. */
sealed interface MockOutcome<out T> {
    data class Ok<T>(val data: T, val status: Int = 200) : MockOutcome<T>

    data class Error(
        val status: Int,
        val message: String,
        val fields: Map<String, List<String>> = emptyMap(),
    ) : MockOutcome<Nothing>
}

/** Seeded account the alpha welcome screen points testers at. */
object MockDemoAccount {
    const val EMAIL = "demo@aram.app"
    const val PASSWORD = "demo1234"
}

/**
 * In-memory implementation of the Aram API rules: fixed category catalog,
 * IRR ledger with entered-currency metadata, UTC months, last-write-wins sync
 * and per-user isolation. Pure Kotlin so it is unit-testable without Ktor.
 */
class MockApiStore(    private val clock: Clock = Clock.systemUTC(),
    seedDemo: Boolean = true,
) {

    val categories: List<CategoryRefDto> = CATEGORY_SEEDS.map { seed ->
        CategoryRefDto(
            id = uuid("category:${seed.slug}"),
            slug = seed.slug,
            name = seed.name,
            iconKey = seed.icon,
            tone = seed.tone,
            kind = seed.kind,
        )
    }

    private val categoryBySlug = categories.associateBy { it.slug }
    private val users = LinkedHashMap<String, UserData>()
    private var userSequence = 0

    init {
        if (seedDemo) {
            val demo = createUser(MockDemoAccount.EMAIL, MockDemoAccount.PASSWORD, "Ava Bennett", "IRR")
            seedLedger(demo)
        }
    }

    // ---------------------------------------------------------------- auth

    @Synchronized
    fun register(request: RegisterRequest): MockOutcome<AuthResponseDto> {
        val email = request.email.trim().lowercase(Locale.US)
        val fields = mutableMapOf<String, List<String>>()
        if (!email.contains("@") || email.length < 5) fields["email"] = listOf("Enter a valid email address.")
        if (request.displayName.isBlank()) fields["displayName"] = listOf("Display name is required.")
        if (!validPassword(request.password)) {
            fields["password"] = listOf("Use at least 8 characters with a lowercase letter and a digit.")
        }
        val home = request.homeCurrency?.uppercase(Locale.US)
        if (home != null && currencyInfo(home) == null) fields["homeCurrency"] = listOf("Unsupported currency.")
        if (fields.isNotEmpty()) return MockOutcome.Error(400, "Validation failed.", fields)
        if (users.values.any { it.email == email }) {
            return MockOutcome.Error(400, "That email is already registered.", mapOf("email" to listOf("Already registered.")))
        }

        val user = createUser(email, request.password, request.displayName.trim(), home ?: "IRR")
        seedWelcomeNotification(user)
        return MockOutcome.Ok(authResponse(user), status = 201)
    }

    @Synchronized
    fun login(request: LoginRequest): MockOutcome<AuthResponseDto> {
        val user = users.values.firstOrNull {
            it.email == request.email.trim().lowercase(Locale.US) && it.password == request.password
        } ?: return MockOutcome.Error(401, "Email or password is incorrect.")
        return MockOutcome.Ok(authResponse(user))
    }

    @Synchronized
    fun forgotPassword(request: ForgotPasswordRequest): MockOutcome<ForgotPasswordResponseDto> {
        val known = users.values.any { it.email == request.email.trim().lowercase(Locale.US) }
        val token = if (known) RESET_TOKEN else null
        return MockOutcome.Ok(
            ForgotPasswordResponseDto(
                message = "If the address exists, reset instructions are on their way.",
                developmentToken = token,
            ),
        )
    }

    @Synchronized
    fun resetPassword(request: ResetPasswordRequest): MockOutcome<Boolean> {
        if (request.token != RESET_TOKEN) return MockOutcome.Error(400, "That reset token is not valid.")
        if (!validPassword(request.newPassword)) {
            return MockOutcome.Error(400, "Validation failed.", mapOf("newPassword" to listOf(BAD_PASSWORD)))
        }
        val user = users.values.firstOrNull { it.email == request.email.trim().lowercase(Locale.US) }
            ?: return MockOutcome.Error(400, "That reset token is not valid.")
        user.password = request.newPassword
        return MockOutcome.Ok(true)
    }

    @Synchronized
    fun changePassword(token: String?, request: ChangePasswordRequest): MockOutcome<Boolean> {
        val user = userFor(token) ?: return UNAUTHORIZED
        if (user.password != request.currentPassword) return MockOutcome.Error(400, "Password is incorrect.")
        if (!validPassword(request.newPassword)) {
            return MockOutcome.Error(400, "Validation failed.", mapOf("newPassword" to listOf(BAD_PASSWORD)))
        }
        user.password = request.newPassword
        return MockOutcome.Ok(true)
    }

    @Synchronized
    fun me(token: String?): MockOutcome<ProfileDto> {
        val user = userFor(token) ?: return UNAUTHORIZED
        return MockOutcome.Ok(profile(user))
    }

    @Synchronized
    fun updateProfile(token: String?, request: UpdateProfileRequest): MockOutcome<ProfileDto> {
        val user = userFor(token) ?: return UNAUTHORIZED
        val error = applyPreferences(
            user,
            request.displayName,
            request.homeCurrency,
            request.secondaryDisplay,
            request.language,
            request.appearance,
            request.quietHours,
        )
        if (error != null) return error
        return MockOutcome.Ok(profile(user))
    }

    @Synchronized
    fun deleteAccount(token: String?, request: DeleteAccountRequest): MockOutcome<Boolean> {
        val user = userFor(token) ?: return UNAUTHORIZED
        if (!request.confirm.equals("DELETE", ignoreCase = true)) {
            return MockOutcome.Error(400, "Type DELETE to confirm.")
        }
        if (!request.password.isNullOrEmpty() && request.password != user.password) {
            return MockOutcome.Error(400, "Password is incorrect.")
        }
        users.remove(user.id)
        return MockOutcome.Ok(true)
    }

    // ------------------------------------------------------------- profile

    @Synchronized
    fun profileStats(token: String?): MockOutcome<ProfileStatsDto> {
        val user = userFor(token) ?: return UNAUTHORIZED
        return MockOutcome.Ok(
            ProfileStatsDto(
                transactions = user.transactions.size,
                budgets = user.budgets.size,
                goals = user.goals.count { !it.archived },
            ),
        )
    }

    @Synchronized
    fun accounts(token: String?): MockOutcome<List<AccountDto>> {
        val user = userFor(token) ?: return UNAUTHORIZED
        return MockOutcome.Ok(user.accounts.sortedByDescending { it.isDefault })
    }

    @Synchronized
    fun exportCsv(token: String?): MockOutcome<String> {
        val user = userFor(token) ?: return UNAUTHORIZED
        val rows = buildString {
            appendLine("Date,Kind,Category,Account,Note,AmountIrr,EnteredCurrency,EnteredAmount,FxRate")
            user.transactions.sortedByDescending { it.occurredAt }.forEach { tx ->
                val account = tx.accountId?.let { id -> user.accounts.firstOrNull { it.id == id }?.name }
                appendLine(
                    listOf(
                        tx.occurredAt.toString(),
                        tx.kind,
                        categoryBySlug[tx.categorySlug]?.name.orEmpty(),
                        account.orEmpty(),
                        tx.note.orEmpty(),
                        tx.amountIrr.toLong().toString(),
                        tx.enteredCurrency,
                        tx.enteredAmount.toLong().toString(),
                        tx.fxRate?.toLong()?.toString().orEmpty(),
                    ).joinToString(",") { csvCell(it) },
                )
            }
        }
        return MockOutcome.Ok(rows)
    }

    // ----------------------------------------------------------- categories

    fun categories(kind: String?): MockOutcome<List<CategoryRefDto>> {
        val normalized = kind?.lowercase(Locale.US) ?: "all"
        val filtered = when (normalized) {
            "expense", "income" -> categories.filter { it.kind == normalized }
            else -> categories
        }
        return MockOutcome.Ok(filtered)
    }

    // --------------------------------------------------------- transactions

    @Synchronized
    fun listTransactions(
        token: String?,
        year: Int?,
        month: Int?,
        kind: String?,
        page: Int,
        pageSize: Int,
        search: String?,
    ): MockOutcome<TransactionListDto> {
        val user = userFor(token) ?: return UNAUTHORIZED
        val ym = yearMonth(year, month) ?: return BAD_MONTH
        val kindFilter = kind?.lowercase(Locale.US)?.takeIf { it != "all" }
        val searchText = search?.trim()?.lowercase(Locale.US)?.takeIf { it.isNotEmpty() }

        val monthRows = user.transactions
            .filter { it.inMonth(ym) }
            .filter { kindFilter == null || it.kind == kindFilter }
            .filter {
                searchText == null ||
                    it.note?.lowercase(Locale.US)?.contains(searchText) == true ||
                    categoryBySlug[it.categorySlug]?.name?.lowercase(Locale.US)?.contains(searchText) == true
            }
            .sortedByDescending { it.occurredAt }

        val size = pageSize.coerceIn(1, 100)
        val currentPage = page.coerceAtLeast(1)
        val pages = if (monthRows.isEmpty()) 0 else (monthRows.size + size - 1) / size
        val pageRows = monthRows.drop((currentPage - 1) * size).take(size)

        val groups = pageRows.groupBy { it.occurredAt.atZone(ZoneOffset.UTC).toLocalDate() }
            .entries
            .sortedByDescending { it.key }
            .map { (date, rows) ->
                TransactionDayGroupDto(
                    date = date.toString(),
                    label = dayLabel(date),
                    totalIrr = rows.sumOf { if (it.kind == "income") it.amountIrr else -it.amountIrr },
                    items = rows.map { transactionDto(user, it) },
                )
            }

        val spent = monthRows.filter { it.kind == "expense" }.sumOf { it.amountIrr }
        val income = monthRows.filter { it.kind == "income" }.sumOf { it.amountIrr }
        val previousSpent = user.transactions
            .filter { it.kind == "expense" && it.inMonth(ym.minusMonths(1)) }
            .sumOf { it.amountIrr }

        return MockOutcome.Ok(
            TransactionListDto(
                year = ym.year,
                month = ym.monthValue,
                page = currentPage,
                pageSize = size,
                totalCount = monthRows.size,
                totalPages = pages,
                totalSpentIrr = spent,
                totalIncomeIrr = income,
                spentDeltaPercent = deltaPercent(spent, previousSpent),
                groups = groups,
            ),
        )
    }

    @Synchronized
    fun transactionSummary(token: String?, year: Int?, month: Int?): MockOutcome<TransactionSummaryDto> {
        val user = userFor(token) ?: return UNAUTHORIZED
        val ym = yearMonth(year, month) ?: return BAD_MONTH
        val rows = user.transactions.filter { it.inMonth(ym) }
        val spent = rows.filter { it.kind == "expense" }.sumOf { it.amountIrr }
        val income = rows.filter { it.kind == "income" }.sumOf { it.amountIrr }
        val previousSpent = user.transactions
            .filter { it.kind == "expense" && it.inMonth(ym.minusMonths(1)) }
            .sumOf { it.amountIrr }
        val daysInMonth = ym.lengthOfMonth()
        val daysLeft = daysLeft(ym)
        val remaining = max(0.0, income - spent)
        return MockOutcome.Ok(
            TransactionSummaryDto(
                year = ym.year,
                month = ym.monthValue,
                spentIrr = spent,
                incomeIrr = income,
                netIrr = income - spent,
                spentDeltaPercent = deltaPercent(spent, previousSpent),
                transactionCount = rows.size,
                daysInMonth = daysInMonth,
                daysLeft = daysLeft,
                dailyAllowanceIrr = if (daysLeft > 0) (remaining / daysLeft).roundToLong().toDouble() else 0.0,
            ),
        )
    }

    @Synchronized
    fun weeklyInsight(token: String?): MockOutcome<WeeklyInsightDto> {
        val user = userFor(token) ?: return UNAUTHORIZED
        return MockOutcome.Ok(weeklyFor(user))
    }

    private fun weeklyFor(user: UserData): WeeklyInsightDto {
        val today = LocalDate.now(clock)
        val start = today.minusDays(6)
        val days = (0..6).map { offset ->
            val date = start.plusDays(offset.toLong())
            val spent = user.transactions
                .filter { it.kind == "expense" && it.occurredAt.atZone(ZoneOffset.UTC).toLocalDate() == date }
                .sumOf { it.amountIrr }
            WeekDayDto(
                date = date.toString(),
                label = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH),
                amountIrr = spent,
                isToday = date == today,
            )
        }
        return WeeklyInsightDto(
            weekStart = start.atStartOfDay().toInstant(ZoneOffset.UTC).toString(),
            spentIrr = days.sumOf { it.amountIrr },
            deltaPercent = null,
            days = days,
        )
    }

    @Synchronized
    fun createTransaction(token: String?, request: SaveTransactionRequest): MockOutcome<TransactionDto> {
        val user = userFor(token) ?: return UNAUTHORIZED
        val validated = when (val outcome = validateTransaction(user, request)) {
            is MockOutcome.Error -> return outcome
            is MockOutcome.Ok -> outcome.data
        }

        val now = Instant.now(clock)
        val occurred = parseInstant(request.occurredAt) ?: now
        val tx = StoredTransaction(
            id = UUID.randomUUID().toString(),
            kind = validated.kind,
            amountIrr = validated.amountIrr,
            enteredCurrency = validated.enteredCurrency,
            enteredAmount = validated.enteredAmount,
            fxRate = validated.fxRate,
            categorySlug = validated.categorySlug,
            accountId = validated.accountId,
            note = request.note?.trim()?.takeIf { it.isNotEmpty() },
            occurredAt = occurred,
            clientUpdatedAt = parseInstant(request.clientUpdatedAt) ?: now,
            createdAt = now,
        )
        user.transactions += tx
        return MockOutcome.Ok(transactionDto(user, tx), status = 201)
    }

    @Synchronized
    fun updateTransaction(token: String?, id: String, request: SaveTransactionRequest): MockOutcome<TransactionDto> {
        val user = userFor(token) ?: return UNAUTHORIZED
        val existing = user.transactions.firstOrNull { it.id == id } ?: return NOT_FOUND_TX
        val validated = when (val outcome = validateTransaction(user, request)) {
            is MockOutcome.Error -> return outcome
            is MockOutcome.Ok -> outcome.data
        }

        val updated = StoredTransaction(
            id = existing.id,
            kind = validated.kind,
            amountIrr = validated.amountIrr,
            enteredCurrency = validated.enteredCurrency,
            enteredAmount = validated.enteredAmount,
            fxRate = validated.fxRate,
            categorySlug = validated.categorySlug,
            accountId = validated.accountId,
            note = request.note?.trim()?.takeIf { it.isNotEmpty() },
            occurredAt = parseInstant(request.occurredAt) ?: existing.occurredAt,
            clientUpdatedAt = parseInstant(request.clientUpdatedAt) ?: Instant.now(clock),
            createdAt = existing.createdAt,
        )
        user.transactions[user.transactions.indexOf(existing)] = updated
        return MockOutcome.Ok(transactionDto(user, updated))
    }

    @Synchronized
    fun deleteTransaction(token: String?, id: String): MockOutcome<Boolean> {
        val user = userFor(token) ?: return UNAUTHORIZED
        val existing = user.transactions.firstOrNull { it.id == id } ?: return NOT_FOUND_TX
        user.transactions.remove(existing)
        return MockOutcome.Ok(true)
    }

    // -------------------------------------------------------------- budgets

    @Synchronized
    fun budgetOverview(token: String?, year: Int?, month: Int?): MockOutcome<BudgetOverviewDto> {
        val user = userFor(token) ?: return UNAUTHORIZED
        val ym = yearMonth(year, month) ?: return BAD_MONTH
        val items = budgetItems(user, ym)
        val totalLimit = items.sumOf { it.limitIrr }
        val totalSpent = items.sumOf { it.spentIrr }
        val remaining = items.sumOf { it.remainingIrr }
        val (status, label) = budgetStatus(totalSpent, totalLimit)
        val daysLeft = daysLeft(ym)
        val resetsOn = ym.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC).toString()
        return MockOutcome.Ok(
            BudgetOverviewDto(
                year = ym.year,
                month = ym.monthValue,
                totalLimitIrr = totalLimit,
                totalSpentIrr = totalSpent,
                totalRemainingIrr = remaining,
                percentUsed = percent(totalSpent, totalLimit),
                status = status,
                statusLabel = label,
                daysLeft = daysLeft,
                perDayLeftIrr = if (daysLeft > 0) (max(0.0, remaining) / daysLeft).roundToLong().toDouble() else 0.0,
                resetsOn = resetsOn,
                categories = items,
            ),
        )
    }

    @Synchronized
    fun budgets(token: String?): MockOutcome<List<BudgetDto>> {
        val user = userFor(token) ?: return UNAUTHORIZED
        return MockOutcome.Ok(user.budgets.map { budgetDto(it) })
    }

    fun budgetCategories(): MockOutcome<List<CategoryRefDto>> =
        MockOutcome.Ok(categories.filter { it.kind == "expense" })

    @Synchronized
    fun createBudget(token: String?, request: SaveBudgetRequest): MockOutcome<BudgetDto> {
        val user = userFor(token) ?: return UNAUTHORIZED
        if (request.monthlyLimitIrr <= 0) {
            return MockOutcome.Error(400, "Validation failed.", mapOf("monthlyLimitIrr" to listOf("Must be greater than zero.")))
        }
        val category = categoryBySlug[request.categorySlug?.lowercase(Locale.US)]
            ?: categories.firstOrNull { it.id == request.categoryId }
            ?: return MockOutcome.Error(400, "Send categoryId or categorySlug.")
        if (category.kind != "expense") return MockOutcome.Error(400, "Budgets only track expense categories.")
        val existing = user.budgets.firstOrNull { it.categoryId == category.id }
        if (existing != null) return MockOutcome.Error(409, "A budget already exists for ${category.name}.")
        val budget = StoredBudget(
            id = UUID.randomUUID().toString(),
            categoryId = category.id,
            limitIrr = request.monthlyLimitIrr,
            rollover = request.rollover,
            createdAt = Instant.now(clock),
            updatedAt = Instant.now(clock),
        )
        user.budgets += budget
        return MockOutcome.Ok(budgetDto(budget), status = 201)
    }

    @Synchronized
    fun updateBudget(token: String?, id: String, request: UpdateBudgetRequest): MockOutcome<BudgetDto> {
        val user = userFor(token) ?: return UNAUTHORIZED
        if (request.monthlyLimitIrr <= 0) {
            return MockOutcome.Error(400, "Validation failed.", mapOf("monthlyLimitIrr" to listOf("Must be greater than zero.")))
        }
        val budget = user.budgets.firstOrNull { it.id == id } ?: return NOT_FOUND_BUDGET
        budget.limitIrr = request.monthlyLimitIrr
        budget.rollover = request.rollover
        budget.updatedAt = Instant.now(clock)
        return MockOutcome.Ok(budgetDto(budget))
    }

    @Synchronized
    fun deleteBudget(token: String?, id: String): MockOutcome<Boolean> {
        val user = userFor(token) ?: return UNAUTHORIZED
        val budget = user.budgets.firstOrNull { it.id == id } ?: return NOT_FOUND_BUDGET
        user.budgets.remove(budget)
        return MockOutcome.Ok(true)
    }

    // ---------------------------------------------------------------- goals

    @Synchronized
    fun goals(token: String?, includeArchived: Boolean): MockOutcome<List<GoalDto>> {
        val user = userFor(token) ?: return UNAUTHORIZED
        return MockOutcome.Ok(
            user.goals
                .filter { includeArchived || !it.archived }
                .map { goalDto(it) },
        )
    }

    @Synchronized
    fun createGoal(token: String?, request: CreateGoalRequest): MockOutcome<GoalDto> {
        val user = userFor(token) ?: return UNAUTHORIZED
        val fields = mutableMapOf<String, List<String>>()
        if (request.name.isBlank() || request.name.length > 80) fields["name"] = listOf("Name is required (max 80).")
        if (request.targetIrr <= 0) fields["targetIrr"] = listOf("Target must be greater than zero.")
        if (fields.isNotEmpty()) return MockOutcome.Error(400, "Validation failed.", fields)

        val now = Instant.now(clock)
        val goal = StoredGoal(
            id = UUID.randomUUID().toString(),
            name = request.name.trim(),
            targetIrr = request.targetIrr,
            iconKey = request.iconKey?.takeIf { it.isNotBlank() } ?: "goal",
            targetDate = parseInstant(request.targetDate),
            updatedAt = now,
        )
        val opening = request.savedIrr ?: 0.0
        if (opening > 0) goal.contributions += Contribution(now, opening)
        user.goals += goal
        return MockOutcome.Ok(goalDto(goal), status = 201)
    }

    @Synchronized
    fun updateGoal(token: String?, id: String, request: UpdateGoalRequest): MockOutcome<GoalDto> {
        val user = userFor(token) ?: return UNAUTHORIZED
        val goal = user.goals.firstOrNull { it.id == id } ?: return NOT_FOUND_GOAL
        if (request.targetIrr != null && request.targetIrr <= 0) {
            return MockOutcome.Error(400, "Validation failed.", mapOf("targetIrr" to listOf("Target must be greater than zero.")))
        }
        request.name?.takeIf { it.isNotBlank() }?.let { goal.name = it.trim().take(80) }
        request.targetIrr?.let { goal.targetIrr = it }
        request.iconKey?.takeIf { it.isNotBlank() }?.let { goal.iconKey = it }
        if (request.targetDate != null) goal.targetDate = parseInstant(request.targetDate)
        request.isArchived?.let { goal.archived = it }
        goal.updatedAt = Instant.now(clock)
        return MockOutcome.Ok(goalDto(goal))
    }

    @Synchronized
    fun deleteGoal(token: String?, id: String): MockOutcome<Boolean> {
        val user = userFor(token) ?: return UNAUTHORIZED
        val goal = user.goals.firstOrNull { it.id == id } ?: return NOT_FOUND_GOAL
        user.goals.remove(goal)
        return MockOutcome.Ok(true)
    }

    @Synchronized
    fun contribute(token: String?, id: String, request: ContributeGoalRequest): MockOutcome<GoalDto> {
        val user = userFor(token) ?: return UNAUTHORIZED
        if (request.amount <= 0) {
            return MockOutcome.Error(400, "Validation failed.", mapOf("amount" to listOf("Must be greater than zero.")))
        }
        val goal = user.goals.firstOrNull { it.id == id } ?: return NOT_FOUND_GOAL
        val currency = request.currency?.uppercase(Locale.US) ?: user.homeCurrency
        val info = currencyInfo(currency)
            ?: return MockOutcome.Error(400, "Validation failed.", mapOf("currency" to listOf("Unsupported currency.")))
        goal.contributions += Contribution(
            occurredAt = parseInstant(request.occurredAt) ?: Instant.now(clock),
            amountIrr = toIrr(request.amount, info),
        )
        goal.updatedAt = Instant.now(clock)
        return MockOutcome.Ok(goalDto(goal))
    }

    // -------------------------------------------------------- notifications

    @Synchronized
    fun notifications(token: String?, scope: String?): MockOutcome<NotificationListDto> {
        val user = userFor(token) ?: return UNAUTHORIZED
        val unreadOnly = scope?.lowercase(Locale.US) == "unread"
        val visible = user.notifications
            .filter { !unreadOnly || !it.isRead }
            .sortedByDescending { it.occurredAt }
        val groups = visible.groupBy { it.groupLabel(clock) }
            .map { (label, rows) -> NotificationGroupDto(label, rows.map { notificationDto(it) }) }
        return MockOutcome.Ok(
            NotificationListDto(
                scope = if (unreadOnly) "unread" else "all",
                unreadCount = user.notifications.count { !it.isRead },
                groups = groups,
            ),
        )
    }

    @Synchronized
    fun markNotification(token: String?, id: String, read: Boolean): MockOutcome<NotificationReadDto> {
        val user = userFor(token) ?: return UNAUTHORIZED
        val notification = user.notifications.firstOrNull { it.id == id }
            ?: return MockOutcome.Error(404, "Notification not found.")
        notification.isRead = read
        return MockOutcome.Ok(
            NotificationReadDto(id = id, isRead = read, unreadCount = user.notifications.count { !it.isRead }),
        )
    }

    @Synchronized
    fun markAllNotificationsRead(token: String?): MockOutcome<Int> {
        val user = userFor(token) ?: return UNAUTHORIZED
        val changed = user.notifications.count { !it.isRead }
        user.notifications.forEach { it.isRead = true }
        return MockOutcome.Ok(changed)
    }

    // ------------------------------------------------------------ dashboard

    @Synchronized
    fun dashboard(token: String?, year: Int?, month: Int?): MockOutcome<DashboardDto> {
        val user = userFor(token) ?: return UNAUTHORIZED
        val ym = yearMonth(year, month) ?: return BAD_MONTH
        val items = budgetItems(user, ym)
        val totalLimit = items.sumOf { it.limitIrr }
        val spent = user.transactions
            .filter { it.kind == "expense" && it.inMonth(ym) }
            .sumOf { it.amountIrr }
        val income = user.transactions
            .filter { it.kind == "income" && it.inMonth(ym) }
            .sumOf { it.amountIrr }
        val remaining = items.sumOf { it.remainingIrr }
        val safe = if (totalLimit > 0) min(max(0.0, remaining), max(0.0, income - spent)) else max(0.0, income - spent)
        val previousSpent = user.transactions
            .filter { it.kind == "expense" && it.inMonth(ym.minusMonths(1)) }
            .sumOf { it.amountIrr }
        val daysLeft = daysLeft(ym)
        val saved = user.goals.flatMap { it.contributions }
            .filter { it.occurredAt.inMonth(ym) }
            .sumOf { it.amountIrr }
        val savedPrevious = user.goals.flatMap { it.contributions }
            .filter { it.occurredAt.inMonth(ym.minusMonths(1)) }
            .sumOf { it.amountIrr }
        val (status, label) = budgetStatus(spent, totalLimit)

        return MockOutcome.Ok(
            DashboardDto(
                owner = DashboardOwnerDto(
                    displayName = user.displayName,
                    firstName = user.firstName,
                    initials = initials(user.displayName),
                    homeCurrency = user.homeCurrency,
                    language = user.language,
                ),
                month = DashboardMonthDto(
                    year = ym.year,
                    month = ym.monthValue,
                    daysInMonth = ym.lengthOfMonth(),
                    daysLeft = daysLeft,
                ),
                safeToSpend = SafeToSpendDto(
                    amountIrr = safe,
                    dailyAllowanceIrr = if (daysLeft > 0) (safe / daysLeft).roundToLong().toDouble() else 0.0,
                    percentOfBudget = percent(safe, totalLimit),
                    status = status,
                    statusLabel = label,
                ),
                stats = DashboardStatsDto(
                    spentIrr = spent,
                    spentDeltaPercent = deltaPercent(spent, previousSpent),
                    savedIrr = saved,
                    savedDeltaIrr = saved - savedPrevious,
                ),
                budgets = items,
                recentTransactions = user.transactions
                    .sortedByDescending { it.occurredAt }
                    .take(5)
                    .map { transactionDto(user, it) },
                week = weeklyFor(user),
                unreadNotifications = user.notifications.count { !it.isRead },
                sync = DashboardSyncDto(lastSyncedAt = user.lastSyncedAt, offlineFirst = true),
            ),
        )
    }

    // ------------------------------------------------------------- settings

    @Synchronized
    fun settings(token: String?): MockOutcome<SettingsDto> {
        val user = userFor(token) ?: return UNAUTHORIZED
        return MockOutcome.Ok(settingsDto(user))
    }

    @Synchronized
    fun updateSettings(token: String?, request: UpdateSettingsRequest): MockOutcome<SettingsDto> {
        val user = userFor(token) ?: return UNAUTHORIZED
        val error = applyPreferences(
            user,
            request.displayName,
            request.homeCurrency,
            request.secondaryDisplay,
            request.language,
            request.appearance,
            request.quietHours,
        )
        if (error != null) return error
        return MockOutcome.Ok(settingsDto(user))
    }

    fun currencies(): MockOutcome<List<CurrencyDto>> = MockOutcome.Ok(CURRENCIES.map { currencyDto(it) })

    fun locales(): MockOutcome<List<LocaleDto>> = MockOutcome.Ok(LOCALES)

    fun convert(request: ConvertCurrencyRequest): MockOutcome<ConversionDto> {
        val from = currencyInfo(request.from.uppercase(Locale.US))
            ?: return MockOutcome.Error(400, "Validation failed.", mapOf("from" to listOf("Unsupported source currency.")))
        val to = currencyInfo(request.to.uppercase(Locale.US))
            ?: return MockOutcome.Error(400, "Validation failed.", mapOf("to" to listOf("Unsupported target currency.")))
        if (request.amount < 0) {
            return MockOutcome.Error(400, "Validation failed.", mapOf("amount" to listOf("Must be zero or more.")))
        }
        val rate = from.rate / to.rate
        val result = roundTo(request.amount * rate, to.decimals)
        return MockOutcome.Ok(
            ConversionDto(
                from = from.code,
                to = to.code,
                amount = request.amount,
                result = result,
                rate = rate,
                updatedAt = ratesUpdatedAt().toString(),
            ),
        )
    }

    // ----------------------------------------------------------------- sync

    @Synchronized
    fun syncPush(token: String?, request: SyncPushRequest): MockOutcome<SyncPushResultDto> {
        val user = userFor(token) ?: return UNAUTHORIZED
        val applied = mutableListOf<String>()
        val skipped = mutableListOf<String>()

        request.items.forEach { item ->
            val result = applySyncItem(user, item)
            if (result) applied += item.id else skipped += item.id
        }

        val now = Instant.now(clock)
        user.lastSyncedAt = now.toString()
        return MockOutcome.Ok(
            SyncPushResultDto(
                applied = applied,
                skipped = skipped,
                serverTime = now.toString(),
                lastSyncedAt = user.lastSyncedAt,
            ),
        )
    }

    @Synchronized
    fun syncPull(token: String?, since: String?): MockOutcome<SyncPullDto> {
        val user = userFor(token) ?: return UNAUTHORIZED
        val sinceInstant = parseInstant(since)
        val now = Instant.now(clock)

        val transactions = user.transactions
            .filter { sinceInstant == null || !it.clientUpdatedAt.isBefore(sinceInstant) }
            .sortedByDescending { it.occurredAt }
            .take(500)
            .map { transactionDto(user, it) }
        val budgets = user.budgets
            .filter { sinceInstant == null || !it.updatedAt.isBefore(sinceInstant) }
            .map { budgetDto(it) }
        val goals = user.goals
            .filter { sinceInstant == null || !it.updatedAt.isBefore(sinceInstant) }
            .map { goalDto(it) }

        user.lastSyncedAt = now.toString()
        return MockOutcome.Ok(
            SyncPullDto(
                serverTime = now.toString(),
                lastSyncedAt = user.lastSyncedAt,
                transactions = transactions,
                budgets = budgets,
                goals = goals,
                settings = settingsDto(user),
            ),
        )
    }

    private fun applySyncItem(user: UserData, item: SyncTransactionItem): Boolean {
        val kind = item.kind.lowercase(Locale.US)
        if (kind != "expense" && kind != "income") return false
        val category = categoryBySlug[item.categorySlug.lowercase(Locale.US)] ?: return false
        if (category.kind != kind) return false
        val accountId = item.accountId?.takeIf { id -> user.accounts.any { it.id == id } }
        val enteredCurrency = item.enteredCurrency?.uppercase(Locale.US) ?: user.homeCurrency
        val info = currencyInfo(enteredCurrency) ?: return false
        val amountIrr = when {
            item.amountIrr > 0 -> item.amountIrr
            (item.enteredAmount ?: 0.0) > 0 -> toIrr(item.enteredAmount ?: 0.0, info)
            else -> return false
        }
        val now = Instant.now(clock)
        val clientUpdated = parseInstant(item.clientUpdatedAt) ?: now
        val existing = user.transactions.firstOrNull { it.id == item.id }

        if (existing != null && clientUpdated.isBefore(existing.clientUpdatedAt)) return false

        val tx = StoredTransaction(
            id = item.id,
            kind = kind,
            amountIrr = amountIrr,
            enteredCurrency = enteredCurrency,
            enteredAmount = item.enteredAmount ?: amountIrr,
            fxRate = item.fxRate ?: info.rate.takeIf { it != 1.0 },
            categorySlug = category.slug,
            accountId = accountId,
            note = item.note?.trim()?.takeIf { it.isNotEmpty() },
            occurredAt = parseInstant(item.occurredAt) ?: existing?.occurredAt ?: now,
            clientUpdatedAt = clientUpdated,
            createdAt = existing?.createdAt ?: now,
        )
        if (existing == null) {
            user.transactions += tx
        } else {
            user.transactions[user.transactions.indexOf(existing)] = tx
        }
        return true
    }

    // ------------------------------------------------------------- helpers

    private fun userFor(token: String?): UserData? {
        if (token == null || !token.startsWith(TOKEN_PREFIX)) return null
        return users[token.removePrefix(TOKEN_PREFIX)]
    }

    private fun createUser(email: String, password: String, displayName: String, homeCurrency: String): UserData {
        userSequence++
        val user = UserData(
            id = uuid("user:$email:$userSequence"),
            email = email.lowercase(Locale.US),
            password = password,
            displayName = displayName,
            homeCurrency = homeCurrency,
        )
        user.accounts += listOf(
            AccountDto(uuid("account:${user.id}:cash"), "Cash", "Melli", "cash", isDefault = true),
            AccountDto(uuid("account:${user.id}:card"), "Card", "Melli", "card"),
            AccountDto(uuid("account:${user.id}:auto"), "Auto-pay", "Melli", "auto"),
        )
        users[user.id] = user
        return user
    }

    private fun authResponse(user: UserData) = AuthResponseDto(
        token = TOKEN_PREFIX + user.id,
        expiresAtUtc = Instant.now(clock).plusSeconds(24 * 60 * 60).toString(),
        profile = profile(user),
    )

    private fun profile(user: UserData) = ProfileDto(
        id = uuid("profile:${user.id}"),
        displayName = user.displayName,
        firstName = user.firstName,
        email = user.email,
        avatarInitials = initials(user.displayName),
        homeCurrency = user.homeCurrency,
        secondaryDisplay = user.secondaryDisplay,
        language = user.language,
        appearance = user.appearance,
        quietHours = user.quietHours,
        lastSyncedAt = user.lastSyncedAt,
        stats = ProfileStatsDto(
            transactions = user.transactions.size,
            budgets = user.budgets.size,
            goals = user.goals.count { !it.archived },
        ),
    )

    private fun applyPreferences(
        user: UserData,
        displayName: String?,
        homeCurrency: String?,
        secondaryDisplay: Boolean?,
        language: String?,
        appearance: String?,
        quietHours: String?,
    ): MockOutcome.Error? {
        if (displayName != null && (displayName.isBlank() || displayName.length > 120)) {
            return MockOutcome.Error(400, "Validation failed.", mapOf("displayName" to listOf("Max 120 characters.")))
        }
        homeCurrency?.let {
            currencyInfo(it.uppercase(Locale.US))
                ?: return MockOutcome.Error(400, "Validation failed.", mapOf("homeCurrency" to listOf("Unsupported currency.")))
        }
        language?.let {
            if (LOCALES.none { locale -> locale.code == it.lowercase(Locale.US) }) {
                return MockOutcome.Error(400, "Validation failed.", mapOf("language" to listOf("Supported languages are en, fa and fr.")))
            }
        }
        appearance?.let {
            if (it !in setOf("system", "light", "dark")) {
                return MockOutcome.Error(400, "Validation failed.", mapOf("appearance" to listOf("Appearance must be system, light or dark.")))
            }
        }
        if (quietHours != null && quietHours.length > 20) {
            return MockOutcome.Error(400, "Validation failed.", mapOf("quietHours" to listOf("Max 20 characters.")))
        }

        displayName?.trim()?.takeIf { it.isNotEmpty() }?.let { user.displayName = it }
        homeCurrency?.let { user.homeCurrency = it.uppercase(Locale.US) }
        secondaryDisplay?.let { user.secondaryDisplay = it }
        language?.let { user.language = it.lowercase(Locale.US) }
        appearance?.let { user.appearance = it.lowercase(Locale.US) }
        quietHours?.let { user.quietHours = it }
        return null
    }

    private fun validateTransaction(
        user: UserData,
        request: SaveTransactionRequest,
    ): MockOutcome<ValidatedTransaction> {
        val kind = request.kind.lowercase(Locale.US)
        if (kind != "expense" && kind != "income") {
            return MockOutcome.Error(400, "Validation failed.", mapOf("kind" to listOf("Kind must be expense or income.")))
        }
        if (request.amount <= 0) {
            return MockOutcome.Error(400, "Validation failed.", mapOf("amount" to listOf("Must be greater than zero.")))
        }
        if (request.note != null && request.note.length > 80) {
            return MockOutcome.Error(400, "Validation failed.", mapOf("note" to listOf("Max 80 characters.")))
        }
        val currency = request.currency?.uppercase(Locale.US) ?: user.homeCurrency
        val info = currencyInfo(currency)
            ?: return MockOutcome.Error(400, "Validation failed.", mapOf("currency" to listOf("Unsupported currency.")))
        val slug = request.categorySlug?.lowercase(Locale.US)
            ?: if (kind == "income") "salary" else "other"
        val category = categoryBySlug[slug]
            ?: return MockOutcome.Error(400, "Validation failed.", mapOf("categorySlug" to listOf("Unknown category '$slug'.")))
        if (category.kind != kind) {
            return MockOutcome.Error(400, "Validation failed.", mapOf("categorySlug" to listOf("Category does not match kind.")))
        }
        val accountId = request.accountId?.let { id ->
            user.accounts.firstOrNull { it.id == id }
                ?: return MockOutcome.Error(400, "Validation failed.", mapOf("accountId" to listOf("Unknown account.")))
        }?.id

        return MockOutcome.Ok(
            ValidatedTransaction(
                kind = kind,
                categorySlug = category.slug,
                accountId = accountId,
                amountIrr = toIrr(request.amount, info),
                enteredCurrency = info.code,
                enteredAmount = request.amount,
                fxRate = info.rate.takeIf { it != 1.0 },
            ),
        )
    }

    private fun transactionDto(user: UserData, tx: StoredTransaction): TransactionDto {
        val category = categoryBySlug.getValue(tx.categorySlug)
        val account = tx.accountId?.let { id -> user.accounts.firstOrNull { it.id == id } }
        return TransactionDto(
            id = tx.id,
            kind = tx.kind,
            amountIrr = tx.amountIrr,
            category = category,
            account = account?.let { AccountRefDto(it.id, it.name, it.institution, it.kind) },
            note = tx.note,
            occurredAt = tx.occurredAt.toString(),
            enteredCurrency = tx.enteredCurrency,
            enteredAmount = tx.enteredAmount,
            fxRate = tx.fxRate,
        )
    }

    private fun budgetDto(budget: StoredBudget): BudgetDto {
        val category = categories.firstOrNull { it.id == budget.categoryId }
        return BudgetDto(
            id = budget.id,
            categoryId = budget.categoryId,
            categorySlug = category?.slug.orEmpty(),
            categoryName = category?.name.orEmpty(),
            iconKey = category?.iconKey.orEmpty(),
            monthlyLimitIrr = budget.limitIrr,
            rollover = budget.rollover,
        )
    }

    private fun budgetItems(user: UserData, month: YearMonth): List<BudgetItemDto> =
        user.budgets.mapNotNull { budget ->
            val category = categories.firstOrNull { it.id == budget.categoryId } ?: return@mapNotNull null
            val spent = user.transactions
                .filter { it.kind == "expense" && it.categorySlug == category.slug && it.inMonth(month) }
                .sumOf { it.amountIrr }
            val (status, label) = budgetStatus(spent, budget.limitIrr)
            BudgetItemDto(
                id = budget.id,
                categoryId = category.id,
                categorySlug = category.slug,
                categoryName = category.name,
                iconKey = category.iconKey,
                limitIrr = budget.limitIrr,
                spentIrr = spent,
                remainingIrr = budget.limitIrr - spent,
                percentUsed = percent(spent, budget.limitIrr),
                status = status,
                statusLabel = label,
                rollover = budget.rollover,
            )
        }

    private fun goalDto(goal: StoredGoal): GoalDto {
        val saved = goal.contributions.sumOf { it.amountIrr }
        return GoalDto(
            id = goal.id,
            name = goal.name,
            targetIrr = goal.targetIrr,
            savedIrr = saved,
            remainingIrr = max(0.0, goal.targetIrr - saved),
            percent = percent(saved, goal.targetIrr),
            targetDate = goal.targetDate?.toString(),
            iconKey = goal.iconKey,
            isReached = saved >= goal.targetIrr,
            isArchived = goal.archived,
        )
    }

    private fun settingsDto(user: UserData) = SettingsDto(
        displayName = user.displayName,
        email = user.email,
        avatarInitials = initials(user.displayName),
        homeCurrency = user.homeCurrency,
        secondaryDisplay = user.secondaryDisplay,
        language = user.language,
        appearance = user.appearance,
        quietHours = user.quietHours,
        version = "0.1.0",
        privacy = "https://aram.app/privacy",
        ratesUpdatedAt = ratesUpdatedAt().toString(),
        currencies = CURRENCIES.map { currencyDto(it) },
        locales = LOCALES,
    )

    private fun notificationDto(notification: StoredNotification) = NotificationDto(
        id = notification.id,
        type = notification.type,
        title = notification.title,
        body = notification.body,
        iconTone = notification.iconTone,
        isRead = notification.isRead,
        occurredAt = notification.occurredAt.toString(),
        relativeTime = relativeTime(notification.occurredAt),
    )

    private fun seedWelcomeNotification(user: UserData) {
        user.notifications += StoredNotification(
            id = UUID.randomUUID().toString(),
            type = "welcome",
            title = "Welcome to Aram",
            body = "Add an income and your first expense to see a daily safe-to-spend number.",
            iconTone = "ok",
            isRead = false,
            occurredAt = Instant.now(clock).minusSeconds(2 * 60 * 60),
        )
    }

    private fun seedLedger(user: UserData) {
        val today = LocalDate.now(clock)
        val months = listOf(YearMonth.from(today), YearMonth.from(today).minusMonths(1), YearMonth.from(today).minusMonths(2))
        months.forEachIndexed { monthIndex, ym ->
            SEED_ROWS.forEachIndexed { index, row ->
                if (monthIndex == 0 && row.day > today.dayOfMonth) return@forEachIndexed
                val date = ym.atDay(row.day.coerceAtMost(ym.lengthOfMonth()))
                val occurred = date.atTime(9 + (index % 8), (index * 7) % 60).toInstant(ZoneOffset.UTC)
                val account = user.accounts[(index + monthIndex) % user.accounts.size]
                user.transactions += StoredTransaction(
                    id = UUID.randomUUID().toString(),
                    kind = row.kind,
                    amountIrr = row.amount,
                    enteredCurrency = "IRR",
                    enteredAmount = row.amount,
                    fxRate = null,
                    categorySlug = row.slug,
                    accountId = account.id,
                    note = row.note,
                    occurredAt = occurred,
                    clientUpdatedAt = occurred,
                    createdAt = occurred,
                )
            }
        }

        val groceries = categories.first { it.slug == "groceries" }
        val dining = categories.first { it.slug == "dining" }
        val transport = categories.first { it.slug == "transport" }
        val now = Instant.now(clock)
        user.budgets += StoredBudget("budget-groceries", groceries.id, 12_000_000.0, false, now, now)
        user.budgets += StoredBudget("budget-dining", dining.id, 6_000_000.0, false, now, now)
        user.budgets += StoredBudget("budget-transport", transport.id, 4_000_000.0, true, now, now)

        val emergency = StoredGoal(
            id = "goal-emergency",
            name = "Emergency fund",
            targetIrr = 200_000_000.0,
            iconKey = "goal",
            targetDate = null,
            updatedAt = now.minusSeconds(30L * 24 * 60 * 60),
        )
        emergency.contributions += Contribution(now.minusSeconds(30L * 24 * 60 * 60), 75_000_000.0)
        val trip = StoredGoal(
            id = "goal-istanbul",
            name = "Istanbul trip",
            targetIrr = 120_000_000.0,
            iconKey = "travel",
            targetDate = LocalDate.now(clock).plusMonths(8).atStartOfDay().toInstant(ZoneOffset.UTC),
            updatedAt = now.minusSeconds(10L * 24 * 60 * 60),
        )
        trip.contributions += Contribution(now.minusSeconds(10L * 24 * 60 * 60), 20_000_000.0)
        user.goals += listOf(emergency, trip)

        user.notifications += StoredNotification(
            id = UUID.randomUUID().toString(),
            type = "budget",
            title = "Dining is close to its limit",
            body = "You have used most of the Dining budget this month.",
            iconTone = "warn",
            isRead = true,
            occurredAt = now.minusSeconds(26L * 60 * 60),
        )
    }

    private fun yearMonth(year: Int?, month: Int?): YearMonth? {
        if (year == null && month == null) return YearMonth.now(clock)
        if (year == null || month == null) return null
        if (month !in 1..12) return null
        return runCatching { YearMonth.of(year, month) }.getOrNull()
    }

    private fun daysLeft(ym: YearMonth): Int {
        val today = YearMonth.now(clock)
        return when {
            ym.isAfter(today) -> ym.lengthOfMonth()
            ym.equals(today) -> ym.lengthOfMonth() - LocalDate.now(clock).dayOfMonth + 1
            else -> 0
        }
    }

    private fun StoredTransaction.inMonth(ym: YearMonth): Boolean =
        occurredAt.atZone(ZoneOffset.UTC).toLocalDate().let { YearMonth.from(it) == ym }

    private fun Instant.inMonth(ym: YearMonth): Boolean =
        atZone(ZoneOffset.UTC).toLocalDate().let { YearMonth.from(it) == ym }

    private fun dayLabel(date: LocalDate): String {
        val today = LocalDate.now(clock)
        return when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> date.format(DAY_LABEL)
        }
    }

    private fun relativeTime(instant: Instant): String {
        val minutes = java.time.Duration.between(instant, Instant.now(clock)).toMinutes()
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            minutes < 24 * 60 -> "${minutes / 60}h ago"
            minutes < 48 * 60 -> "Yesterday"
            else -> "${minutes / (24 * 60)}d ago"
        }
    }

    private fun StoredNotification.groupLabel(clock: Clock): String {
        val date = occurredAt.atZone(ZoneOffset.UTC).toLocalDate()
        val today = LocalDate.now(clock)
        return when {
            date == today -> "Today"
            date == today.minusDays(1) -> "Yesterday"
            else -> "Earlier"
        }
    }

    private fun ratesUpdatedAt(): Instant = Instant.now(clock).minusSeconds(3L * 24 * 60 * 60)

    private fun budgetStatus(spent: Double, limit: Double): Pair<String, String> = when {
        limit <= 0 -> "onTrack" to "On track"
        spent > limit -> "over" to "Over limit"
        spent >= limit * 0.8 -> "nearLimit" to "Close to limit"
        else -> "onTrack" to "On track"
    }

    private fun percent(part: Double, total: Double): Double =
        if (total <= 0) 0.0 else (part / total * 100).roundToLong().toDouble()

    private fun deltaPercent(current: Double, previous: Double): Double? =
        if (previous <= 0) null else ((current - previous) / previous * 100).roundToLong().toDouble()

    private fun toIrr(amount: Double, info: CurrencyInfo): Double = amount * info.rate

    private fun roundTo(value: Double, decimals: Int): Double {
        val factor = Math.pow(10.0, decimals.toDouble())
        return (value * factor).roundToLong() / factor
    }

    private fun currencyInfo(code: String): CurrencyInfo? = CURRENCIES.firstOrNull { it.code == code }

    private fun currencyDto(info: CurrencyInfo) = CurrencyDto(
        code = info.code,
        decimals = info.decimals,
        symbol = info.symbol,
        rateToIrr = info.rate,
        names = mapOf("en" to info.englishName),
    )

    private fun initials(name: String): String = name.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }

    private fun parseInstant(value: String?): Instant? =
        value?.takeIf { it.isNotBlank() }?.let { runCatching { Instant.parse(it) }.getOrNull() }

    private fun validPassword(password: String): Boolean =
        password.length >= 8 && password.any { it.isLowerCase() } && password.any { it.isDigit() }

    private fun csvCell(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' }) "\"${value.replace("\"", "\"\"")}\"" else value

    private fun uuid(seed: String): String =
        UUID.nameUUIDFromBytes(seed.toByteArray(Charsets.UTF_8)).toString()

    private class UserData(
        val id: String,
        var email: String,
        var password: String,
        var displayName: String,
        var homeCurrency: String,
        var language: String = "en",
        var appearance: String = "system",
        var secondaryDisplay: Boolean = true,
        var quietHours: String = "22:00-07:00",
        var lastSyncedAt: String? = null,
    ) {
        val firstName: String get() = displayName.trim().substringBefore(' ')
        val accounts = mutableListOf<AccountDto>()
        val transactions = mutableListOf<StoredTransaction>()
        val budgets = mutableListOf<StoredBudget>()
        val goals = mutableListOf<StoredGoal>()
        val notifications = mutableListOf<StoredNotification>()
    }

    private class StoredTransaction(
        val id: String,
        val kind: String,
        val amountIrr: Double,
        val enteredCurrency: String,
        val enteredAmount: Double,
        val fxRate: Double?,
        val categorySlug: String,
        val accountId: String?,
        val note: String?,
        val occurredAt: Instant,
        val clientUpdatedAt: Instant,
        val createdAt: Instant,
    )

    private class StoredBudget(
        val id: String,
        val categoryId: String,
        var limitIrr: Double,
        var rollover: Boolean,
        val createdAt: Instant,
        var updatedAt: Instant,
    )

    private class StoredGoal(
        val id: String,
        var name: String,
        var targetIrr: Double,
        var iconKey: String,
        var targetDate: Instant?,
        var archived: Boolean = false,
        var updatedAt: Instant,
    ) {
        val contributions = mutableListOf<Contribution>()
    }

    private data class Contribution(val occurredAt: Instant, val amountIrr: Double)

    private class StoredNotification(
        val id: String,
        val type: String,
        val title: String,
        val body: String,
        val iconTone: String,
        var isRead: Boolean,
        val occurredAt: Instant,
    )

    private data class ValidatedTransaction(
        val kind: String,
        val categorySlug: String,
        val accountId: String?,
        val amountIrr: Double,
        val enteredCurrency: String,
        val enteredAmount: Double,
        val fxRate: Double?,
    )

    private data class CategorySeed(val slug: String, val name: String, val icon: String, val tone: String, val kind: String)

    private data class CurrencyInfo(
        val code: String,
        val symbol: String,
        val decimals: Int,
        val rate: Double,
        val englishName: String,
    )

    private data class SeedRow(val day: Int, val kind: String, val slug: String, val note: String, val amount: Double)

    private companion object {
        private const val TOKEN_PREFIX = "mock."
        const val RESET_TOKEN = "mock-reset-token"
        const val BAD_PASSWORD = "Use at least 8 characters with a lowercase letter and a digit."
        val NOT_FOUND_TX = MockOutcome.Error(404, "Transaction not found.")
        val NOT_FOUND_BUDGET = MockOutcome.Error(404, "Budget not found.")
        val NOT_FOUND_GOAL = MockOutcome.Error(404, "Goal not found.")

        val UNAUTHORIZED = MockOutcome.Error(401, "Unauthorized.")
        val BAD_MONTH = MockOutcome.Error(400, "Validation failed.", mapOf("month" to listOf("Month must be 1-12.")))

        val DAY_LABEL: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

        val CATEGORY_SEEDS = listOf(
            CategorySeed("groceries", "Groceries", "cart", "ok", "expense"),
            CategorySeed("dining", "Dining", "wallet", "warn", "expense"),
            CategorySeed("transport", "Transport", "car", "info", "expense"),
            CategorySeed("subscriptions", "Subscriptions", "music", "info", "expense"),
            CategorySeed("bills", "Bills", "receipt", "danger", "expense"),
            CategorySeed("shopping", "Shopping", "bag", "warn", "expense"),
            CategorySeed("fun", "Fun", "smile", "ok", "expense"),
            CategorySeed("housing", "Housing", "home", "info", "expense"),
            CategorySeed("other", "Other", "wallet", "info", "expense"),
            CategorySeed("salary", "Salary", "in", "ok", "income"),
        )

        val CURRENCIES = listOf(
            CurrencyInfo("IRR", "\uFDFC", 0, 1.0, "Iranian rial"),
            CurrencyInfo("USD", "$", 2, 1_042_000.0, "US dollar"),
            CurrencyInfo("EUR", "\u20AC", 2, 1_128_000.0, "Euro"),
            CurrencyInfo("AED", "AED", 2, 283_700.0, "UAE dirham"),
            CurrencyInfo("TRY", "\u20BA", 2, 30_200.0, "Turkish lira"),
        )

        val LOCALES = listOf(
            LocaleDto("en", "English", "English", "ltr"),
            LocaleDto("fa", "Persian", "فارسی", "rtl"),
            LocaleDto("fr", "French", "Français", "ltr"),
        )

        val SEED_ROWS = listOf(
            SeedRow(1, "income", "salary", "Monthly salary", 45_000_000.0),
            SeedRow(2, "expense", "housing", "Rent", 12_000_000.0),
            SeedRow(3, "expense", "groceries", "Weekly shop", 1_450_000.0),
            SeedRow(5, "expense", "transport", "Metro card", 320_000.0),
            SeedRow(6, "expense", "dining", "Lunch with team", 640_000.0),
            SeedRow(8, "expense", "bills", "Internet", 850_000.0),
            SeedRow(9, "expense", "subscriptions", "Music", 199_000.0),
            SeedRow(11, "expense", "fun", "Cinema", 480_000.0),
            SeedRow(13, "expense", "shopping", "Winter jacket", 2_150_000.0),
            SeedRow(16, "expense", "groceries", "Groceries", 980_000.0),
            SeedRow(18, "expense", "dining", "Dinner", 720_000.0),
            SeedRow(21, "expense", "transport", "Fuel", 1_100_000.0),
        )
    }
}
