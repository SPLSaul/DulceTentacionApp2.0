package com.example.ddma.data.model

data class CarritoItemResponse(
    val id: Int,
    val carritoId: Int,
    val pastelId: Int,
    val nombrePastel: String,
    val imagenPastel: String,
    val cantidad: Int,
    val precioUnitario: Double,
    val subtotal: Double
)