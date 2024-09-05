package com.basistheory.threeds.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import com.basistheory.threeds.model.CreateThreeDsSessionResponse
import com.basistheory.threeds.service.ThreeDsService
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

open class ThreeDsViewModel(application: Application) : AndroidViewModel(application) {
    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?>
        get() = _errorMessage

    private val _result = MutableLiveData<String?>(null)
    val result: LiveData<String?>
        get() = _result

    private val json = Json {
        prettyPrint = true
        prettyPrintIndent = "   "
    }

    private val threeDsService = ThreeDsService
        .Builder()
        .withApiKey("<YOUR API KEY>")
        .withSandbox()
        .withApplicationContext(application.applicationContext)
        .build()

    fun initialize(): LiveData<Any> = liveData {
        _errorMessage.value = null
        _result.value = null

        try {
            Log.i("3ds_service", "Initializing 3DS Service")

            threeDsService.initialize()
        } catch (e: Exception) {
            Log.i("3ds_service", "${e.localizedMessage ?: e}")

            _errorMessage.value = e.localizedMessage
        }
    }

    fun createSession(tokenId: String): LiveData<Any> = liveData {
        _errorMessage.value = null
        _result.value = null

        try {
            _result.value = threeDsService.createSession(tokenId)?.let {
                json.encodeToString<CreateThreeDsSessionResponse>(it)
            }
        } catch (e: Exception) {
            _errorMessage.value = e.localizedMessage
        }
    }
}
