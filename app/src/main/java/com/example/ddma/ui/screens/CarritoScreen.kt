package com.example.ddma.ui.screens

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ddma.R
import com.example.ddma.data.api.CarritoApiService
import com.example.ddma.data.model.CarritoDto
import com.example.ddma.data.model.CarritoItemDto
import com.example.ddma.data.model.CarritoCustomItemDto
import com.example.ddma.data.repositories.PaymentRepository  // Add this import
import com.example.ddma.di.DependencyProvider
import com.example.ddma.ui.viewmodel.CarritoViewModel
import com.example.ddma.ui.viewmodel.CarritoViewModelFactory
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarritoScreen(
    userId: Int,
    onNavigateBack: () -> Unit
) {
    // Use the existing PaymentRepository from DependencyProvider
    val factory = remember {
        CarritoViewModelFactory(DependencyProvider.paymentRepository)
    }

    val viewModel: CarritoViewModel = viewModel(factory = factory)
    val cartState by viewModel.cartState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val clientSecret by viewModel.clientSecret.collectAsState()
    val mostrarPantallaStripe by viewModel.mostrarPantallaStripe.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    if (mostrarPantallaStripe && clientSecret != null) {
        PagoStripeScreen(
            clientSecret = clientSecret!!,
            onPaymentCompleted = {
                viewModel.clearCart()
                viewModel.setMostrarPantallaStripe(false)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("¡Pago completado con éxito!")
                }
            },
            onPaymentFailed = { error ->
                viewModel.setMostrarPantallaStripe(false)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Error en el pago: $error")
                }
            },
            onBackPressed = {
                viewModel.setMostrarPantallaStripe(false)
            }
        )
    } else {
        // Inicializar con el ID de usuario
        LaunchedEffect(userId) {
            if (userId > 0) {
                viewModel.setUserId(userId)
                viewModel.fetchCartWithRetry()
            } else {
                Log.e("CarritoScreen", "Error: Acción no permitida - userId: $userId")
            }
        }

        LaunchedEffect(errorMessage) {
            errorMessage?.let { message ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message)
                    viewModel.clearErrorMessage()
                }
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Mi Carrito") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                        }
                    },
                    actions = {
                        if (cartState?.items?.isNotEmpty() == true || cartState?.customItems?.isNotEmpty() == true) {
                            IconButton(
                                onClick = {
                                    if (userId > 0) {
                                        viewModel.clearCart()
                                    } else {
                                        Log.e("CarritoScreen", "Error: Acción no permitida - userId: $userId")
                                    }
                                },
                                enabled = !isLoading && userId > 0
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Vaciar carrito")
                            }
                        }
                    }
                )
            },
            bottomBar = {
                cartState?.let { cart ->
                    if (cart.items.isNotEmpty() || cart.customItems.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                text = "Total: $${"%.2f".format(cart.total)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.End)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.procesarPagoSimple() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoading && userId > 0
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text("Proceder al pago")
                                }
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when {
                    userId <= 0 -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Por favor inicie sesión para ver su carrito")
                            Button(
                                onClick = { /* Navigate to login */ },
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text("Ir a inicio de sesión")
                            }
                        }
                    }

                    isLoading && cartState == null -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    cartState?.items?.isEmpty() == true && cartState?.customItems?.isEmpty() == true -> {
                        EmptyCartView(
                            onRetry = { viewModel.fetchCartWithRetry() },
                            isLoading = isLoading
                        )
                    }

                    else -> {
                        cartState?.let { cart ->
                            CarritoList(
                                cart = cart,
                                viewModel = viewModel,
                                userId = userId
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyCartView(
    onRetry: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Tu carrito está vacío",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Volver a intentar")
            }
        }
    }
}

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
@Composable
fun CarritoList(
    cart: CarritoDto,
    viewModel: CarritoViewModel,
    userId: Int,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(cart.items) { item ->
            CartItem(
                item = item,
                onQuantityChange = { newQuantity ->
                    if (userId > 0) {
                        viewModel.updateItemQuantity(item.id, newQuantity)
                    } else {
                        Log.e("CarritoScreen", "Error: Acción no permitida - userId: $userId")
                    }
                },
                onRemove = {
                    if (userId > 0) {
                        viewModel.removeItem(item.id)
                    } else {
                        Log.e("CarritoScreen", "Error: Acción no permitida - userId: $userId")
                    }
                },
                userId = userId
            )
        }
        if (cart.customItems.isNotEmpty()) {
            item {
                Text(
                    text = "Productos Personalizados",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(cart.customItems) { customItem ->
                CustomCartItem(customItem)
            }
        }
    }
}

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
@Composable
fun CartItem(
    item: CarritoItemDto,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit,
    userId: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.imagenPastel)
                    .crossfade(true)
                    .error(R.drawable.broken)
                    .build(),
                contentDescription = item.nombrePastel,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.nombrePastel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Precio: $${"%.2f".format(item.precioUnitario)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Subtotal: $${"%.2f".format(item.subtotal)}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (userId > 0 && item.cantidad > 1) {
                                onQuantityChange(item.cantidad - 1)
                            }
                        },
                        enabled = userId > 0 && item.cantidad > 1
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Reducir cantidad")
                    }
                    Text(
                        text = item.cantidad.toString(),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    IconButton(
                        onClick = {
                            if (userId > 0) {
                                onQuantityChange(item.cantidad + 1)
                            }
                        },
                        enabled = userId > 0
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Aumentar cantidad")
                    }
                }
            }

            IconButton(
                onClick = onRemove,
                enabled = userId > 0
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar ítem")
            }
        }
    }
}

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
@Composable
fun CustomCartItem(
    item: CarritoCustomItemDto,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.imagenPersonalizado)
                    .crossfade(true)
                    .error(R.drawable.broken)
                    .build(),
                contentDescription = item.nombrePersonalizado,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = item.nombrePersonalizado,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Precio: $${"%.2f".format(item.precioUnitario)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Cantidad: ${item.cantidad}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Subtotal: $${"%.2f".format(item.subtotal)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}