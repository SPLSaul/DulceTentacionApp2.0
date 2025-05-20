package com.example.ddma.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ddma.data.model.payment.PaymentIntentRequest
import com.example.ddma.data.model.payment.PaymentIntentResponse
import com.example.ddma.di.DependencyProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.lang.Exception

class PaymentViewModel : ViewModel() {
    private val paymentRepository = DependencyProvider.paymentRepository

    // Interface para comunicar eventos a la UI
    sealed class PaymentState {
        data class Success(val paymentIntent: PaymentIntentResponse) : PaymentState()
        data class Error(val message: String) : PaymentState()
        object Loading : PaymentState()
    }

    // StateFlow para observar el estado del pago
    private val _paymentState = MutableStateFlow<PaymentState?>(null)
    val paymentState: StateFlow<PaymentState?> = _paymentState.asStateFlow()

    fun processPayment(userId: Int, cartId: Int, amount: Double) {
        viewModelScope.launch {
            _paymentState.value = PaymentState.Loading

            try {
                val request = PaymentIntentRequest(
                    userId = userId,
                    amount = (amount * 100).toLong(), // Convertir a centavos
                    cartId = cartId,
                    currency = "MXN"
                )

                val response = paymentRepository.createPaymentIntent(request)

                if (response.isSuccess) {
                    response.getOrNull()?.let { paymentIntent ->
                        _paymentState.value = PaymentState.Success(paymentIntent)
                    } ?: run {
                        _paymentState.value = PaymentState.Error("Respuesta inválida del servidor")
                    }
                } else {
                    val errorMessage = response.exceptionOrNull()?.message ?: "Error desconocido"
                    _paymentState.value = PaymentState.Error(errorMessage)
                }
            } catch (e: Exception) {
                _paymentState.value = PaymentState.Error("Error de conexión: ${e.message ?: "Desconocido"}")
            }
        }
    }
}