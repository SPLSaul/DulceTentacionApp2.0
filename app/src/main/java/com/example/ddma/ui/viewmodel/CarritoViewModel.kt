package com.example.ddma.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ddma.data.api.CarritoApiService
import com.example.ddma.data.model.CarritoDto
import com.example.ddma.data.model.UpdateCartItemRequest
import com.example.ddma.data.model.payment.ConfirmPaymentRequest
import com.example.ddma.data.model.payment.PaymentIntentRequest
import com.example.ddma.data.model.payment.PaymentMethodResponse
import com.example.ddma.data.repositories.PaymentRepository
import com.example.ddma.di.DependencyProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.lang.Exception

class CarritoViewModel(
    private val paymentRepository: PaymentRepository
) : ViewModel() {
    internal val _mostrarPantallaStripe = MutableStateFlow(false)
    val mostrarPantallaStripe: StateFlow<Boolean> = _mostrarPantallaStripe
    private val carritoApiService: CarritoApiService = DependencyProvider.carritoApiService

    private val _cartState = MutableStateFlow<CarritoDto?>(null)
    val cartState: StateFlow<CarritoDto?> = _cartState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _paymentMethods = MutableStateFlow<List<PaymentMethodResponse>>(emptyList())
    val paymentMethods: StateFlow<List<PaymentMethodResponse>> = _paymentMethods.asStateFlow()

    // Payment intent information
    private val _clientSecret = MutableStateFlow<String?>(null)
    val clientSecret: StateFlow<String?> = _clientSecret.asStateFlow()

    private val _paymentIntentId = MutableStateFlow<String?>(null)
    val paymentIntentId: StateFlow<String?> = _paymentIntentId.asStateFlow()

    private var userId: Int = 0

    fun setUserId(id: Int) {
        userId = id
    }
    fun setMostrarPantallaStripe(value: Boolean) {
        _mostrarPantallaStripe.value = value
    }
    fun fetchCartWithRetry() {
        if (userId <= 0) {
            _errorMessage.value = "Usuario no identificado"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = carritoApiService.getUserCart(userId)
                if (response.isSuccessful) {
                    _cartState.value = response.body()
                } else {
                    _errorMessage.value = "Error: ${response.message()}"
                }
            } catch (e: IOException) {
                _errorMessage.value = "Error de conexión: ${e.message}"
                Log.e("CarritoViewModel", "Error de conexión", e)
            } catch (e: HttpException) {
                _errorMessage.value = "Error HTTP: ${e.message}"
                Log.e("CarritoViewModel", "Error HTTP", e)
            } catch (e: Exception) {
                _errorMessage.value = "Error desconocido: ${e.message}"
                Log.e("CarritoViewModel", "Error desconocido", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateItemQuantity(itemId: Int, newQuantity: Int) {
        if (userId <= 0) return

        _isLoading.value = true
        viewModelScope.launch {
            try {
                // Get the pastelId for the cart item from the current state
                val pastelId = _cartState.value?.items?.find { it.id == itemId }?.pastelId
                    ?: run {
                        _errorMessage.value = "No se pudo encontrar el pastel correspondiente"
                        _isLoading.value = false
                        return@launch
                    }

                val request = UpdateCartItemRequest(
                    userId = userId,
                    productId = pastelId,
                    quantity = newQuantity
                )

                val response = carritoApiService.updateCartItem(itemId, request)
                if (response.isSuccessful) {
                    fetchCartWithRetry() // Reload cart after update
                } else {
                    _errorMessage.value = "Error al actualizar cantidad: ${response.message()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error al actualizar cantidad: ${e.message}"
                Log.e("CarritoViewModel", "Error al actualizar cantidad", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun removeItem(itemId: Int) {
        if (userId <= 0) return

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = carritoApiService.removeFromCart(itemId)
                if (response.isSuccessful) {
                    fetchCartWithRetry() // Reload cart after removal
                } else {
                    _errorMessage.value = "Error al eliminar ítem: ${response.message()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error al eliminar ítem: ${e.message}"
                Log.e("CarritoViewModel", "Error al eliminar ítem", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearCart() {
        if (userId <= 0) return

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = carritoApiService.clearCart()
                if (response.isSuccessful) {
                    // Create an empty cart with the same structure as the CarritoDto
                    _cartState.value = CarritoDto(
                        id = 0,
                        usuarioId = userId,
                        fecha = "",
                        activo = true,
                        total = 0.0,
                        items = emptyList(),
                        customItems = emptyList()
                    )
                } else {
                    _errorMessage.value = "Error al vaciar carrito: ${response.message()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error al vaciar carrito: ${e.message}"
                Log.e("CarritoViewModel", "Error al vaciar carrito", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Payment-related methods that use the PaymentRepository
    fun createPaymentIntent() {
        if (userId <= 0) {
            _errorMessage.value = "Usuario no identificado"
            return
        }

        val cart = _cartState.value ?: run {
            _errorMessage.value = "No hay carrito disponible"
            return
        }

        if (cart.total <= 0.0) {
            _errorMessage.value = "El total del carrito debe ser mayor a cero"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                // Convert total to cents for Stripe (minimum 50 cents)
                val amountInCents = (cart.total * 100).toLong().coerceAtLeast(50)

                val request = PaymentIntentRequest(
                    userId = userId,
                    amount = amountInCents,
                    cartId = cart.id,
                    description = "Compra de pasteles - Carrito #${cart.id}"
                )

                paymentRepository.createPaymentIntent(request)
                    .onSuccess { response ->
                        _clientSecret.value = response.clientSecret
                        _paymentIntentId.value = response.paymentIntentId
                        Log.d("CarritoViewModel", "Payment intent created: ${response.paymentIntentId}")
                    }
                    .onFailure { exception ->
                        _errorMessage.value = "Error al crear intento de pago: ${exception.message}"
                        Log.e("CarritoViewModel", "Error al crear intento de pago", exception)
                    }
            } catch (e: Exception) {
                _errorMessage.value = "Error al crear intento de pago: ${e.message}"
                Log.e("CarritoViewModel", "Error al crear intento de pago", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun confirmPayment(paymentMethodId: String) {
        if (userId <= 0) {
            _errorMessage.value = "Usuario no identificado"
            return
        }

        val cart = _cartState.value ?: run {
            _errorMessage.value = "No hay carrito disponible"
            return
        }

        val paymentIntentId = _paymentIntentId.value ?: run {
            _errorMessage.value = "No hay intento de pago activo"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val request = ConfirmPaymentRequest(
                    paymentIntentId = paymentIntentId,
                    paymentMethodId = paymentMethodId,
                    userId = userId,
                    cartId = cart.id,
                    savePaymentMethod = true // Save payment method for future use
                )

                paymentRepository.confirmPayment(request)
                    .onSuccess { response ->
                        if (response.success) {
                            // Handle successful payment confirmation
                            Log.d("CarritoViewModel", "Payment confirmed: ${response.paymentId}")
                            if (response.orderId != null) {
                                Log.d("CarritoViewModel", "Order created: ${response.orderId}")
                            }

                            // Clear payment intent info
                            _clientSecret.value = null
                            _paymentIntentId.value = null

                            // Clear cart after successful payment
                            clearCart()
                        } else if (response.requiresAction) {
                            // Handle cases where additional action is needed (3D Secure, etc.)
                            _errorMessage.value = "Se requiere una acción adicional: ${response.nextAction}"
                        } else {
                            _errorMessage.value = "La confirmación del pago falló"
                        }
                    }
                    .onFailure { exception ->
                        _errorMessage.value = "Error al confirmar pago: ${exception.message}"
                        Log.e("CarritoViewModel", "Error al confirmar pago", exception)
                    }
            } catch (e: Exception) {
                _errorMessage.value = "Error al confirmar pago: ${e.message}"
                Log.e("CarritoViewModel", "Error al confirmar pago", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchPaymentMethods() {
        if (userId <= 0) {
            _errorMessage.value = "Usuario no identificado"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                paymentRepository.getPaymentMethods(userId)
                    .onSuccess { methods ->
                        _paymentMethods.value = methods
                    }
                    .onFailure { exception ->
                        _errorMessage.value = "Error al obtener métodos de pago: ${exception.message}"
                        Log.e("CarritoViewModel", "Error al obtener métodos de pago", exception)
                    }
            } catch (e: Exception) {
                _errorMessage.value = "Error al obtener métodos de pago: ${e.message}"
                Log.e("CarritoViewModel", "Error al obtener métodos de pago", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun procesarPagoSimple() {
        if (userId <= 0) {
            _errorMessage.value = "Usuario no identificado"
            return
        }

        val cart = _cartState.value ?: run {
            _errorMessage.value = "No hay carrito disponible"
            return
        }

        if (cart.total <= 0.0) {
            _errorMessage.value = "El total del carrito debe ser mayor a cero"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                // Monto en centavos para Stripe (mínimo 50 centavos)
                val amountInCents = (cart.total * 100).toLong().coerceAtLeast(50)

                // Crear un PaymentIntent simple
                val request = PaymentIntentRequest(
                    userId = userId,
                    amount = amountInCents,
                    cartId = cart.id,
                    currency = "mxn",
                    description = "Compra de pasteles - Total: $${cart.total}"
                )

                // Llamar al backend para crear el PaymentIntent
                iniciarProcesoDePago(request)
            } catch (e: Exception) {
                _errorMessage.value = "Error al procesar pago: ${e.message}"
                Log.e("CarritoViewModel", "Error al procesar pago", e)
                _isLoading.value = false
            }
        }
    }

    private fun iniciarProcesoDePago(request: PaymentIntentRequest) {
        viewModelScope.launch {
            try {
                paymentRepository.createPaymentIntent(request)
                    .onSuccess { response ->
                        // Guardar clientSecret para usarlo con Stripe SDK
                        _clientSecret.value = response.clientSecret

                        // Mostrar la interfaz de pago de Stripe
                        _mostrarPantallaStripe.value = true
                        Log.d("CarritoViewModel", "Payment intent creado correctamente")
                    }
                    .onFailure { exception ->
                        _errorMessage.value = "Error al crear intento de pago: ${exception.message}"
                        Log.e("CarritoViewModel", "Error al crear intento de pago", exception)
                    }
            } catch (e: Exception) {
                _errorMessage.value = "Error al crear intento de pago: ${e.message}"
                Log.e("CarritoViewModel", "Error al crear intento de pago", e)
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}