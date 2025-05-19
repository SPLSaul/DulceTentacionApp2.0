package com.example.ddma.ui.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresExtension
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ddma.data.api.CarritoService
import com.example.ddma.data.model.CarritoItemRequest
import com.example.ddma.data.model.CarritoItemResponse
import com.example.ddma.data.model.CarritoState
import com.example.ddma.data.model.PastelesResponse
import com.example.ddma.data.model.PastelesUiState
import com.example.ddma.data.model.PastelesRepository
import com.example.ddma.data.model.RepositoryResult
import com.google.gson.stream.MalformedJsonException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException

class PastelesViewModel constructor(
    private val pastelesRepository: PastelesRepository,
    private val carritoService: CarritoService,
    private val context: Context
) : ViewModel() {

    private val _userId = MutableStateFlow(0)
    val userId: StateFlow<Int> = _userId.asStateFlow()

    private val _uiState = MutableStateFlow(PastelesUiState())
    val uiState: StateFlow<PastelesUiState> = _uiState.asStateFlow()

    private val _carritoState = MutableStateFlow<CarritoState>(CarritoState.Idle)
    val carritoState: StateFlow<CarritoState> = _carritoState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        loadPasteles()
    }

    fun setUserId(id: Int) {
        _userId.value = id
        Log.d("USER_ID", "UserId updated to: $id")
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }

    fun loadPasteles() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = pastelesRepository.getPasteles()) {
                is RepositoryResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        pasteles = result.data,
                        isLoading = false
                    )
                }
                is RepositoryResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.exception.message ?: "Unknown error",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun agregarAlCarrito(pastelId: Int, cantidad: Int = 1) {
        viewModelScope.launch {
            val currentUserId = _userId.value
            if (currentUserId == 0) {
                Log.e("USER_ID", "Invalid user ID (0) when trying to add to cart")
                _carritoState.value = CarritoState.Error("Usuario no identificado")
                _toastMessage.value = "Por favor, inicia sesión primero"
                return@launch
            }

            Log.d("USER_ACTION", "User $currentUserId adding product $pastelId (qty: $cantidad) to cart")

            if (!isNetworkAvailable()) {
                val mensajeError = "Sin conexión a internet"
                Log.w("NETWORK", "No internet connection")
                _carritoState.value = CarritoState.Error(mensajeError)
                _toastMessage.value = mensajeError
                return@launch
            }

            _carritoState.value = CarritoState.Loading

            try {
                val request = CarritoItemRequest(
                    userId = currentUserId,
                    productId = pastelId,
                    quantity = cantidad
                )

                Log.i("API_REQUEST", "POST /api/Carritos/items - $request")

                val response = carritoService.agregarItemAlCarrito(request)

                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        Log.i("API_SUCCESS", "Added to cart: $body")
                        _carritoState.value = CarritoState.Success(body)
                        _toastMessage.value = "${body.nombrePastel} agregado al carrito"
                    } ?: run {
                        Log.e("API_ERROR", "Empty response body")
                        _carritoState.value = CarritoState.Error("Respuesta vacía del servidor")
                        _toastMessage.value = "Error al agregar al carrito"
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "No error body"
                    Log.e("API_ERROR", "Error ${response.code()}: $errorBody")
                    _carritoState.value = CarritoState.Error(
                        when (response.code()) {
                            400 -> "Datos inválidos"
                            401 -> "No autorizado"
                            404 -> "Producto no encontrado"
                            else -> "Error del servidor (${response.code()})"
                        }
                    )
                    _toastMessage.value = "Error al agregar al carrito"
                }
            } catch (e: Exception) {
                Log.e("API_EXCEPTION", "Error adding to cart", e)
                _carritoState.value = CarritoState.Error(
                    when (e) {
                        is SocketTimeoutException -> "Tiempo de espera agotado"
                        is IOException -> "Error de conexión"
                        else -> "Error inesperado"
                    }
                )
                _toastMessage.value = "Error al agregar al carrito"
            }
        }
    }

    fun resetCarritoState() {
        _carritoState.value = CarritoState.Idle
    }
}