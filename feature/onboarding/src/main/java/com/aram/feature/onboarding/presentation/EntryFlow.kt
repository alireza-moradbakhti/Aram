package com.aram.feature.onboarding.presentation

import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aram.core.designsystem.aramColors
import com.aram.core.designsystem.component.AramBackground
import com.aram.core.designsystem.icon.AramIcons
import com.aram.feature.onboarding.R

private enum class EntryStage { Welcome, Onboarding, CreateAccount }

@Composable
fun EntryRoute(
    modifier: Modifier = Modifier,
    viewModel: WelcomeViewModel = hiltViewModel(),
) {
    var stage by rememberSaveable { mutableStateOf(EntryStage.Welcome) }
    var onboardingStep by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current
    val signInUnavailable = stringResource(R.string.welcome_sign_in_unavailable)

    when (stage) {
        EntryStage.Welcome -> WelcomeScreen(
            onGetStarted = {
                onboardingStep = 0
                stage = EntryStage.Onboarding
            },
            onSignIn = {
                Toast.makeText(context, signInUnavailable, Toast.LENGTH_SHORT).show()
            },
            modifier = modifier,
        )

        EntryStage.Onboarding -> OnboardingScreen(
            step = onboardingStep,
            onStepChange = { onboardingStep = it },
            onSkip = { stage = EntryStage.CreateAccount },
            onDone = { stage = EntryStage.CreateAccount },
            modifier = modifier,
        )

        EntryStage.CreateAccount -> CreateAccountRoute(
            viewModel = viewModel,
            onBack = { stage = EntryStage.Welcome },
            onLogIn = {
                Toast.makeText(context, signInUnavailable, Toast.LENGTH_SHORT).show()
            },
            modifier = modifier,
        )
    }
}

@Composable
fun AramSplashScreen(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.aramColors
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
                    .height(38.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = AramIcons.shield,
                        contentDescription = null,
                        tint = colors.primaryHi,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.splash_tag).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        letterSpacing = 1.05.sp,
                        color = colors.textFaint,
                    )
                }
                LanguagePill()
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 34.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(286.dp)
                        .background(
                            Brush.radialGradient(
                                colorStops = arrayOf(
                                    0f to Color(0x572FD39A),
                                    0.58f to Color(0x120E9F6E),
                                    0.72f to Color.Transparent,
                                ),
                            ),
                        ),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BrandMark(size = 92, corner = 30, glyphSize = 34)
                    Spacer(Modifier.height(22.dp))
                    Text(
                        text = stringResource(R.string.splash_tag),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 34.sp,
                            lineHeight = 39.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.9).sp,
                        ),
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = stringResource(R.string.splash_tagline).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        letterSpacing = 2.3.sp,
                        color = colors.textFaint,
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.splash_copy),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                        ),
                        color = colors.textDim,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SplashLoader()
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.splash_foot).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    letterSpacing = 0.9.sp,
                    color = colors.textFaint,
                )
            }
        }
    }
}

@Composable
private fun SplashLoader() {
    val colors = MaterialTheme.aramColors
    val transition = rememberInfiniteTransition(label = "splash")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep",
    )
    Canvas(
        modifier = Modifier
            .width(118.dp)
            .height(3.dp),
    ) {
        drawRoundRect(
            color = colors.surface2,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f),
        )
        val barWidth = size.width * 0.46f
        val x = size.width * (sweep * 4.2f - 1.2f)
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, colors.primaryHi, Color.Transparent),
                startX = x,
                endX = x + barWidth,
            ),
            topLeft = Offset(x, 0f),
            size = androidx.compose.ui.geometry.Size(barWidth, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f),
        )
    }
}

@Composable
fun LanguagePill(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.aramColors
    val context = LocalContext.current
    val soon = stringResource(R.string.lang_coming_soon)
    var expanded by remember { mutableStateOf(false) }

    Box(modifier) {
        Row(
            modifier = Modifier
                .height(32.dp)
                .clip(CircleShape)
                .background(colors.surface1)
                .border(
                    width = 1.dp,
                    color = if (expanded) Color(0x730E9F6E) else colors.border,
                    shape = CircleShape,
                )
                .clickable { expanded = true }
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = AramIcons.globe,
                contentDescription = null,
                tint = colors.primaryHi,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.lang_pill),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = colors.textDim,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = AramIcons.chevronDown,
                contentDescription = null,
                tint = colors.textFaint,
                modifier = Modifier.size(12.dp),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = colors.surface2,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderStrong),
            shadowElevation = 18.dp,
        ) {
            Column(Modifier.width(188.dp).padding(6.dp)) {
                Text(
                    text = stringResource(R.string.lang_title).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    letterSpacing = 0.8.sp,
                    color = colors.textFaint,
                    modifier = Modifier.padding(start = 10.dp, top = 6.dp, bottom = 7.dp),
                )
                LanguageOption(
                    label = "English",
                    selected = true,
                    onClick = { expanded = false },
                )
                LanguageOption(
                    label = "فارسی",
                    selected = false,
                    soon = true,
                    onClick = {
                        expanded = false
                        Toast.makeText(context, soon, Toast.LENGTH_SHORT).show()
                    },
                )
                LanguageOption(
                    label = "Français",
                    selected = false,
                    soon = true,
                    onClick = {
                        expanded = false
                        Toast.makeText(context, soon, Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }
    }
}

@Composable
private fun LanguageOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    soon: Boolean = false,
) {
    val colors = MaterialTheme.aramColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = if (selected) colors.textDim else colors.textFaint,
            modifier = Modifier.weight(1f),
        )
        if (soon) {
            Text(
                text = stringResource(R.string.lang_soon),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = colors.textFaint,
            )
        } else if (selected) {
            Icon(
                imageVector = AramIcons.check,
                contentDescription = null,
                tint = colors.primaryHi,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** The brand tile with the peak glyph from the prototype (not the initial). */
@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    size: Int,
    corner: Int,
    glyphSize: Int,
) {
    val shape = RoundedCornerShape(corner.dp)
    val colors = MaterialTheme.aramColors
    Box(
        modifier = modifier
            .size(size.dp)
            .shadow(
                elevation = (size * 0.3f).dp,
                shape = shape,
                ambientColor = MaterialTheme.colorScheme.primary,
                spotColor = MaterialTheme.colorScheme.primary,
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF5BE7B7), MaterialTheme.colorScheme.primary, Color(0xFF076249)),
                ),
            )
            .border(1.dp, Color(0x4DFFFFFF), shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = AramIcons.aramGlyph,
            contentDescription = null,
            tint = Color(0xFF052018),
            modifier = Modifier.size(glyphSize.dp),
        )
    }
}
