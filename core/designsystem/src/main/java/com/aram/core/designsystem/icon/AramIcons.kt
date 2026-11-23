package com.aram.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * The prototype's icon set: 24-unit viewBox, round caps, 1.75 stroke. Drawn as
 * stroked vectors so they keep the line weight of the design at any icon size.
 */
object AramIcons {

    val home: ImageVector = lineIcon(
        "Home",
        "m3 10 9-7 9 7v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2Z",
        "M9 21v-7h6v7",
    )

    val activity: ImageVector = lineIcon(
        "Activity",
        "M4 19V9M10 19V5M16 19v-7M22 19H2",
    )

    val budgets: ImageVector = lineIcon(
        "Budgets",
        "M3,12a9,9 0 1,0 18,0a9,9 0 1,0 -18,0",
        "M12 7v10M9.5 9.5h3.75a2 2 0 0 1 0 4H9.5",
    )

    val profile: ImageVector = lineIcon(
        "Profile",
        "M8,8a4,4 0 1,0 8,0a4,4 0 1,0 -8,0",
        "M4 21c0-4 3.6-7 8-7s8 3 8 7",
    )

    val add: ImageVector = lineIcon("Add", "M12 5v14M5 12h14")

    val close: ImageVector = lineIcon("Close", "M6 6l12 12M18 6 6 18")

    val check: ImageVector = lineIcon("Check", "M20 6 9 17l-5-5")

    val chevronLeft: ImageVector = lineIcon("ChevronLeft", "m15 6-6 6 6 6")

    val chevronRight: ImageVector = lineIcon("ChevronRight", "m9 6 6 6-6 6")

    val chevronDown: ImageVector = lineIcon("ChevronDown", "m6 9 6 6 6-6")

    val chevronUp: ImageVector = lineIcon("ChevronUp", "m18 15-6-6-6 6")

    val notifications: ImageVector = lineIcon(
        "Notifications",
        "M18 8a6 6 0 1 0-12 0c0 7-3 8-3 8h18s-3-1-3-8",
        "M10.3 21a2 2 0 0 0 3.4 0",
    )

    val search: ImageVector = lineIcon(
        "Search",
        "M4,11a7,7 0 1,0 14,0a7,7 0 1,0 -14,0",
        "m20 20-3.5-3.5",
    )

    val filter: ImageVector = lineIcon(
        "Filter",
        "M4 7h10M18 7h2M4 17h4M12 17h8",
        "M14,7a2,2 0 1,0 4,0a2,2 0 1,0 -4,0",
        "M8,17a2,2 0 1,0 4,0a2,2 0 1,0 -4,0",
    )

    val cart: ImageVector = lineIcon(
        "Cart",
        "M7.6,20a1.4,1.4 0 1,0 2.8,0a1.4,1.4 0 1,0 -2.8,0",
        "M16.6,20a1.4,1.4 0 1,0 2.8,0a1.4,1.4 0 1,0 -2.8,0",
        "M3 4h2l2.2 11h11L21 8H6.2",
    )

    val restaurant: ImageVector = lineIcon(
        "Restaurant",
        "M4 8h13a3 3 0 0 1 0 6H4z",
        "M4 8V6h11",
    )

    val cafe: ImageVector = lineIcon(
        "Cafe",
        "M5 8h11v7a4 4 0 0 1-4 4H9a4 4 0 0 1-4-4Z",
        "M16 9h2a2.5 2.5 0 0 1 0 5h-2",
        "M8 3v2M12 3v2",
    )

    val car: ImageVector = lineIcon(
        "Car",
        "M5 16h14M6.5 16l1.4-4.6A2 2 0 0 1 9.8 10h4.4a2 2 0 0 1 1.9 1.4L17.5 16",
        "M6.6,18a1.4,1.4 0 1,0 2.8,0a1.4,1.4 0 1,0 -2.8,0",
        "M14.6,18a1.4,1.4 0 1,0 2.8,0a1.4,1.4 0 1,0 -2.8,0",
    )

    val musicNote: ImageVector = lineIcon(
        "MusicNote",
        "M9 18V6l7-2v12",
        "M5,18a2,2 0 1,0 4,0a2,2 0 1,0 -4,0",
        "M14,16a2,2 0 1,0 4,0a2,2 0 1,0 -4,0",
    )

    val tune: ImageVector = lineIcon(
        "Tune",
        "M4 7h7M15 7h5M4 17h3M11 17h9",
        "M11,7a2,2 0 1,0 4,0a2,2 0 1,0 -4,0",
        "M7,17a2,2 0 1,0 4,0a2,2 0 1,0 -4,0",
    )

    val shield: ImageVector = lineIcon(
        "Shield",
        "M12 3l7 3v5c0 4.5-3 7.7-7 9-4-1.3-7-4.5-7-9V6Z",
        "m9 12 2 2 4-4",
    )

    val contrast: ImageVector = lineIcon(
        "Contrast",
        "M3,12a9,9 0 1,0 18,0a9,9 0 1,0 -18,0",
        "M12 3v18M12 3a9 9 0 0 1 0 18",
    )

    val payments: ImageVector = lineIcon(
        "Payments",
        "M3,12a9,9 0 1,0 18,0a9,9 0 1,0 -18,0",
        "M8 12h8M12 8v8",
    )

    val card: ImageVector = lineIcon(
        "Card",
        "M5,6h14a2,2 0 0 1 2,2v8a2,2 0 0 1 -2,2H5a2,2 0 0 1 -2,-2V8a2,2 0 0 1 2,-2Z",
        "M16 12h3",
    )

    val calendar: ImageVector = lineIcon(
        "Calendar",
        "M5,5h14a2,2 0 0 1 2,2v12a2,2 0 0 1 -2,2H5a2,2 0 0 1 -2,-2V7a2,2 0 0 1 2,-2Z",
        "M8 3v4M16 3v4M3 10h18",
    )

    val download: ImageVector = lineIcon(
        "Download",
        "M12 3v12M7.5 10.5 12 15l4.5-4.5",
        "M4 19h16",
    )

    val key: ImageVector = lineIcon(
        "Key",
        "M4,15a4,4 0 1,0 8,0a4,4 0 1,0 -8,0",
        "m11 12 9-9M17 3l4 4M14 6l4 4",
    )

    val edit: ImageVector = lineIcon(
        "Edit",
        "M4 20h4L20 8l-4-4L4 16Z",
        "m14 6 4 4",
    )

    val trash: ImageVector = lineIcon(
        "Trash",
        "M4 7h16M9 7V5h6v2M6 7l1 13h10l1-13M10 11v5M14 11v5",
    )

    val logout: ImageVector = lineIcon(
        "Logout",
        "M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3",
        "M10 8l-4 4 4 4M6 12h9",
    )

    val settings: ImageVector = lineIcon(
        "Settings",
        "M9,12a3,3 0 1,0 6,0a3,3 0 1,0 -6,0",
        "M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.9-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1-1.6 1.7 1.7 0 0 0-1.9.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.9 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.6-1 1.7 1.7 0 0 0-.3-1.9l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.9.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.9-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.9V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1Z",
    )

    val bug: ImageVector = lineIcon(
        "Bug",
        "M8 8h8a4 4 0 0 1 4 4v3a4 4 0 0 1-4 4H8a4 4 0 0 1-4-4v-3a4 4 0 0 1 4-4Z",
        "M12 8V5M9 5h6",
        "M4 11h4M16 11h4M4 17h4M16 17h4",
    )

    val cloudOff: ImageVector = lineIcon(
        "CloudOff",
        "M2 8.8a16 16 0 0 1 6.2-2.9M13 6a16 16 0 0 1 9 2.8",
        "M5 12.3a11 11 0 0 1 4.8-1.9M15.5 10.7A11 11 0 0 1 19 12.3",
        "M8.5 15.7a6 6 0 0 1 3.5-1.2M14.5 15.2a6 6 0 0 1 1 .5",
        "M11,19a1,1 0 1,0 2,0a1,1 0 1,0 -2,0",
        "M3 3l18 18",
    )

    val lock: ImageVector = lineIcon(
        "Lock",
        "M7 10h10a3 3 0 0 1 3 3v4a3 3 0 0 1-3 3H7a3 3 0 0 1-3-3v-4a3 3 0 0 1 3-3Z",
        "M8 10V7a4 4 0 0 1 8 0v3",
    )

    val mail: ImageVector = lineIcon(
        "Mail",
        "M5 5h14a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2Z",
        "m4 7 8 6 8-6",
    )

    val person: ImageVector = lineIcon(
        "Person",
        "M12,8a4,4 0 1,0 0,0.01",
        "M4 21c0-4 3.6-7 8-7s8 3 8 7",
    )

    val eye: ImageVector = lineIcon(
        "Eye",
        "M2 12s3.6-6 10-6 10 6 10 6-3.6 6-10 6-10-6-10-6Z",
        "M12,12a2.6,2.6 0 1,0 0,0.01",
    )

    val eyeOff: ImageVector = lineIcon(
        "EyeOff",
        "M3 3l18 18",
        "M10.6 6.2A9.6 9.6 0 0 1 12 6c6.4 0 10 6 10 6a17 17 0 0 1-3.2 3.9",
        "M6.1 6.6A17.5 17.5 0 0 0 2 12s3.6 6 10 6a9.4 9.4 0 0 0 4.4-1.1",
        "M9.9 9.9a3 3 0 0 0 4.2 4.2",
    )

    val alert: ImageVector = lineIcon(
        "Alert",
        "M12,12a9,9 0 1,0 0,0.01",
        "M12 7v6M12 16.5v.1",
    )

    val receipt: ImageVector = lineIcon(
        "Receipt",
        "M6 3h12v18l-3-2-3 2-3-2-3 2Z",
        "M9 8h6M9 12h6",
    )

    val shoppingBag: ImageVector = lineIcon(
        "ShoppingBag",
        "M6 8h12l-1 13H7Z",
        "M9 8V6a3 3 0 0 1 6 0v2",
    )

    val smile: ImageVector = lineIcon(
        "Smile",
        "M3,12a9,9 0 1,0 18,0a9,9 0 1,0 -18,0",
        "M8 14s1.5 2 4 2 4-2 4-2M9 9.5v.1M15 9.5v.1",
    )

    val star: ImageVector = lineIcon(
        "Star",
        "m12 3 2.7 5.5 6.1.9-4.4 4.3 1 6-5.4-2.9-5.4 2.9 1-6L3.2 9.4l6.1-.9Z",
    )

    val target: ImageVector = lineIcon(
        "Target",
        "M3,12a9,9 0 1,0 18,0a9,9 0 1,0 -18,0",
        "M8,12a4,4 0 1,0 8,0a4,4 0 1,0 -8,0",
    )

    val arrowIn: ImageVector = lineIcon("ArrowIn", "M17 7 7 17M15 17H7V9")

    val trending: ImageVector = lineIcon(
        "Trending",
        "M3 17l6-6 4 4 7-7",
        "M14 8h6v6",
    )

    val qr: ImageVector = lineIcon(
        "Qr",
        "M4 8V5a1 1 0 0 1 1-1h3M16 4h3a1 1 0 0 1 1 1v3M20 16v3a1 1 0 0 1-1 1h-3M8 20H5a1 1 0 0 1-1-1v-3",
        "M4 12h16",
    )

    val wallet: ImageVector = lineIcon(
        "Wallet",
        "M3 7h18v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2Z",
        "M3 7l2-4h14l2 4",
        "M9 12h6",
    )
}

private const val VIEWPORT = 24f
private const val STROKE_WIDTH = 1.75f

private fun lineIcon(name: String, vararg paths: String): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = VIEWPORT,
        viewportHeight = VIEWPORT,
    )
    paths.forEach { path ->
        builder.addPath(
            pathData = PathParser().parsePathString(path).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    return builder.build()
}
