package com.example.ddma.data.model

data class CarritoItemRequest(
    val userId: Int,
    val productId: Int,
    val quantity: Int = 1 // Default to 1 quantity
)