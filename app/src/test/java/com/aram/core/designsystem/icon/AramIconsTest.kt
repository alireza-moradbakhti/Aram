package com.aram.core.designsystem.icon

import org.junit.Assert.assertEquals
import org.junit.Test

class AramIconsTest {

    @Test
    fun `every icon parses into a 24 unit vector`() {
        val icons = listOf(
            AramIcons.home,
            AramIcons.activity,
            AramIcons.budgets,
            AramIcons.profile,
            AramIcons.add,
            AramIcons.close,
            AramIcons.check,
            AramIcons.chevronLeft,
            AramIcons.chevronRight,
            AramIcons.chevronDown,
            AramIcons.chevronUp,
            AramIcons.notifications,
            AramIcons.search,
            AramIcons.filter,
            AramIcons.cart,
            AramIcons.restaurant,
            AramIcons.cafe,
            AramIcons.car,
            AramIcons.musicNote,
            AramIcons.tune,
            AramIcons.shield,
            AramIcons.contrast,
            AramIcons.payments,
            AramIcons.card,
            AramIcons.calendar,
            AramIcons.download,
            AramIcons.key,
            AramIcons.edit,
            AramIcons.trash,
            AramIcons.logout,
            AramIcons.settings,
            AramIcons.bug,
            AramIcons.cloudOff,
            AramIcons.lock,
            AramIcons.receipt,
            AramIcons.shoppingBag,
            AramIcons.smile,
            AramIcons.star,
            AramIcons.target,
            AramIcons.arrowIn,
            AramIcons.trending,
            AramIcons.qr,
            AramIcons.wallet,
        )

        icons.forEach { icon ->
            assertEquals(24f, icon.defaultWidth.value, 0f)
            assertEquals(24f, icon.defaultHeight.value, 0f)
            assertEquals(24f, icon.viewportWidth, 0f)
            assertEquals(24f, icon.viewportHeight, 0f)
        }
    }
}
