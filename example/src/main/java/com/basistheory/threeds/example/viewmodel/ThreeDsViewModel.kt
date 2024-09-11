package com.basistheory.threeds.example.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import com.basistheory.threeds.model.ChallengeResponse
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

    private val _warnings = MutableLiveData<List<String?>?>(null)
    val warnings: LiveData<List<String?>?>
        get() = _warnings

    val tokenId = MutableLiveData<String?>(null)

    val session = MutableLiveData<CreateThreeDsSessionResponse?>(null)


    val challengeResponse = MutableLiveData<ChallengeResponse?>(null)

    private val json = Json {
        prettyPrint = true
        prettyPrintIndent = "   "
        ignoreUnknownKeys = true
    }

    private val threeDsService = ThreeDsService
        .Builder()
        .withApiKey("< API_KEY >")
        .withAuthenticationEndpoint("< AUTH ENDPOINT >")
        .withApplicationContext(application.applicationContext)
        .build()

    fun initialize(): LiveData<Any> = liveData {
        _errorMessage.value = null
        _result.value = null

        try {
            _warnings.value = threeDsService.initialize()
        } catch (e: Exception) {
            _errorMessage.value = e.localizedMessage
        }
    }

    fun createSession(): LiveData<Any> = liveData {
        _errorMessage.value = null
        _result.value = null

        val tokenId = requireNotNull(tokenId.value)

        try {
            session.value = threeDsService.createSession(tokenId)
        } catch (e: Exception) {
            _errorMessage.value = e.localizedMessage
        }
    }

    private fun onChallengeCompleted(result: ChallengeResponse) {
        challengeResponse.postValue(result)
    }

    private fun onChallengeFailed(result: ChallengeResponse) {
        _errorMessage.postValue(json.encodeToString(result))
    }

    fun startChallenge(sessionId: String, activity: Activity): LiveData<Any> = liveData {
        _errorMessage.value = null
        _result.value = null

        try {
            threeDsService.startChallenge(
                sessionId,
                activity,
                ::onChallengeCompleted,
                ::onChallengeFailed
            )
        } catch (e: Exception) {
            throw e
        }
    }
}
