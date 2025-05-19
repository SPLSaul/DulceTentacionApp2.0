package com.example.ddma.data.model

data class CarritoDto(
    val id: Int,
    val usuarioId: Int,
    val fecha: String,
    val activo: Boolean,
    val total: Double,
    val items: List<CarritoItemDto>,
    val customItems: List<CarritoCustomItemDto>
)

data class CarritoItemDto(
    val id: Int,
    val carritoId: Int,
    val pastelId: Int,
    val nombrePastel: String,
    val imagenPastel: String,
    val cantidad: Int,
    val precioUnitario: Double,
    val subtotal: Double
)

data class CarritoCustomItemDto(
    val id: Int,
    val carritoId: Int,
    val personalizadoId: Int,
    val nombrePersonalizado: String,
    val imagenPersonalizado: String,
    val cantidad: Int,
    val precioUnitario: Double,
    val subtotal: Double
)

