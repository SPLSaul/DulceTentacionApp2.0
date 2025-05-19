package com.example.ddma.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ddma.R
import com.example.ddma.data.api.CarritoService
import com.example.ddma.data.model.CarritoState
import com.example.ddma.data.model.PastelesResponse
import com.example.ddma.data.model.PastelesRepository
import com.example.ddma.di.DependencyProvider
import com.example.ddma.ui.viewmodel.PastelesViewModel
import com.example.ddma.ui.viewmodel.PastelesViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastelesScreen(
    pastelesRepository: PastelesRepository,
    carritoService: CarritoService,
    userId: Int, // User ID parameter
    onNavigateBack: () -> Unit = {},
    onNavigateToCart: (Int) -> Unit
) {
    val context = LocalContext.current
    val factory = remember {
        PastelesViewModelFactory(
            pastelesRepository = pastelesRepository,
            carritoService = carritoService,
            context = context
        )
    }
    val viewModel: PastelesViewModel = viewModel(factory = factory)

    LaunchedEffect(userId) {
        viewModel.setUserId(userId)
    }
    val uiState by viewModel.uiState.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val carritoState by viewModel.carritoState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { message ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = if (carritoState is CarritoState.Error) "Reintentar" else null
                ).let { result ->
                    if (result == SnackbarResult.ActionPerformed && carritoState is CarritoState.Error) {
                        viewModel.agregarAlCarrito(uiState.pasteles.firstOrNull()?.id ?: return@let)
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Dulce Tentación - Pasteles") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadPasteles() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
                    }
                    IconButton(onClick = { onNavigateToCart(userId) }) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Ver carrito")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.error != null -> {
                    ErrorView(
                        message = uiState.error!!,
                        onRetry = { viewModel.loadPasteles() }
                    )
                }
                uiState.pasteles.isEmpty() -> {
                    EmptyView(
                        message = "No hay pasteles disponibles",
                        onRetry = { viewModel.loadPasteles() }
                    )
                }
                else -> {
                    PastelesList(
                        pasteles = uiState.pasteles,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
@Composable
fun PastelesList(
    pasteles: List<PastelesResponse>,
    viewModel: PastelesViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(pasteles) { pastel ->
            PastelItem(
                pastel = pastel,
                viewModel = viewModel
            )
        }
    }
}

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
@Composable
fun PastelItem(
    pastel: PastelesResponse,
    viewModel: PastelesViewModel,
    modifier: Modifier = Modifier
) {
    val carritoState by viewModel.carritoState.collectAsState()
    var isButtonEnabled by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { /* TODO: Navegar a detalle */ },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("android.resource://${LocalContext.current.packageName}/drawable/${pastel.imagen.substringBeforeLast(".")}")
                        .crossfade(true)
                        .error(R.drawable.broken)
                        .build(),
                    contentDescription = pastel.nombre,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(16.dp)
                    .weight(1f)
            ) {
                Text(
                    text = pastel.nombre,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = pastel.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$${pastel.precio}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    when (carritoState) {
                        is CarritoState.Error -> {
                            Button(
                                onClick = {
                                    if (isButtonEnabled) {
                                        isButtonEnabled = false
                                        viewModel.agregarAlCarrito(pastel.id)
                                        coroutineScope.launch {
                                            delay(1000)
                                            isButtonEnabled = true
                                        }
                                    }
                                },
                                enabled = isButtonEnabled
                            ) {
                                Text("Reintentar")
                            }
                        }
                        else -> {
                            Button(
                                onClick = {
                                    if (isButtonEnabled) {
                                        isButtonEnabled = false
                                        viewModel.agregarAlCarrito(pastel.id)
                                        coroutineScope.launch {
                                            delay(1000)
                                            isButtonEnabled = true
                                        }
                                    }
                                },
                                enabled = isButtonEnabled && carritoState !is CarritoState.Loading
                            ) {
                                if (carritoState is CarritoState.Loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text("Agregar al carrito")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(carritoState) {
        if (carritoState is CarritoState.Success) {
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            val runnable = Runnable { viewModel.resetCarritoState() }
            handler.postDelayed(runnable, 1000)
            onDispose {
                handler.removeCallbacks(runnable)
            }
        }
        onDispose { }
    }
}

@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error: $message",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRetry) {
            Text("Reintentar")
        }
    }
}

@Composable
fun EmptyView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRetry) {
            Text("Reintentar")
        }
    }
}