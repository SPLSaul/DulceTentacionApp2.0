package com.example.ddma.data.model

sealed class CarritoState {
    object Idle : CarritoState()
    object Loading : CarritoState()
    data class Success(val item: CarritoItemResponse) : CarritoState()
    data class Error(val message: String) : CarritoState()
}