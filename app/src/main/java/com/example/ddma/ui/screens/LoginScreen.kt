package com.example.ddma.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ddma.data.model.LoginViewModel
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel(),
    onLoginSuccess: (Int) -> Unit = {}
) {
    var usernameOrEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val loginState = viewModel.loginState.collectAsState().value
    val context = LocalContext.current

    // Handle successful login
    LaunchedEffect(loginState.isSuccess) {
        if (loginState.isSuccess) {
            loginState.userData?.let { userData ->
                if (userData.id > 0) {
                    // Show welcome message
                    Toast.makeText(
                        context,
                        "Bienvenido ${userData.username}",
                        Toast.LENGTH_LONG
                    ).show()

                    // Pass the validated user ID to parent after short delay
                    delay(300) // Small delay for better UX
                    onLoginSuccess(userData.id)
                } else {
                    Toast.makeText(
                        context,
                        "Error: ID de usuario inválido",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } ?: run {
                Toast.makeText(
                    context,
                    "Error: Datos de usuario no recibidos",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Handle login errors
    LaunchedEffect(loginState.error) {
        loginState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App title
        Text(
            text = "Dulce Tentación",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Username/Email field
        OutlinedTextField(
            value = usernameOrEmail,
            onValueChange = { usernameOrEmail = it },
            label = { Text("Usuario o Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = loginState.error != null
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = loginState.error != null
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Login button
        Button(
            onClick = {
                when {
                    usernameOrEmail.isBlank() && password.isBlank() -> {
                        Toast.makeText(
                            context,
                            "Ingrese usuario y contraseña",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    usernameOrEmail.isBlank() -> {
                        Toast.makeText(
                            context,
                            "Ingrese su usuario o email",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    password.isBlank() -> {
                        Toast.makeText(
                            context,
                            "Ingrese su contraseña",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else -> {
                        viewModel.login(usernameOrEmail.trim(), password)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !loginState.isLoading
        ) {
            if (loginState.isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("Ingresar")
            }
        }
    }
}