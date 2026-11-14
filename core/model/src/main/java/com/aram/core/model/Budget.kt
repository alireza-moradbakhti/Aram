package com.aram.core.model

data class Budget(
    val id: String,
    val categoryId: String,
    val periodStartEpochDay: Long,
    val limitMinor: Long,
    val currencyCode: String,
)
