package com.basistheory.threeds.example.viewmodel

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import com.basistheory.threeds.example.BuildConfig
import com.basistheory.threeds.example.model.ChallengeResult
import com.basistheory.threeds.model.ChallengeResponse
import com.basistheory.threeds.model.CreateThreeDsSessionResponse
import com.basistheory.threeds.service.ThreeDSService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject


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
    val status = MutableLiveData<String?>(null)
    val statusReason = MutableLiveData<String?>(null)

    val challengeResponse = MutableLiveData<ChallengeResponse?>(null)

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        prettyPrint = true
        prettyPrintIndent = "   "
        ignoreUnknownKeys = true
    }

    fun clear() {
        _errorMessage.postValue(null)
        _result.postValue(null)
        challengeResponse.postValue(null)
        status.postValue(null)
        statusReason.postValue(null)
        tokenId.postValue(null)
        _warnings.postValue(null)
    }

    private val threeDsService = ThreeDSService
        .Builder()
        .withApiKey(BuildConfig.BT_API_KEY_PUB)
        .withAuthenticationEndpoint("http://10.0.2.2:3333/3ds/authenticate")
        .withSandbox()
        .withBaseUrl("api.flock-dev.com")
        .withApplicationContext(application.applicationContext)
        .build()

    fun initialize(): LiveData<Any> = liveData {
        _errorMessage.value = null
        _result.value = null

        try {
            _warnings.value = threeDsService.initialize()
        } catch (e: Throwable) {
            _errorMessage.value = e.localizedMessage
        }
    }

    fun createSession(): LiveData<Any> = liveData {
        _errorMessage.value = null
        _result.value = null

        val tokenId = requireNotNull(tokenId.value)

        try {
            session.value = threeDsService.createSession(tokenId)
        } catch (e: Throwable) {
            _errorMessage.value = e.message
        }
    }

    private fun onChallengeCompleted(result: ChallengeResponse) {
        challengeResponse.postValue(result)

        status.postValue(result.status)

        result.details?.let {
            statusReason.postValue(result.details)
        }
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
        } catch (e: Throwable) {
            _result.value = e.message
        }
    }


    fun getChallengeResult(): LiveData<Any> = liveData {
        var response: String? = null

        withContext(Dispatchers.IO) {
            runCatching {
                val sessionId = requireNotNull(session.value).id

                val client = OkHttpClient()
                val payload = JSONObject().apply {
                    put("sessionId", sessionId)
                }.toString()

                val req = Request.Builder()
                    .url("http://10.0.2.2:3333/3ds/get-result")
                    .post(payload.toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(req)
                    .execute().use {
                        val responseBody = requireNotNull(it.body?.string())
                        it.body?.close()
                        if (!it.isSuccessful) {
                            throw Error("Unable to authenticate, downstream service responded ${it.code} for session $sessionId, ${it.message}")
                        }
                        response = responseBody
                    }
            }.onSuccess {
                Log.i("3ds_service", "$response");
            }.onFailure {
                throw it
            }
        }

        val decodedResponse = json.decodeFromString<ChallengeResult>(requireNotNull(response));
        status.postValue(decodedResponse.authenticationStatus)
        statusReason.postValue(decodedResponse.authenticationStatusReason)
        _result.postValue(json.encodeToString(decodedResponse))
    }

}
