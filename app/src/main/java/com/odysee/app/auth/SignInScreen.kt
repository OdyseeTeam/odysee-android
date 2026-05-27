package com.odysee.app.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree

import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odysee.app.core.data.auth.AuthState

@Suppress("DEPRECATION")
@androidx.compose.ui.ExperimentalComposeUiApi
private fun Modifier.autofillField(
    autofillTypes: List<AutofillType>,
    onFill: (String) -> Unit,
): Modifier = composed {
    val autofill = LocalAutofill.current
    val node = remember {
        AutofillNode(autofillTypes = autofillTypes, onFill = onFill)
    }
    LocalAutofillTree.current += node
    this
        .onGloballyPositioned { node.boundingBox = it.boundsInWindow() }
        .onFocusChanged { focusState ->
            autofill?.run {
                if (focusState.isFocused) requestAutofillForNode(node)
                else cancelAutofillForNode(node)
            }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    viewModel: SignInViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)

    LaunchedEffect(Unit) {
        viewModel.dismissEvents.collect { onBack() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (authState is AuthState.SignedIn) "Account" else "Sign in") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (val s = authState) {
                is AuthState.SignedIn -> SignedInContent(
                    email = s.user.email ?: "",
                    isSubmitting = ui.isSubmitting,
                    onSignOut = viewModel::signOut,
                )
                else -> SignInForm(
                    isSubmitting = ui.isSubmitting,
                    onSignIn = viewModel::signIn,
                    onSignUp = viewModel::signUp,
                    onForgotPassword = viewModel::requestPasswordReset,
                )
            }
            ui.statusMessage?.let { msg ->
                Spacer(Modifier.size(4.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (ui.statusIsError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    },
                )
            }
        }
    }
}

@Composable
private fun SignInForm(
    isSubmitting: Boolean,
    onSignIn: (String, String?) -> Unit,
    onSignUp: (String, String?) -> Unit,
    onForgotPassword: (String) -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Text(
        text = "Sign in to Odysee",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Text(
        text = "Enter your email. Add your password to sign in directly, or leave it blank to receive a verification email.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.size(8.dp))
    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Email") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier
            .fillMaxWidth()
            .autofillField(
                autofillTypes = listOf(AutofillType.EmailAddress, AutofillType.Username),
                onFill = { email = it },
            ),
        enabled = !isSubmitting,
    )
    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password (optional)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { showPassword = !showPassword }) {
                Icon(
                    imageVector = if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (showPassword) "Hide password" else "Show password",
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .autofillField(
                autofillTypes = listOf(AutofillType.Password),
                onFill = { password = it },
            ),
        enabled = !isSubmitting,
    )
    Spacer(Modifier.size(8.dp))
    Button(
        onClick = { onSignIn(email, password.takeIf { it.isNotBlank() }) },
        enabled = !isSubmitting,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (isSubmitting) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Text("Sign in")
        }
    }
    OutlinedButton(
        onClick = { onSignUp(email, password.takeIf { it.isNotBlank() }) },
        enabled = !isSubmitting,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Create account")
    }
    androidx.compose.material3.TextButton(
        onClick = { onForgotPassword(email) },
        enabled = !isSubmitting,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Forgot password?")
    }
}

@Composable
private fun SignedInContent(
    email: String,
    isSubmitting: Boolean,
    onSignOut: () -> Unit,
) {
    Text(
        text = "Signed in",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Text(
        text = email,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.size(16.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
    Spacer(Modifier.size(16.dp))
    OutlinedButton(
        onClick = onSignOut,
        enabled = !isSubmitting,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Sign out")
    }
}
