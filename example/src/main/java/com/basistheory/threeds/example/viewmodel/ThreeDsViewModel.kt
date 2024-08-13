package com.basistheory.threeds.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.basistheory.threeds.service.ThreeDsService

open class ThreeDsViewModel(private val threeDsService: ThreeDsService): ViewModel() {
    suspend fun initialize(context: Context) {
        runCatching {
            threeDsService.initialize(context, null, null)
        }.onFailure {
            throw error("Failed to initialize service in view model: ${it.localizedMessage}")
        }
    }
}