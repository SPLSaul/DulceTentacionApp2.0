package com.example.ddma.data.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class LoginViewModel : ViewModel() {
    private val _loginState = MutableStateFlow(LoginState())
    val loginState: StateFlow<LoginState> = _loginState

    fun login(usernameOrEmail: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState(isLoading = true)
            try {
                val response = RetrofitClient.instance.login(
                    LoginRequest(usernameOrEmail, password)
                )

                if (response.isSuccessful) {
                    response.body()?.let { loginResponse ->
                        if (loginResponse.id > 0) { // Validate we got a valid user ID
                            _loginState.value = LoginState(
                                isSuccess = true,
                                userData = loginResponse
                            )
                        } else {
                            _loginState.value = LoginState(
                                error = "Invalid user ID received"
                            )
                        }
                    } ?: run {
                        _loginState.value = LoginState(
                            error = "Empty response from server"
                        )
                    }
                } else {
                    _loginState.value = LoginState(
                        error = when (response.code()) {
                            401 -> "Invalid credentials"
                            404 -> "User not found"
                            else -> "Login failed (Error ${response.code()})"
                        }
                    )
                }
            } catch (e: IOException) {
                _loginState.value = LoginState(
                    error = "Network error: ${e.message}"
                )
            } catch (e: Exception) {
                _loginState.value = LoginState(
                    error = "Error: ${e.message}"
                )
            }
        }
    }
}