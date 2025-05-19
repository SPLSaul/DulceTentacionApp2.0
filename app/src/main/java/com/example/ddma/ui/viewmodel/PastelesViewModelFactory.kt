package com.example.ddma.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ddma.data.api.CarritoService
import com.example.ddma.data.model.PastelesRepository

class PastelesViewModelFactory(
    private val pastelesRepository: PastelesRepository,
    private val carritoService: CarritoService,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PastelesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PastelesViewModel(pastelesRepository, carritoService, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}