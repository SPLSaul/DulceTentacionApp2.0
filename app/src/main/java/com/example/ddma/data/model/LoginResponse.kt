package com.example.ddma.data.model

data class LoginResponse(
    val id: Int,
    val email: String,
    val username: String,
    val passwordHash: String?,
    val rol: String,
    val createdDT: String,
    val profilePicture: String?,
    val telefono: String?
)