package com.basistheory.threeds.service

import android.content.Context
import android.util.Log
import com.ravelin.core.configparameters.ConfigParametersBuilder
import com.ravelin.threeds2service.instantiation.ThreeDS2ServiceInstance
import com.ul.emvco3ds.sdk.spec.ConfigParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.Request

enum class Region {
    US,
    EU
}

class ThreeDsServiceBuilder {
    private val regionMap = mapOf(Region.EU to "EuLive", Region.US to "USLive")

    private var apiKey: String? = null;
    private var context: Context? = null;
    private var region: String = regionMap[Region.US]!!;
    private var locale: String? = null;
    private var scope: CoroutineScope = CoroutineScope(context = Dispatchers.IO)
    private var btEnv: String = "https://api.basistheory.com"

    fun withApiKey(apiKey: String): ThreeDsServiceBuilder {
        this.apiKey = apiKey;

        return this
    }

    fun withApplicationContext(value: Context): ThreeDsServiceBuilder {
        this.context = value;

        return this
    }

    fun withRegion(value: Region): ThreeDsServiceBuilder {
        region = regionMap[value]!!;

        return this
    }

    fun withCoroutineScope(value: CoroutineScope): ThreeDsServiceBuilder {
        scope = value;

        return this
    }

    /**
     * For internal testing purposes
     */
    fun withBtTestEnv(): ThreeDsServiceBuilder {
        btEnv = "https://api.flock-dev.com";

        return this
    }

    fun withLocale(_locale: String?): ThreeDsServiceBuilder {
        locale = _locale

        return this
    }

    fun build(): ThreeDsService {
        requireNotNull(apiKey) { "Missing Api Key" }
        requireNotNull(context) { "Missing Application Context" }

        val localeOrDefault = locale
            ?: "${context!!.resources.configuration.locale.language}-${context!!.resources.configuration.locale.country}"

        return ThreeDsService(
            apiKey = apiKey!!,
            context = context!!,
            region = region,
            scope = scope,
            btEnv = btEnv,
            locale = localeOrDefault
        )
    }

}

class ThreeDsService(
    private val apiKey: String,
    private val context: Context,
    private val region: String,
    private val scope: CoroutineScope,
    private val btEnv: String,
    private val locale: String
) {
    private val sdk = ThreeDS2ServiceInstance.get();
    private val client = OkHttpClient()

    companion object {
        fun Builder() = ThreeDsServiceBuilder()
    }

    suspend fun initialize() {
        runCatching {
            val req = Request.Builder()
                .url("$btEnv/3ds/key")
                .addHeader("BT-API-KEY", apiKey)
                .get()
                .build()

            val call = client.newCall(req)
            val response = call.execute()

            if (!response.isSuccessful) {
                throw Error("Unable to fetch credentials, downstream service responded ${response.code}")
            }

            val ravelinApiToken = response.body?.string()

            val configParameters: ConfigParameters = ConfigParametersBuilder.Builder()
                .setEnvironment(region)
                .setApiToken("Bearer $ravelinApiToken")
                .build()


            sdk.initialize(
                context,
                configParameters,
                locale,
                null, // UI customization not supported for V1
                null, // UI customization not supported for V1
                scope
            )
        }.onSuccess {
            // TODO: Should we bubble them up to the user or just throw here?
            checkWarnings()

            Log.i("3ds_service", "Service initialized correctly")

            return
        }.onFailure {
            Log.i("3ds_service", "Failed to initialize service")
            throw Error(it.localizedMessage)
        }
    }


    /**
     * https://developer.ravelin.com/psp/libraries-and-sdks/android/3ds-sdk/android/#warnings
     * SW01	The device is jailbroken.	HIGH
     * SW02	The integrity of the SDK has been tampered.	HIGH
     * SW03	An emulator is being used to run the App.	HIGH
     * SM04	A debugger is attached to the App.	MEDIUM
     * SW05	The OS or the OS version is not supported.	HIGH
     */
    private fun checkWarnings() = sdk.getWarnings()
        ?.filter { it?.getID() in setOf("SW01", "SW02", "SW03", "SW04", "SW05") }
        ?.forEach {
            Log.i(
                "3ds_service",
                it?.getMessage().toString()
            )
        }

}