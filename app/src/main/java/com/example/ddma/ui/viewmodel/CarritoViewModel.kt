package com.example.ddma.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ddma.data.api.CarritoApiService
import com.example.ddma.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.io.IOException
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.*

class CarritoViewModel(
    private val carritoApiService: CarritoApiService,
    context: Context
) : ViewModel() {
    private var _userId = 0
    private val _cartState = MutableStateFlow<CarritoDto?>(null)
    val cartState: StateFlow<CarritoDto?> = _cartState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun setUserId(userId: Int) {
        _userId = userId
        fetchCart()
    }

    fun fetchCart() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = carritoApiService.getUserCart(_userId)
                when {
                    response.isSuccessful -> {
                        _cartState.value = response.body() ?: createEmptyCart()
                    }
                    response.code() == 500 -> {
                        _errorMessage.value = "Server error. Please try again later."
                        // Try to create an empty cart as fallback
                        _cartState.value = createEmptyCart()
                    }
                    else -> {
                        _errorMessage.value = "Error ${response.code()}: ${response.message()}"
                    }
                }
            } catch (e: SocketTimeoutException) {
                _errorMessage.value = "Request timed out. Please check your connection."
            } catch (e: IOException) {
                _errorMessage.value = "Network error. Please check your internet connection."
            } catch (e: Exception) {
                _errorMessage.value = "Unexpected error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    private var retryCount = 0
    private val MAX_RETRIES = 2

    fun fetchCartWithRetry() {
        viewModelScope.launch {
            _isLoading.value = true
            var success = false

            while (retryCount < MAX_RETRIES && !success) {
                try {
                    val response = carritoApiService.getUserCart(_userId)
                    when {
                        response.isSuccessful -> {
                            _cartState.value = response.body() ?: createEmptyCart()
                            success = true
                            retryCount = 0
                        }
                        response.code() == 500 -> {
                            retryCount++
                            if (retryCount >= MAX_RETRIES) {
                                _errorMessage.value = "Server error. Showing empty cart."
                                _cartState.value = createEmptyCart()
                            }
                            delay((1000L * retryCount).toLong()) // Fixed delay
                        }
                        else -> {
                            _errorMessage.value = "Error ${response.code()}: ${response.message()}"
                            _cartState.value = createEmptyCart()
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    retryCount++
                    if (retryCount >= MAX_RETRIES) {
                        _errorMessage.value = when (e) {
                            is SocketTimeoutException -> "Connection timed out"
                            is IOException -> "Network error"
                            else -> "Unexpected error"
                        }
                        _cartState.value = createEmptyCart()
                    }
                    delay((1000L * retryCount).toLong()) // Fixed delay
                }
            }
            _isLoading.value = false
        }
    }

    fun updateItemQuantity(itemId: Int, newQuantity: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val updateRequest = UpdateCartItemRequest(
                    userId = _userId,
                    productId = itemId,
                    quantity = newQuantity
                )
                val response = carritoApiService.updateCartItem(itemId, updateRequest) // Pass itemId and updateRequest

                if (response.isSuccessful) {
                    fetchCart() // Refresh the cart
                } else {
                    _errorMessage.value = "Error al actualizar cantidad: ${response.message()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun removeItem(itemId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = carritoApiService.removeFromCart(itemId)

                if (response.isSuccessful) {
                    fetchCart() // Refresh the cart
                } else {
                    _errorMessage.value = "Error al eliminar el ítem: ${response.message()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = carritoApiService.clearCart()
                if (response.isSuccessful) {
                    _cartState.value = createEmptyCart()
                } else {
                    _errorMessage.value = "Error al vaciar carrito: ${response.message()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    private fun createEmptyCart(): CarritoDto {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return CarritoDto(
            id = 0,
            usuarioId = _userId,
            fecha = dateFormat.format(Date()),
            activo = true,
            total = 0.0,
            items = emptyList(),
            customItems = emptyList()
        )
    }
}

class CarritoViewModelFactory(
    private val carritoApiService: CarritoApiService,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CarritoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CarritoViewModel(carritoApiService, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
//Comment