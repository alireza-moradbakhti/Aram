package com.aram.feature.onboarding.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aram.core.designsystem.AramTextStyles
import com.aram.core.designsystem.AramTheme
import com.aram.core.designsystem.aramColors
import com.aram.core.designsystem.component.AramBackground
import com.aram.core.designsystem.component.AramGhostButton
import com.aram.core.designsystem.component.AramPrimaryButton
import com.aram.core.designsystem.icon.AramIcons
import com.aram.feature.onboarding.R

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.aramColors

    AramBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMark(size = 46, corner = 15, glyphSize = 22)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.splash_tag),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 18.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp,
                        ),
                    )
                    Text(
                        text = stringResource(R.string.welcome_brand_sub).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        letterSpacing = 1.4.sp,
                        color = colors.textFaint,
                    )
                }
                Spacer(Modifier.weight(1f))
                LanguagePill()
            }

            Spacer(Modifier.height(26.dp))

            val headlinePrefix = stringResource(R.string.welcome_headline_prefix)
            val headlineAccent = stringResource(R.string.welcome_headline_accent)
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(headlinePrefix)
                    }
                    withStyle(SpanStyle(color = colors.primaryHi)) {
                        append(headlineAccent)
                    }
                },
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 30.sp,
                    lineHeight = 36.sp,
                    letterSpacing = (-0.9).sp,
                ),
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.welcome_lead),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = colors.textDim,
                modifier = Modifier.widthIn(max = 252.dp),
            )

            Spacer(Modifier.height(24.dp))

            SafeToSpendPreview()

            Spacer(Modifier.height(20.dp))

            WelcomePoint(
                icon = AramIcons.cloudOff,
                title = stringResource(R.string.welcome_point_offline_title),
                body = stringResource(R.string.welcome_point_offline_body),
            )
            WelcomePoint(
                icon = AramIcons.shield,
                title = stringResource(R.string.welcome_point_private_title),
                body = stringResource(R.string.welcome_point_private_body),
            )
            WelcomePoint(
                icon = AramIcons.payments,
                title = stringResource(R.string.welcome_point_currency_title),
                body = stringResource(R.string.welcome_point_currency_body),
            )

            Spacer(Modifier.height(24.dp))

            Column(Modifier.fillMaxWidth()) {
                AramPrimaryButton(
                    text = stringResource(R.string.welcome_get_started),
                    onClick = onGetStarted,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                AramGhostButton(
                    text = stringResource(R.string.welcome_sign_in),
                    onClick = onSignIn,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.welcome_terms),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.sp,
                ),
                color = colors.textFaint,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SafeToSpendPreview() {
    val colors = MaterialTheme.aramColors
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(colors.heroStart, colors.heroMid, colors.heroEnd, colors.heroDeep),
                ),
            )
            .border(1.dp, Color(0x40FFFFFF), shape)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colorStops = arrayOf(0f to Color(0x61FFFFFF), 0.48f to Color.Transparent),
                        center = Offset(size.width * 0.08f, 0f),
                        radius = size.width * 1.2f,
                    ),
                )
                drawCircle(
                    color = Color(0x1AFFFFFF),
                    radius = 95.dp.toPx(),
                    center = Offset(size.width - 55.dp.toPx(), -56.dp.toPx()),
                    style = Stroke(width = 24.dp.toPx()),
                )
            }
            .padding(20.dp),
    ) {
        Column {
            Text(
                text = stringResource(R.string.welcome_preview_label),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                letterSpacing = 0.4.sp,
                color = Color(0xD9FFFFFF),
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = stringResource(R.string.welcome_preview_amount),
                    style = AramTextStyles.heroAmountCompact.copy(fontSize = 30.sp),
                    color = Color.White,
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "IRR",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.sp,
                    color = Color(0xCCFFFFFF),
                    modifier = Modifier.padding(bottom = 5.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroPill(
                    text = stringResource(R.string.welcome_preview_days),
                    icon = AramIcons.check,
                )
                HeroPill(text = stringResource(R.string.welcome_preview_budget))
            }
        }
    }
}

@Composable
private fun HeroPill(
    text: String,
    icon: ImageVector? = null,
) {
    Row(
        modifier = Modifier
            .height(26.dp)
            .clip(CircleShape)
            .background(Color(0x29FFFFFF))
            .border(1.dp, Color(0x3DFFFFFF), CircleShape)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
            ),
            color = Color.White,
        )
    }
}

@Composable
private fun WelcomePoint(
    icon: ImageVector,
    title: String,
    body: String,
) {
    val colors = MaterialTheme.aramColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface1)
                .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.primaryHi,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    letterSpacing = 0.sp,
                ),
                color = colors.textFaint,
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun WelcomeScreenPreview() {
    AramTheme {
        WelcomeScreen(onGetStarted = {}, onSignIn = {})
    }
}
