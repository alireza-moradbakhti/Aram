package com.aram.core.domain.transaction

import com.aram.core.domain.repository.LedgerLookup
import com.aram.core.domain.repository.TransactionRepository
import com.aram.core.model.Account
import com.aram.core.model.AccountType
import com.aram.core.model.Category
import com.aram.core.model.CategoryKind
import com.aram.core.model.Outcome
import com.aram.core.model.Transaction
import com.aram.core.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal fun testAccount(
    id: String = "acc",
    currencyCode: String = "IRR",
    archived: Boolean = false,
) = Account(
    id = id,
    name = "Cash",
    type = AccountType.CASH,
    currencyCode = currencyCode,
    openingBalanceMinor = 0L,
    colorArgb = 0,
    iconKey = "wallet",
    archived = archived,
)

internal fun testCategory(
    id: String = "cat",
    kind: CategoryKind = CategoryKind.EXPENSE,
    archived: Boolean = false,
) = Category(
    id = id,
    name = "Food",
    kind = kind,
    parentId = null,
    colorArgb = 0,
    iconKey = "food",
    archived = archived,
)

internal fun testDraft(
    accountId: String = "acc",
    categoryId: String? = "cat",
    type: TransactionType = TransactionType.EXPENSE,
    amountMinor: Long = 1_000L,
    currencyCode: String = "IRR",
    occurredAt: Long = 1_700_000_000_000L,
) = TransactionDraft(
    accountId = accountId,
    categoryId = categoryId,
    type = type,
    amountMinor = amountMinor,
    currencyCode = currencyCode,
    occurredAt = occurredAt,
)

internal fun testTransaction(id: String = "tx-1", draft: TransactionDraft = testDraft()) = Transaction(
    id = id,
    accountId = draft.accountId,
    categoryId = draft.categoryId,
    type = draft.type,
    amountMinor = draft.amountMinor,
    currencyCode = draft.currencyCode,
    occurredAt = draft.occurredAt,
    note = draft.note,
    transferGroupId = null,
    recurringRuleId = null,
)

internal class FakeTransactionRepository : TransactionRepository {

    val created = mutableListOf<TransactionDraft>()
    val updated = mutableListOf<Pair<String, TransactionDraft>>()
    val deleted = mutableListOf<String>()

    var createResult: (TransactionDraft) -> Outcome<Transaction> = { Outcome.Success(testTransaction(draft = it)) }
    var updateResult: (String, TransactionDraft) -> Outcome<Transaction> =
        { id, draft -> Outcome.Success(testTransaction(id = id, draft = draft)) }
    var deleteResult: Outcome<Unit> = Outcome.Success(Unit)

    override fun observeTransactions(
        fromInclusive: Long,
        toExclusive: Long,
    ): Flow<List<Transaction>> = flowOf(emptyList())

    override fun observeRecent(limit: Int): Flow<List<Transaction>> = flowOf(emptyList())

    override fun observeTransaction(id: String): Flow<Transaction?> = flowOf(null)

    override suspend fun create(draft: TransactionDraft): Outcome<Transaction> {
        created += draft
        return createResult(draft)
    }

    override suspend fun update(id: String, draft: TransactionDraft): Outcome<Transaction> {
        updated += id to draft
        return updateResult(id, draft)
    }

    override suspend fun softDelete(id: String): Outcome<Unit> {
        deleted += id
        return deleteResult
    }
}

internal class FakeLedgerLookup : LedgerLookup {

    val accounts = mutableMapOf<String, Account>()
    val categories = mutableMapOf<String, Category>()

    var accountResult: Outcome<Account?>? = null
    var categoryResult: Outcome<Category?>? = null

    override suspend fun account(id: String): Outcome<Account?> =
        accountResult ?: Outcome.Success(accounts[id])

    override suspend fun category(id: String): Outcome<Category?> =
        categoryResult ?: Outcome.Success(categories[id])
}
