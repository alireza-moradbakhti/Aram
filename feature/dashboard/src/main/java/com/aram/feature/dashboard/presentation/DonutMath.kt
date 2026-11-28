package com.aram.feature.dashboard.presentation

import kotlin.math.floor
import kotlin.math.round

internal data class CategorySpend(
    val categoryId: String,
    val label: String,
    val colorArgb: Int,
    val amountMinor: Long,
)

data class DonutSlice(
    val categoryId: String,
    val label: String,
    val colorArgb: Int,
    val amountMinor: Long,
    val percent: Int,
    val startAngle: Float,
    val sweepAngle: Float,
)

/**
 * Angles are degrees starting at 0 (12 o'clock once the canvas rotates by
 * -90); the last slice ends exactly at 360.
 */
internal fun buildDonutSlices(spending: List<CategorySpend>): List<DonutSlice> {
    val slices = spending
        .filter { it.amountMinor > 0L }
        .sortedWith(compareByDescending<CategorySpend> { it.amountMinor }.thenBy { it.label })
    val total = slices.sumOf { it.amountMinor }
    if (total <= 0L) return emptyList()

    val percents = allocatePercents(slices.map { it.amountMinor }, total)
    var cumulative = 0L
    var start = 0.0
    return slices.mapIndexed { index, slice ->
        cumulative += slice.amountMinor
        val boundary = if (index == slices.lastIndex) {
            360.0
        } else {
            val raw = cumulative.toDouble() * 360.0 / total
            (raw * 100.0).roundToInt() / 100.0
        }
        DonutSlice(
            categoryId = slice.categoryId,
            label = slice.label,
            colorArgb = slice.colorArgb,
            amountMinor = slice.amountMinor,
            percent = percents[index],
            startAngle = start.toFloat(),
            sweepAngle = (boundary - start).toFloat(),
        ).also { start = boundary }
    }
}

private fun allocatePercents(amounts: List<Long>, total: Long): List<Int> {
    val raw = amounts.map { it.toDouble() * 100.0 / total }
    val percents = raw.map { floor(it).toInt() }.toMutableList()
    var remaining = 100 - percents.sum()
    val byRemainder = raw.indices.sortedByDescending { raw[it] - floor(raw[it]) }
    for (index in byRemainder) {
        if (remaining <= 0) break
        percents[index]++
        remaining--
    }
    return percents
}

private fun Double.roundToInt(): Int = round(this).toInt()