package com.example.ddma.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ddma.data.repositories.PaymentRepository
import com.example.ddma.ui.viewmodel.CarritoViewModel

class CarritoViewModelFactory(
    private val paymentRepository: PaymentRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CarritoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CarritoViewModel(paymentRepository) as T
        }
        throw IllegalArgumentException("ViewModel class desconocida")
    }
}