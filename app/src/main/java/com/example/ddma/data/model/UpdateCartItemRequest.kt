package com.example.ddma.data.model

data class UpdateCartItemRequest(
    val userId: Int,
    val productId: Int,
    val quantity: Int
)