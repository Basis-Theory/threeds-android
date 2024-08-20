package com.basistheory.threeds.example.viewmodel

import androidx.lifecycle.ViewModel
import com.basistheory.threeds.service.ThreeDsService

open class ThreeDsViewModel(private val threeDsService: ThreeDsService): ViewModel() {
    suspend fun initialize() {
        runCatching {
            threeDsService.initialize()
        }.onFailure {
            throw it
        }
    }
}