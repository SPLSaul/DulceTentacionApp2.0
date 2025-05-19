package com.example.ddma.data.model

import android.adservices.ondevicepersonalization.UserData

data class LoginState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val userData: LoginResponse? = null
)