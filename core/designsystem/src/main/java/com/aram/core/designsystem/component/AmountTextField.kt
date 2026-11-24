package com.aram.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aram.core.designsystem.AramTheme
import com.aram.core.designsystem.aramColors
import com.aram.core.money.CurrencyCode

/**
 * Stateless amount entry. The caller owns the raw text and parses it with
 * `AmountParser`; the symbol and allowed fraction digits come from [currency].
 */
@Composable
fun AmountTextField(
    value: String,
    onValueChange: (String) -> Unit,
    currency: CurrencyCode,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "0",
    error: String? = null,
    hint: String? = null,
    enabled: Boolean = true,
    large: Boolean = false,
) {
    val colors = MaterialTheme.aramColors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val borderColor = when {
        error != null -> colors.danger
        focused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        else -> colors.border
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) {
            Text(
                text = if (large) label.uppercase() else label,
                style = if (large) {
                    MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.88.sp,
                    )
                } else {
                    MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp)
                },
                color = if (large) colors.textFaint else colors.textDim,
            )
        }

        if (large) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                    textStyle = TextStyle(
                        fontSize = 34.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.68).sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Start,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    interactionSource = interaction,
                    decorationBox = { inner ->
                        Box {
                            if (value.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    fontSize = 34.sp,
                                    lineHeight = 40.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textFaint,
                                )
                            }
                            inner()
                        }
                    },
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = currency.isoCode,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = colors.textFaint,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(borderColor),
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                    .background(colors.surface1, RoundedCornerShape(12.dp))
                    .padding(start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    interactionSource = interaction,
                    decorationBox = { inner ->
                        Box {
                            if (value.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    fontSize = 16.sp,
                                    color = colors.textFaint,
                                )
                            }
                            inner()
                        }
                    },
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = currency.isoCode,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.72.sp,
                    ),
                    color = colors.textFaint,
                )
            }
        }

        val supporting = error ?: hint
        if (supporting != null) {
            Text(
                text = supporting,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = if (error != null) colors.danger else colors.textFaint,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun AmountTextFieldDarkPreview() {
    AramTheme(darkTheme = true) {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            AmountTextField(
                value = "180000",
                onValueChange = {},
                currency = CurrencyCode.IRR,
                label = "Amount",
                hint = "Fraction digits are 0 for IRR.",
            )
            AmountTextField(
                value = "12,50",
                onValueChange = {},
                currency = CurrencyCode.USD,
                label = "Amount",
                error = "Too many fraction digits",
                large = true,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun AmountTextFieldLightPreview() {
    AramTheme(darkTheme = false) {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            AmountTextField(
                value = "180000",
                onValueChange = {},
                currency = CurrencyCode.IRR,
                label = "Amount",
                hint = "Fraction digits are 0 for IRR.",
            )
            AmountTextField(
                value = "",
                onValueChange = {},
                currency = CurrencyCode.USD,
                label = "Amount",
                large = true,
            )
        }
    }
}
