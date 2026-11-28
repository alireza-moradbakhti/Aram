package com.aram.feature.dashboard.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DonutMathTest {

    @Test
    fun `empty and zero totals produce no slices`() {
        assertTrue(buildDonutSlices(emptyList()).isEmpty())
        assertTrue(
            buildDonutSlices(
                listOf(
                    spend("a", "Food", 0),
                    spend("b", "Rent", 0),
                ),
            ).isEmpty(),
        )
    }

    @Test
    fun `non-positive amounts are ignored`() {
        val slices = buildDonutSlices(
            listOf(
                spend("a", "Food", 1_000),
                spend("b", "Refund", -500),
            ),
        )

        assertEquals(1, slices.size)
        assertEquals("Food", slices.single().label)
    }

    @Test
    fun `single category owns the full ring`() {
        val slice = buildDonutSlices(listOf(spend("a", "Groceries", 4_200))).single()

        assertEquals(100, slice.percent)
        assertEquals(0f, slice.startAngle)
        assertEquals(360f, slice.sweepAngle, 0.001f)
    }

    @Test
    fun `sweeps sum to 360 and percents to 100 with rounding`() {
        val slices = buildDonutSlices(
            listOf(
                spend("a", "Groceries", 4_120),
                spend("b", "Dining", 1_760),
                spend("c", "Transport", 1_000),
                spend("d", "Fun", 333),
                spend("e", "Other", 201),
            ),
        )

        assertEquals(360.0, slices.sumOf { it.sweepAngle.toDouble() }, 0.01)
        assertEquals(100, slices.sumOf { it.percent })
        var expectedStart = 0f
        slices.forEach { slice ->
            assertEquals(expectedStart, slice.startAngle, 0.01f)
            expectedStart += slice.sweepAngle
        }
    }

    @Test
    fun `percent remainders go to the largest fractions`() {
        // 1/3 each is 33.33%; the missing point lands on the first slice so the
        // displayed legend never loses a percentage.
        val slices = buildDonutSlices(
            listOf(
                spend("a", "Food", 1),
                spend("b", "Rent", 1),
                spend("c", "Fun", 1),
            ),
        )

        assertEquals(listOf(34, 33, 33), slices.map { it.percent })
        assertEquals(100, slices.sumOf { it.percent })
    }

    @Test
    fun `slices are ordered by amount descending`() {
        val slices = buildDonutSlices(
            listOf(
                spend("a", "Fun", 100),
                spend("b", "Groceries", 900),
                spend("c", "Rent", 500),
            ),
        )

        assertEquals(listOf("Groceries", "Rent", "Fun"), slices.map { it.label })
    }

    private fun spend(id: String, label: String, amount: Long) = CategorySpend(
        categoryId = id,
        label = label,
        colorArgb = 0xFF2FD39A.toInt(),
        amountMinor = amount,
    )
}