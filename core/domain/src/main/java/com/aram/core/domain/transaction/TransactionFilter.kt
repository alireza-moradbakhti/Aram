package com.aram.core.domain.transaction

import com.aram.core.model.TransactionType

/**
 * Ledger search criteria; every populated field narrows the result with AND.
 *
 * [excludeType] exists because "Expenses" means everything except income —
 * transfers render as outflows and must stay visible — which a single equality
 * [type] cannot express.
 */
data class TransactionFilter(
    val query: String = "",
    val type: TransactionType? = null,
    val excludeType: TransactionType? = null,
    val categoryId: String? = null,
    val fromInclusive: Long? = null,
    val toExclusive: Long? = null,
) {
    val isActive: Boolean
        get() = query.isNotBlank() || type != null || excludeType != null ||
            categoryId != null || fromInclusive != null || toExclusive != null
}