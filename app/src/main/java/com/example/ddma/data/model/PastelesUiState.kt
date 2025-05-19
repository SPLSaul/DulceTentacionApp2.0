package com.example.ddma.data.model

data class PastelesUiState(
    val pasteles: List<PastelesResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)