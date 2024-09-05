package com.basistheory.threeds.service

import android.content.Context
import android.util.Log
import com.basistheory.threeds.model.CreateThreeDsSessionResponse
import com.basistheory.threeds.model.RavelinKeys
import com.basistheory.threeds.model.ThreeDSDeviceInfo
import com.basistheory.threeds.model.ThreeDSMobileSdkRenderOptions
import com.basistheory.threeds.model.UpdateThreeDsSessionRequest
import com.ravelin.core.configparameters.ConfigParametersBuilder
import com.ravelin.threeds2service.instantiation.ThreeDS2ServiceInstance
import com.ul.emvco3ds.sdk.spec.AuthenticationRequestParameters
import com.ul.emvco3ds.sdk.spec.ConfigParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

enum class Region {
    US,
    EU
}

class ThreeDsServiceBuilder {
    /** Default to EU, according to Ravelin our account was setup to work in the EU
     * US is reserved for "big holdings" in the US
     */
    private val regionMap = mapOf(Region.EU to "EuLive", Region.US to "USLive")

    private var apiKey: String? = null
    private var context: Context? = null
    private var region: String = regionMap[Region.EU]!!
    private var locale: String? = null
    private var scope: CoroutineScope = CoroutineScope(context = Dispatchers.IO)
    private var sandbox: Boolean = false
    private var apiBaseUrl: String = "api.basistheory.com"

    fun withApiKey(apiKey: String) = apply {
        this.apiKey = apiKey
    }

    fun withApplicationContext(value: Context) = apply {
        this.context = value
    }

    fun withCoroutineScope(value: CoroutineScope) = apply {
        this.scope = value

    }

    fun withLocale(_locale: String?) = apply { this.locale = _locale }


    fun withSandbox() = apply {
        this.sandbox = true
    }

    /**
     * Internal use only
     */
    fun withBaseUrl(apiBaseUrl: String) = apply {
        require(apiBaseUrl == "api.flock-dev.com") {
            Log.e(
                "3ds_service",
                "Invalid base url $apiBaseUrl"
            )
        }

        this.apiBaseUrl = apiBaseUrl
    }

    fun build(): ThreeDsService {
        requireNotNull(apiKey) { "Missing Api Key" }
        requireNotNull(context) { "Missing Application Context" }

        val localeOrDefault: String = context!!.let  {
                locale
                    ?: "${it.resources.configuration.locale.language}-${it.resources.configuration.locale.country}"
            }


        return ThreeDsService(
            apiKey = apiKey!!,
            context = context!!,
            region = region,
            scope = scope,
            locale = localeOrDefault,
            sandbox = sandbox,
            apiBaseUrl = apiBaseUrl
        )
    }

}

class ThreeDsService(
    private val apiKey: String,
    private val context: Context,
    private val region: String,
    private val scope: CoroutineScope,
    private val locale: String,
    private val sandbox: Boolean,
    private val apiBaseUrl: String
) {
    private val sdk = ThreeDS2ServiceInstance.get()
    private val client = OkHttpClient()

    companion object {
        fun Builder() = ThreeDsServiceBuilder()
    }

    suspend fun initialize(): List<String?>? {
        withContext(Dispatchers.IO) {
            runCatching {

                Log.i("3ds_service", "Getting Keys from CDN")

                val req = Request.Builder()
                    .url("https://cdn.basistheory.com/keys/3ds.json")
                    .get()
                    .build()

                val keysResponseBody = client.newCall(req)
                    .execute().use {
                        val responseBody = requireNotNull(it.body?.string())
                        it.body?.close()
                        if (!it.isSuccessful) {
                            throw Error("Unable to fetch credentials, downstream service responded ${it.code}")
                        }

                        responseBody.also { body ->
                            Log.i("3ds_service", "Successful response from downstream: $body")
                        }
                    }

                val ravelinApiKeys =
                    Json.decodeFromString<RavelinKeys>(keysResponseBody)

                val configParameters: ConfigParameters = ConfigParametersBuilder.Builder()
                    .setEnvironment(region)
                    .setApiToken("Bearer ${if (sandbox) ravelinApiKeys.test else ravelinApiKeys.live}")
                    .build()


                sdk.initialize(
                    context,
                    configParameters,
                    locale,
                    null, // UI customization not supported for V1
                    null, // UI customization not supported for V1
                    scope
                )
            }
        }.onSuccess {

            Log.i("3ds_service", "Service initialized correctly, running security checks")

            // TODO: Decide if we want to surface these to the customer or handle them ourselves
            /**  (Security Warnings)[https://developer.ravelin.com/merchant/libraries-and-sdks/android/3ds-sdk/android/#warnings]
             *  We leverage Ravelin SDKs to perform checks
             * SW01	The device is jailbroken.	HIGH
             * SW02	The integrity of the SDK has been tampered.	HIGH
             * SW03	An emulator is being used to run the App.	HIGH
             * SM04	A debugger is attached to the App.	MEDIUM
             * SW05	The OS or the OS version is not supported.	HIGH
             */
            return sdk.getWarnings()
                ?.filter { it?.getID() in setOf("SW01", "SW02", "SW03", "SW04", "SW05") }
                ?.mapNotNull { it?.getMessage().toString() }

        }.onFailure {
            Log.i("3ds_service", "Failed to initialize service ${it.message} | $it")
            throw Error(it)
        }

        return null
    }

    suspend fun createSession(tokenId: String): CreateThreeDsSessionResponse? {
        var session: CreateThreeDsSessionResponse?  = null

        withContext(Dispatchers.IO) {
            runCatching {

                val createSessionResponse = create3dsSession(tokenId)

                Log.i(
                    "3ds_service",
                    "Creating transaction for ${createSessionResponse.directoryServerId} and ${createSessionResponse.recommendedVersion}"
                )

                val transaction = sdk.createTransaction(
                    createSessionResponse.directoryServerId,
                    createSessionResponse.recommendedVersion
                )

                val authRequestParams = transaction.getAuthenticationRequestParameters()
                    ?: throw Exception("Unable to get authentication request parameters").also {
                        Log.i("3ds_service", it.localizedMessage)
                    }

              update3dsSession(createSessionResponse.id, authRequestParams)

            }.onSuccess {
                session = it
            }.onFailure {
                Log.i(
                    "3ds_service",
                    "Error response from downstream: ${it.localizedMessage}"
                )
            }


        }


        return session
    }

    private fun create3dsSession(tokenId: String): CreateThreeDsSessionResponse {
        val createSessionBody = JSONObject().apply {
            put("pan", tokenId)
            put("device", "app")
        }.toString()

        val createSessionRequest = Request.Builder()
            .url("https://${apiBaseUrl}/3ds/sessions")
            .addHeader("BT-API-KEY", apiKey)
            .post(createSessionBody.toRequestBody("application/json".toMediaType()))
            .build()

        val createSessionResponseBody = client.newCall(createSessionRequest).execute().use {
            val responseBody = requireNotNull(it.body?.string())
            it.body?.close()
            if (!it.isSuccessful) {
                throw Exception("Unable to create session: ${it.code} $responseBody")
            }

            responseBody.also { body ->
                Log.i("3ds_service", "Successful response from downstream: $body")
            }
        }

        return Json.decodeFromString<CreateThreeDsSessionResponse>(createSessionResponseBody)
    }


    private fun update3dsSession(
        sessionId: String,
        authRequestParams: AuthenticationRequestParameters
    ): CreateThreeDsSessionResponse {
        val updateSessionBody = Json.encodeToString(
            UpdateThreeDsSessionRequest(
                deviceInfo = ThreeDSDeviceInfo(
                    sdkEphemeralPublicKey = authRequestParams.getSDKEphemeralPublicKey(),
                    sdkReferenceNumber = authRequestParams.getSDKReferenceNumber(),
                    sdkApplicationId = authRequestParams.getSDKAppID(),
                    sdkMaxTimeout = "05", // 5 minutes seems to be default
                    sdkRenderOptions = ThreeDSMobileSdkRenderOptions(
                        sdkInterface = RenderOptions.Native.toRavelinCode(),
                        sdkUiType = listOf(
                            UiTypes.TextField.toRavelinCode(),
                            UiTypes.SingleSelectField.toRavelinCode(),
                            UiTypes.MultiSelectField.toRavelinCode(),
                            UiTypes.OOB.toRavelinCode()
                        )
                    ),
                    sdkTransactionId = authRequestParams.getSDKTransactionID(),
                    sdkEncryptionData = authRequestParams.getDeviceData()
                )
            )
        )

        val updateSessionRequest = Request.Builder()
            .url("https://${apiBaseUrl}/3ds/sessions/${sessionId}")
            .addHeader("BT-API-KEY", apiKey)
            .put(updateSessionBody.toRequestBody("application/json".toMediaType()))
            .build()

        val updateSessionResponseBody = client.newCall(updateSessionRequest).execute().use {
            val responseBody = requireNotNull(it.body?.string())
            it.body?.close()
            if (!it.isSuccessful) {
                throw Exception("Unable to update session: ${it.code} $responseBody")
            }
            responseBody.also { body ->
                Log.i("3ds_service", "Successful response from downstream: $body")
            }
        }

        val session = Json.decodeFromString<CreateThreeDsSessionResponse>(updateSessionResponseBody)

        return session
    }
}

enum class RenderOptions(val code: String) {
    Native("01")
}

fun RenderOptions.toRavelinCode(): String = code

enum class UiTypes(val code: String) {
    TextField("01"),
    SingleSelectField("02"),
    MultiSelectField("03"),
    OOB("04"),
}

fun UiTypes.toRavelinCode(): String = code