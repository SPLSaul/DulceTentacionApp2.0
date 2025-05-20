package com.example.ddma.ui.screens

import android.app.Activity
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.rememberPaymentSheet

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagoStripeScreen(
    clientSecret: String,
    onPaymentCompleted: () -> Unit,
    onPaymentFailed: (String) -> Unit,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current

    // Estado para seguir el progreso del pago
    var isProcessing by remember { mutableStateOf(false) }

    // Usar rememberPaymentSheet en lugar de crear la instancia directamente
    val paymentSheet = rememberPaymentSheet { result ->
        when (result) {
            is PaymentSheetResult.Completed -> {
                Log.d("PagoStripeScreen", "Pago completado con éxito")
                onPaymentCompleted()
            }
            is PaymentSheetResult.Canceled -> {
                Log.d("PagoStripeScreen", "Pago cancelado por el usuario")
                onPaymentFailed("Pago cancelado")
            }
            is PaymentSheetResult.Failed -> {
                Log.e("PagoStripeScreen", "Error en el pago: ${result.error.localizedMessage}")
                onPaymentFailed("Error: ${result.error.localizedMessage}")
            }
        }
        isProcessing = false
    }

    // Configuración para la UI de pago
    val paymentSheetConfig = remember {
        PaymentSheet.Configuration(
            merchantDisplayName = "Dulce Tentación",
            allowsDelayedPaymentMethods = true
        )
    }

    // Presentar PaymentSheet cuando cambie el clientSecret
    LaunchedEffect(clientSecret) {
        if (clientSecret.isNotEmpty() && !isProcessing) {
            isProcessing = true
            try {
                paymentSheet.presentWithPaymentIntent(
                    paymentIntentClientSecret = clientSecret,
                    configuration = paymentSheetConfig
                )
            } catch (e: Exception) {
                Log.e("PagoStripeScreen", "Error al presentar PaymentSheet", e)
                isProcessing = false
                onPaymentFailed("Error al iniciar el pago: ${e.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Procesando Pago") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed, enabled = !isProcessing) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (isProcessing) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Procesando tu pago...")
                    Text(
                        text = "Si no aparece la pantalla de pago, presiona el botón 'Regresar' e intenta nuevamente.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Esperando a que se inicie el pago...")
                    Button(
                        onClick = onBackPressed
                    ) {
                        Text("Regresar al carrito")
                    }
                }
            }
        }
    }
}