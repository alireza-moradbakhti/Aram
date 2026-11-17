package com.aram.core.data.mapper

import com.aram.core.database.entity.AccountEntity
import com.aram.core.database.entity.CategoryEntity
import com.aram.core.database.entity.TransactionEntity
import com.aram.core.model.Account
import com.aram.core.model.AccountType
import com.aram.core.model.Category
import com.aram.core.model.CategoryKind
import com.aram.core.model.Transaction
import com.aram.core.model.TransactionType

internal fun TransactionEntity.toModel(): Transaction = Transaction(
    id = id,
    accountId = accountId,
    categoryId = categoryId,
    type = runCatching { TransactionType.valueOf(type) }
        .getOrDefault(TransactionType.EXPENSE),
    amountMinor = amountMinor,
    currencyCode = currencyCode,
    occurredAt = occurredAt,
    note = note,
    transferGroupId = transferGroupId,
    recurringRuleId = recurringRuleId,
)

internal fun AccountEntity.toModel(): Account = Account(
    id = id,
    name = name,
    type = runCatching { AccountType.valueOf(type) }
        .getOrDefault(AccountType.CASH),
    currencyCode = currencyCode,
    openingBalanceMinor = openingBalanceMinor,
    colorArgb = colorArgb,
    iconKey = iconKey,
    archived = archived,
)

internal fun CategoryEntity.toModel(): Category = Category(
    id = id,
    name = name,
    kind = runCatching { CategoryKind.valueOf(kind) }
        .getOrDefault(CategoryKind.EXPENSE),
    parentId = parentId,
    colorArgb = colorArgb,
    iconKey = iconKey,
    archived = archived,
)
