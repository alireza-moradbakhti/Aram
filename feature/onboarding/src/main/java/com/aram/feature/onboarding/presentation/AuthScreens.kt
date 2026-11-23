package com.aram.feature.onboarding.presentation

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aram.core.designsystem.AramTheme
import com.aram.core.designsystem.aramColors
import com.aram.core.designsystem.component.AramBackground
import com.aram.core.designsystem.component.AramPrimaryButton
import com.aram.core.designsystem.icon.AramIcons
import com.aram.core.designsystem.state.SubmitState
import com.aram.core.model.AppError
import com.aram.feature.onboarding.R

@Composable
fun AuthFormScreen(
    uiState: WelcomeUiState,
    demoCredentials: DemoCredentials?,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onTermsChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onSwitchMode: () -> Unit,
    onUseDemo: () -> Unit,
    onForgotPassword: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSignUp = uiState.screen == AuthScreen.SignUp
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
            BackButton(onClick = onBack)

            Spacer(Modifier.height(18.dp))

            if (isSignUp) {
                AuthMark()
                Spacer(Modifier.height(16.dp))
            }

            Text(
                text = stringResource(
                    if (isSignUp) R.string.auth_signup_title else R.string.auth_login_title,
                ),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(
                    if (isSignUp) R.string.auth_signup_sub else R.string.auth_login_sub,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textDim,
            )

            Spacer(Modifier.height(22.dp))

            if (uiState.submitState is SubmitState.Failed) {
                AuthAlert(message = authErrorMessage((uiState.submitState as SubmitState.Failed).error))
                Spacer(Modifier.height(14.dp))
            }

            if (isSignUp) {
                AuthField(
                    label = stringResource(R.string.auth_full_name),
                    value = uiState.name,
                    onValueChange = onNameChange,
                    placeholder = "Aram Moradi",
                    icon = AramIcons.person,
                    enabled = !uiState.isSubmitting,
                    capitalization = KeyboardCapitalization.Words,
                    error = uiState.fieldErrors["displayName"],
                )
                Spacer(Modifier.height(14.dp))
            }

            AuthField(
                label = stringResource(R.string.auth_email),
                value = uiState.email,
                onValueChange = onEmailChange,
                placeholder = "you@example.com",
                icon = AramIcons.mail,
                enabled = !uiState.isSubmitting,
                keyboardType = KeyboardType.Email,
                error = uiState.fieldErrors["email"],
            )

            Spacer(Modifier.height(14.dp))

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.auth_password),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textDim,
                    )
                    if (isSignUp) {
                        StrengthLabel(uiState.passwordStrength)
                    }
                }
                Spacer(Modifier.height(8.dp))
                AuthField(
                    label = null,
                    value = uiState.password,
                    onValueChange = onPasswordChange,
                    placeholder = "••••••••",
                    icon = AramIcons.lock,
                    enabled = !uiState.isSubmitting,
                    keyboardType = KeyboardType.Password,
                    visualTransformation = if (uiState.passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailing = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onTogglePasswordVisibility),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (uiState.passwordVisible) {
                                    AramIcons.eyeOff
                                } else {
                                    AramIcons.eye
                                },
                                contentDescription = stringResource(
                                    if (uiState.passwordVisible) {
                                        R.string.auth_hide_password
                                    } else {
                                        R.string.auth_show_password
                                    },
                                ),
                                tint = colors.textFaint,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    },
                    error = uiState.fieldErrors["password"],
                )
                if (isSignUp) {
                    Spacer(Modifier.height(8.dp))
                    PasswordStrengthBar(strength = uiState.passwordStrength)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.auth_password_hint),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.sp,
                        ),
                        color = colors.textFaint,
                    )
                }
            }

            if (isSignUp) {
                Spacer(Modifier.height(16.dp))
                TermsRow(
                    checked = uiState.termsAccepted,
                    enabled = !uiState.isSubmitting,
                    onCheckedChange = onTermsChange,
                )
                uiState.fieldErrors["terms"]?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 30.dp, top = 6.dp),
                    )
                }
            } else {
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        text = stringResource(R.string.auth_forgot_password),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primaryHi,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onForgotPassword)
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            AramPrimaryButton(
                text = stringResource(
                    if (isSignUp) R.string.auth_signup_action else R.string.auth_login_action,
                ),
                onClick = onSubmit,
                loading = uiState.isSubmitting,
                enabled = !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )

            if (uiState.isSubmitting) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        if (isSignUp) R.string.auth_signup_loading else R.string.auth_login_loading,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textFaint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(14.dp))

            SwitchModeRow(
                isSignUp = isSignUp,
                onSwitchMode = onSwitchMode,
            )

            if (demoCredentials != null) {
                Spacer(Modifier.height(14.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = demoCredentials.hint,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.sp,
                        ),
                        color = colors.textFaint,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.auth_use_demo),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primaryHi,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onUseDemo)
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    val colors = MaterialTheme.aramColors
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = AramIcons.chevronLeft,
            contentDescription = stringResource(R.string.auth_back),
            tint = colors.textDim,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun AuthMark() {
    val colors = MaterialTheme.aramColors
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(colors.surface2)
            .border(1.dp, colors.borderStrong, RoundedCornerShape(15.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = AramIcons.key,
            contentDescription = null,
            tint = colors.primaryHi,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun AuthField(
    label: String?,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
    error: String? = null,
) {
    val colors = MaterialTheme.aramColors
    val shape = RoundedCornerShape(14.dp)

    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textDim,
            )
            Spacer(Modifier.height(8.dp))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                capitalization = capitalization,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(shape)
                        .background(colors.surface1)
                        .border(
                            width = 1.dp,
                            color = if (error != null) MaterialTheme.colorScheme.error else colors.border,
                            shape = shape,
                        )
                        .padding(start = 14.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colors.textFaint,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.textFaint,
                            )
                        }
                        innerTextField()
                    }
                    trailing?.invoke()
                }
            },
        )
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp),
            )
        }
    }
}

@Composable
private fun AuthAlert(message: String) {
    val colors = MaterialTheme.aramColors
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.dangerSoft)
            .border(1.dp, colors.danger.copy(alpha = 0.45f), shape)
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = AramIcons.alert,
            contentDescription = null,
            tint = colors.danger,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
            color = colors.danger,
        )
    }
}

@Composable
private fun StrengthLabel(strength: PasswordStrength) {
    if (strength == PasswordStrength.Empty) return
    Text(
        text = stringResource(
            when (strength) {
                PasswordStrength.Weak -> R.string.auth_strength_weak
                PasswordStrength.Fair -> R.string.auth_strength_fair
                PasswordStrength.Strong -> R.string.auth_strength_strong
                PasswordStrength.Empty -> R.string.auth_strength_weak
            },
        ),
        style = MaterialTheme.typography.labelSmall,
        color = strengthColor(strength),
    )
}

@Composable
private fun PasswordStrengthBar(strength: PasswordStrength) {
    val colors = MaterialTheme.aramColors
    val filled = when (strength) {
        PasswordStrength.Empty -> 0
        PasswordStrength.Weak -> 1
        PasswordStrength.Fair -> 2
        PasswordStrength.Strong -> 3
    }
    val tone: Color = if (strength == PasswordStrength.Empty) colors.surface2 else strengthColor(strength)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(if (index < filled) tone else colors.surface2),
            )
        }
    }
}

@Composable
private fun strengthColor(strength: PasswordStrength): Color = when (strength) {
    PasswordStrength.Weak -> MaterialTheme.aramColors.danger
    PasswordStrength.Fair -> MaterialTheme.colorScheme.secondary
    PasswordStrength.Strong -> MaterialTheme.aramColors.primaryHi
    PasswordStrength.Empty -> MaterialTheme.aramColors.textFaint
}

@Composable
private fun TermsRow(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.aramColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (checked) MaterialTheme.colorScheme.primary else Color.Transparent)
                .border(1.5.dp, if (checked) MaterialTheme.colorScheme.primary else colors.borderStrong, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    imageVector = AramIcons.check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.auth_terms_check),
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
            color = colors.textDim,
        )
    }
}

@Composable
private fun SwitchModeRow(isSignUp: Boolean, onSwitchMode: () -> Unit) {
    val colors = MaterialTheme.aramColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(
                if (isSignUp) R.string.auth_switch_to_login else R.string.auth_switch_to_signup,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textDim,
        )
        Text(
            text = stringResource(if (isSignUp) R.string.auth_login_link else R.string.auth_signup_link),
            style = MaterialTheme.typography.labelLarge,
            color = colors.primaryHi,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onSwitchMode)
                .padding(horizontal = 4.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun authErrorMessage(error: AppError): String = when (error) {
    AppError.Unauthorized -> stringResource(R.string.auth_error_credentials)
    AppError.Offline -> stringResource(R.string.auth_error_offline)
    AppError.Timeout -> stringResource(R.string.auth_error_timeout)
    is AppError.RateLimited -> stringResource(R.string.auth_error_rate_limited)
    is AppError.Conflict -> error.message ?: stringResource(R.string.auth_error_conflict)
    AppError.Forbidden -> stringResource(R.string.auth_error_forbidden)
    is AppError.Storage -> stringResource(R.string.auth_error_storage)
    else -> stringResource(R.string.auth_error_generic)
}

@Suppress("unused")
@Composable
private fun AuthFormPreview() {
    AramTheme {
        AuthFormScreen(
            uiState = WelcomeUiState(screen = AuthScreen.SignUp),
            demoCredentials = null,
            onBack = {},
            onNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onTermsChange = {},
            onSubmit = {},
            onSwitchMode = {},
            onUseDemo = {},
            onForgotPassword = {},
        )
    }
}
