package com.aram.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aram.core.designsystem.aramColors

private val ToastShape = RoundedCornerShape(percent = 50)
private val ToastBackground = Color(0xEB14221D)
private val ToastText = Color(0xFFE4EDE9)

@Composable
fun AramToast(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        modifier = modifier
            .background(ToastBackground, ToastShape)
            .border(1.dp, MaterialTheme.aramColors.borderStrong, ToastShape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        color = ToastText,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
    )
}
