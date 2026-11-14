package com.aram.core.model

data class Category(
    val id: String,
    val name: String,
    val kind: CategoryKind,
    val parentId: String?,
    val colorArgb: Int,
    val iconKey: String,
    val archived: Boolean,
)

enum class CategoryKind {
    INCOME,
    EXPENSE,
}
