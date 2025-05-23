package com.example.ddma.data.model.payment

import com.google.gson.annotations.SerializedName

/**
 * Modelo para solicitar la creación de un Payment Intent en Stripe
 *
 * @property userId ID del usuario que realiza el pago
 * @property amount Monto total en centavos (mínimo normalmente es 50 centavos)
 * @property currency Moneda (MXN para pesos mexicanos)
 * @property cartId ID del carrito asociado al pago
 * @property metadata Información adicional para tracking
 * @property description Descripción opcional del pago
 * @property customerId ID opcional del cliente en Stripe
 */
data class cdPaymentIntentRequest(
    val userId: Int,
    val amount: Long,
    val currency: String = "mxn",
    val cartId: Int,
    val metadata: Map<String, String> = emptyMap(),
    val description: String? = null,
    val customerId: String? = null
) {
    init {
        require(amount >= 50) { "El monto mínimo debe ser de 50 centavos" }
        require(currency.isNotEmpty()) { "La moneda no puede estar vacía" }
    }
}

/**
 * Respuesta con los datos del Payment Intent creado
 *
 * @property clientSecret Clave secreta para confirmar el pago en el cliente
 * @property paymentIntentId ID único del payment intent
 * @property amount Monto en centavos
 * @property currency Moneda usada
 * @property status Estado actual del payment intent
 * @property requiresAction Indica si se requiere acción adicional (como 3D Secure)
 */
data class PaymentIntentResponse(
    val clientSecret: String,
    val paymentIntentId: String,
    val amount: Long,
    val currency: String,
    val status: String = "requires_payment_method",
    val requiresAction: Boolean = false
)

/**
 * Solicitud para confirmar un pago
 *
 * @property paymentIntentId ID del payment intent a confirmar
 * @property paymentMethodId ID del método de pago a usar
 * @property userId ID del usuario que realiza el pago
 * @property cartId ID del carrito asociado
 * @property savePaymentMethod Si se debe guardar el método de pago para usos futuros
 */
data class ConfirmPaymentRequest(
    val paymentIntentId: String,
    val paymentMethodId: String,
    val userId: Int,
    val cartId: Int,
    val savePaymentMethod: Boolean = false
)

/**
 * Respuesta de confirmación de pago
 *
 * @property success Indica si el pago fue exitoso
 * @property paymentId ID único del pago
 * @property orderId ID opcional del pedido asociado
 * @property receiptUrl URL del recibo digital
 * @property requiresAction Indica si se requiere acción adicional
 * @property nextAction Tipo de acción requerida si aplica
 */
data class PaymentConfirmationResponse(
    val success: Boolean,
    val paymentId: String,
    val orderId: String? = null,
    val receiptUrl: String? = null,
    val requiresAction: Boolean = false,
    val nextAction: String? = null
)

/**
 * Información de un método de pago
 *
 * @property id ID único del método de pago
 * @property type Tipo (card, sepa_debit, etc.)
 * @property card Detalles de tarjeta (si aplica)
 * @property created Fecha de creación en timestamp
 * @property isDefault Si es el método de pago por defecto
 */
data class PaymentMethodResponse(
    val id: String,
    val type: String,
    val card: CardDetails?,
    @SerializedName("created")
    val createdAt: Long,
    val isDefault: Boolean = false
) {
    val displayName: String
        get() = when (type) {
            "card" -> "${card?.brand?.capitalize()} •••• ${card?.last4}"
            else -> type.replace("_", " ").capitalize()
        }
}

/**
 * Detalles de tarjeta de crédito/débito
 *
 * @property brand Marca de la tarjeta (visa, mastercard, etc.)
 * @property last4 Últimos 4 dígitos
 * @property expMonth Mes de expiración
 * @property expYear Año de expiración
 * @property country País de emisión
 * @property funding Tipo (credit, debit, prepaid)
 */
data class CardDetails(
    val brand: String,
    val last4: String,
    val expMonth: Int,
    val expYear: Int,
    val country: String? = null,
    val funding: String? = null
) {
    val expiration: String
        get() = "$expMonth/${expYear.toString().takeLast(2)}"
}

/**
 * Modelo para errores de pago
 *
 * @property error Tipo de error
 * @property message Mensaje descriptivo
 * @property code Código de error específico
 * @property declineCode Código de rechazo (si aplica)
 * @property paymentMethodId ID del método de pago relacionado
 */
data class PaymentErrorResponse(
    val error: String,
    val message: String? = null,
    val code: String? = null,
    val declineCode: String? = null,
    val paymentMethodId: String? = null
)

/**
 * Modelo para solicitar creación de un cliente en Stripe
 */
data class CreateCustomerRequest(
    val userId: Int,
    val name: String,
    val email: String,
    val phone: String? = null
)

/**
 * Respuesta con datos del cliente creado
 */
data class CustomerResponse(
    val id: String,
    val name: String,
    val email: String,
    val phone: String? = null
)