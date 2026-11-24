package com.aram.feature.onboarding.presentation

import android.widget.Toast
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aram.core.designsystem.AramTextStyles
import com.aram.core.designsystem.AramTheme
import com.aram.core.designsystem.aramColors
import com.aram.core.designsystem.component.AramBackground
import com.aram.core.designsystem.component.AramGhostButton
import com.aram.core.designsystem.component.AramMark
import com.aram.core.designsystem.component.AramPrimaryButton
import com.aram.core.designsystem.icon.AramIcons
import com.aram.core.designsystem.state.SubmitState
import com.aram.feature.onboarding.R

@Composable
fun WelcomeRoute(
    modifier: Modifier = Modifier,
    demoCredentials: DemoCredentials? = null,
    viewModel: WelcomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    WelcomeScreen(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onCreateProfile = viewModel::onCreateProfile,
        onOpenSignIn = viewModel::onOpenSignIn,
        onOpenSignUp = viewModel::onOpenSignUp,
        onBack = viewModel::onBack,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
        onTermsChange = viewModel::onTermsChange,
        onSubmit = viewModel::onSubmit,
        onUseDemo = {
            demoCredentials?.let(viewModel::onUseDemo)
        },
        onForgotPassword = {
            Toast.makeText(
                context,
                context.getString(R.string.auth_forgot_password_pending),
                Toast.LENGTH_SHORT,
            ).show()
        },
        demoCredentials = demoCredentials,
        modifier = modifier,
    )
}

@Composable
fun WelcomeScreen(
    uiState: WelcomeUiState,
    onNameChange: (String) -> Unit,
    onCreateProfile: () -> Unit,
    modifier: Modifier = Modifier,
    demoCredentials: DemoCredentials? = null,
    onOpenSignIn: () -> Unit = {},
    onOpenSignUp: () -> Unit = {},
    onBack: () -> Unit = {},
    onEmailChange: (String) -> Unit = {},
    onPasswordChange: (String) -> Unit = {},
    onTogglePasswordVisibility: () -> Unit = {},
    onTermsChange: (Boolean) -> Unit = {},
    onSubmit: () -> Unit = {},
    onUseDemo: () -> Unit = {},
    onForgotPassword: () -> Unit = {},
) {
    if (uiState.screen == AuthScreen.Welcome) {
        WelcomeContent(
            uiState = uiState,
            onNameChange = onNameChange,
            onCreateProfile = onCreateProfile,
            onOpenSignIn = onOpenSignIn,
            onOpenSignUp = onOpenSignUp,
            demoCredentials = demoCredentials,
            modifier = modifier,
        )
    } else {
        AuthFormScreen(
            uiState = uiState,
            demoCredentials = demoCredentials,
            onBack = onBack,
            onNameChange = onNameChange,
            onEmailChange = onEmailChange,
            onPasswordChange = onPasswordChange,
            onTogglePasswordVisibility = onTogglePasswordVisibility,
            onTermsChange = onTermsChange,
            onSubmit = onSubmit,
            onSwitchMode = if (uiState.screen == AuthScreen.SignUp) {
                onOpenSignIn
            } else {
                onOpenSignUp
            },
            onUseDemo = onUseDemo,
            onForgotPassword = onForgotPassword,
            modifier = modifier,
        )
    }
}

@Composable
private fun WelcomeContent(
    uiState: WelcomeUiState,
    onNameChange: (String) -> Unit,
    onCreateProfile: () -> Unit,
    onOpenSignIn: () -> Unit,
    onOpenSignUp: () -> Unit,
    modifier: Modifier = Modifier,
    demoCredentials: DemoCredentials? = null,
) {
    val isSubmitting = uiState.submitState is SubmitState.Submitting
    val hasSubmitError = uiState.submitState is SubmitState.Failed
    val colors = MaterialTheme.aramColors

    AramBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AramMark(size = 46, corner = 15)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Aram",
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
                style = MaterialTheme.typography.headlineLarge,
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.welcome_lead),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = colors.textDim,
                modifier = Modifier.widthIn(max = 250.dp),
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
                icon = AramIcons.lock,
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
                val nameError = when {
                    uiState.showNameError -> stringResource(R.string.welcome_name_error)
                    hasSubmitError -> stringResource(R.string.welcome_save_error)
                    else -> null
                }
                BasicTextField(
                    value = uiState.name,
                    onValueChange = onNameChange,
                    enabled = !isSubmitting,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onCreateProfile() }),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.surface1)
                                .border(
                                    width = 1.dp,
                                    color = if (nameError != null) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        colors.border
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                )
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (uiState.name.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.welcome_name_label),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colors.textFaint,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
                if (nameError != null) {
                    Text(
                        text = nameError,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            AramPrimaryButton(
                text = stringResource(R.string.welcome_create_profile),
                onClick = onCreateProfile,
                loading = isSubmitting,
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))

            AramGhostButton(
                text = stringResource(R.string.welcome_sign_in),
                onClick = onOpenSignIn,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.auth_switch_to_signup),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textDim,
                )
                Text(
                    text = stringResource(R.string.auth_signup_link),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primaryHi,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenSignUp)
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }

            if (demoCredentials != null) {
                Text(
                    text = demoCredentials.hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textFaint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
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
                    center = Offset(size.width - 55.dp.toPx(), 39.dp.toPx()),
                    style = Stroke(width = 24.dp.toPx()),
                )
            }
            .padding(20.dp),
    ) {
        Column {
            Text(
                text = stringResource(R.string.welcome_preview_label),
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 0.5.sp,
                color = Color(0xD9FFFFFF),
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = stringResource(R.string.welcome_preview_amount),
                    style = AramTextStyles.heroAmountCompact,
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
        WelcomeScreen(
            uiState = WelcomeUiState(),
            onNameChange = {},
            onCreateProfile = {},
        )
    }
}
