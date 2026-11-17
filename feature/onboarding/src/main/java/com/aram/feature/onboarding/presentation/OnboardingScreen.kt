package com.aram.feature.onboarding.presentation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aram.core.designsystem.AramTheme
import com.aram.core.designsystem.aramColors
import com.aram.core.designsystem.component.AramBackground
import com.aram.core.designsystem.component.AramGhostButton
import com.aram.core.designsystem.component.AramPrimaryButton
import com.aram.core.designsystem.icon.AramIcons
import com.aram.feature.onboarding.R

private data class OnboardingStep(
    val titleRes: Int,
    val bodyRes: Int,
    val glyph: ImageVector,
)

private val steps = listOf(
    OnboardingStep(
        titleRes = R.string.onboarding_title_1,
        bodyRes = R.string.onboarding_body_1,
        glyph = AramIcons.activity,
    ),
    OnboardingStep(
        titleRes = R.string.onboarding_title_2,
        bodyRes = R.string.onboarding_body_2,
        glyph = AramIcons.clock,
    ),
    OnboardingStep(
        titleRes = R.string.onboarding_title_3,
        bodyRes = R.string.onboarding_body_3,
        glyph = AramIcons.lock,
    ),
)

@Composable
fun OnboardingScreen(
    step: Int,
    onStepChange: (Int) -> Unit,
    onSkip: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.aramColors
    val index = step.coerceIn(steps.indices)
    val current = steps[index]
    val isLast = index == steps.lastIndex

    AramBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandMark(size = 38, corner = 12, glyphSize = 18)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.splash_tag),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 15.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.15).sp,
                    ),
                )
                Spacer(Modifier.weight(1f))
                LanguagePill()
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.onboarding_skip),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = colors.primaryHi,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onSkip)
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                steps.forEachIndexed { i, _ ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (i <= index) colors.primaryHi else colors.borderStrong),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            OnboardingArt(
                glyph = current.glyph,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(306.dp),
            )

            Crossfade(targetState = index, label = "onboarding-copy") { visibleStep ->
                val page = steps[visibleStep]
                Column {
                    Text(
                        text = stringResource(page.titleRes),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 26.sp,
                            lineHeight = 32.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                        ),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(page.bodyRes),
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = colors.textDim,
                        modifier = Modifier.widthIn(max = 252.dp),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                steps.indices.forEach { i ->
                    val active = i == index
                    val width by animateDpAsState(
                        targetValue = if (active) 20.dp else 6.dp,
                        label = "dot-width",
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .width(width)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (active) colors.primaryHi else colors.borderStrong),
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AramGhostButton(
                    text = stringResource(R.string.onboarding_back),
                    onClick = { if (index > 0) onStepChange(index - 1) },
                    modifier = Modifier.alpha(if (index == 0) 0.42f else 1f),
                )
                Spacer(Modifier.width(12.dp))
                AramPrimaryButton(
                    text = if (isLast) {
                        stringResource(R.string.onboarding_start)
                    } else {
                        stringResource(R.string.onboarding_next)
                    },
                    onClick = { if (isLast) onDone() else onStepChange(index + 1) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun OnboardingArt(
    glyph: ImageVector,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.aramColors
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(258.dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to Color(0x802FD39A),
                                0.58f to Color(0x1F0E9F6E),
                                0.72f to Color.Transparent,
                            ),
                            center = Offset(size.width * 0.34f, size.height * 0.30f),
                            radius = size.width * 0.96f,
                        ),
                        radius = size.width / 2f,
                    )
                },
        )
        Crossfade(targetState = glyph, label = "onboarding-glyph") { currentGlyph ->
            Box {
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0x990E9F6E), Color(0x000E9F6E)),
                                    center = Offset(size.width / 2f, size.height * 0.66f),
                                    radius = size.width * 0.85f,
                                ),
                            )
                        },
                )
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .clip(RoundedCornerShape(38.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0x29FFFFFF), Color(0x08FFFFFF)),
                            ),
                        )
                        .background(colors.glassTint)
                        .border(1.dp, colors.borderStrong, RoundedCornerShape(38.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = currentGlyph,
                        contentDescription = null,
                        tint = colors.primaryHi,
                        modifier = Modifier.size(46.dp),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 34.dp, end = 12.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(colors.surface2)
                .border(1.dp, colors.border, RoundedCornerShape(13.dp))
                .padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = AramIcons.arrowIn,
                contentDescription = null,
                tint = colors.primaryHi,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = stringResource(R.string.onboarding_chip_expense),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = colors.textDim,
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 38.dp, start = 6.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(colors.surface2)
                .border(1.dp, colors.border, RoundedCornerShape(13.dp))
                .padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = AramIcons.clock,
                contentDescription = null,
                tint = colors.secondaryHi,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = stringResource(R.string.onboarding_chip_month),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = colors.textDim,
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun OnboardingScreenPreview() {
    AramTheme {
        OnboardingScreen(step = 0, onStepChange = {}, onSkip = {}, onDone = {})
    }
}
