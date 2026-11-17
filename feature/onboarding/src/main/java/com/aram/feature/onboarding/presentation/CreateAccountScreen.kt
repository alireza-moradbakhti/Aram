package com.aram.feature.onboarding.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aram.core.designsystem.AramTheme
import com.aram.core.designsystem.aramColors
import com.aram.core.designsystem.component.AramBackground
import com.aram.core.designsystem.component.AramPrimaryButton
import com.aram.core.designsystem.icon.AramIcons
import com.aram.core.designsystem.state.SubmitState
import com.aram.feature.onboarding.R

@Composable
fun CreateAccountRoute(
    onBack: () -> Unit,
    onLogIn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WelcomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CreateAccountScreen(
        uiState = uiState,
        onBack = onBack,
        onLogIn = onLogIn,
        onNameChange = viewModel::onNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePassword = viewModel::onTogglePasswordVisibility,
        onAcceptedTermsChange = viewModel::onAcceptedTermsChange,
        onCreateAccount = viewModel::onCreateAccount,
        modifier = modifier,
    )
}

@Composable
fun CreateAccountScreen(
    uiState: WelcomeUiState,
    onBack: () -> Unit,
    onLogIn: () -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onAcceptedTermsChange: (Boolean) -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.aramColors
    val isSubmitting = uiState.submitState is SubmitState.Submitting
    val submitFailed = uiState.submitState is SubmitState.Failed

    AramBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 0.dp, top = 8.dp)
                    .size(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AramIcons.chevronLeft,
                    contentDescription = stringResource(R.string.auth_back),
                    tint = colors.textDim,
                    modifier = Modifier.size(20.dp),
                )
            }

            Text(
                text = stringResource(R.string.auth_title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                ),
                modifier = Modifier.padding(top = 18.dp),
            )
            Text(
                text = stringResource(R.string.auth_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color = colors.textDim,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )

            AuthField(
                label = stringResource(R.string.auth_full_name),
                icon = AramIcons.user,
                value = uiState.name,
                placeholder = stringResource(R.string.auth_full_name_hint),
                enabled = !isSubmitting,
                error = uiState.showNameError,
                errorText = stringResource(R.string.auth_error_name),
                onValueChange = onNameChange,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
            )
            Spacer(Modifier.height(16.dp))
            AuthField(
                label = stringResource(R.string.auth_email),
                icon = AramIcons.mail,
                value = uiState.email,
                placeholder = stringResource(R.string.auth_email_hint),
                enabled = !isSubmitting,
                error = uiState.showEmailError,
                errorText = stringResource(R.string.auth_error_email),
                onValueChange = onEmailChange,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            )
            Spacer(Modifier.height(16.dp))

            PasswordField(
                value = uiState.password,
                visible = uiState.passwordVisible,
                score = uiState.passwordScore,
                showError = uiState.showPasswordError,
                enabled = !isSubmitting,
                onValueChange = onPasswordChange,
                onToggleVisibility = onTogglePassword,
            )

            Spacer(Modifier.height(16.dp))

            TermsCheckbox(
                checked = uiState.acceptedTerms,
                onCheckedChange = onAcceptedTermsChange,
            )
            if (uiState.showTermsError) {
                Text(
                    text = stringResource(R.string.auth_error_terms),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.danger,
                    modifier = Modifier.padding(start = 30.dp, top = 6.dp),
                )
            }

            Spacer(Modifier.height(20.dp))

            AramPrimaryButton(
                text = if (isSubmitting) {
                    stringResource(R.string.auth_creating_account)
                } else {
                    stringResource(R.string.auth_create_account)
                },
                onClick = onCreateAccount,
                enabled = !isSubmitting,
                loading = isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )

            if (submitFailed) {
                Text(
                    text = stringResource(R.string.auth_save_error),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.danger,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.auth_have_account),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = colors.textDim,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.auth_log_in),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = colors.primaryHi,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onLogIn)
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun AuthField(
    label: String,
    icon: ImageVector,
    value: String,
    placeholder: String,
    enabled: Boolean,
    error: Boolean,
    errorText: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
) {
    val colors = MaterialTheme.aramColors
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = colors.textDim,
        )
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(),
            interactionSource = interactionSource,
            decorationBox = { inner ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(shape)
                        .background(colors.surface1)
                        .border(
                            width = 1.dp,
                            color = when {
                                error -> colors.danger
                                focused -> MaterialTheme.colorScheme.primary
                                else -> colors.border
                            },
                            shape = shape,
                        )
                        .padding(start = 15.dp, end = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colors.textFaint,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(11.dp))
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.textFaint,
                            )
                        }
                        inner()
                    }
                }
            },
        )
        if (error) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.labelSmall,
                color = colors.danger,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp),
            )
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    visible: Boolean,
    score: Int,
    showError: Boolean,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
) {
    val colors = MaterialTheme.aramColors
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    val strengthColor = when (score) {
        1 -> colors.danger
        2 -> colors.secondaryHi
        else -> colors.primaryHi
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.auth_password),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = colors.textDim,
            )
            if (score > 0) {
                Text(
                    text = stringResource(strengthLabelRes(score)),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = strengthColor,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(),
            interactionSource = interactionSource,
            decorationBox = { inner ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(shape)
                        .background(colors.surface1)
                        .border(
                            width = 1.dp,
                            color = when {
                                showError -> colors.danger
                                focused -> MaterialTheme.colorScheme.primary
                                else -> colors.border
                            },
                            shape = shape,
                        )
                        .padding(start = 15.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = AramIcons.lock,
                        contentDescription = null,
                        tint = colors.textFaint,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(11.dp))
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(
                                text = "••••••••",
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.textFaint,
                            )
                        }
                        inner()
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onToggleVisibility),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (visible) AramIcons.eyeOff else AramIcons.eye,
                            contentDescription = stringResource(
                                if (visible) R.string.auth_hide_password else R.string.auth_show_password,
                            ),
                            tint = colors.textFaint,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            },
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i < score) strengthColor else colors.surface2),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (showError) {
                stringResource(R.string.auth_error_password)
            } else {
                stringResource(strengthHintRes(score))
            },
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = if (showError) colors.danger else colors.textFaint,
        )
    }
}

@Composable
private fun TermsCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.aramColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (checked) MaterialTheme.colorScheme.primary else Color.Transparent)
                .border(
                    width = 1.dp,
                    color = if (checked) Color.Transparent else colors.borderStrong,
                    shape = RoundedCornerShape(6.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    imageVector = AramIcons.check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.auth_terms),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
            color = colors.textDim,
        )
    }
}

private fun strengthLabelRes(score: Int): Int = when (score) {
    1 -> R.string.strength_1
    2 -> R.string.strength_2
    else -> R.string.strength_3
}

private fun strengthHintRes(score: Int): Int = when (score) {
    0 -> R.string.strength_hint_0
    1 -> R.string.strength_hint_1
    2 -> R.string.strength_hint_2
    else -> R.string.strength_hint_3
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun CreateAccountScreenPreview() {
    AramTheme {
        CreateAccountScreen(
            uiState = WelcomeUiState(),
            onBack = {},
            onLogIn = {},
            onNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePassword = {},
            onAcceptedTermsChange = {},
            onCreateAccount = {},
        )
    }
}
