package com.basistheory.threeds.service

import android.content.Context
import android.util.Log
import com.ravelin.core.configparameters.ConfigParametersBuilder
import com.ravelin.threeds2service.instantiation.ThreeDS2ServiceInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers


interface Service {
    /**
     * - `application: Context` The application context.
     * - `locale: String?` Provides information about the default Locality
     * - `coroutineScope: CoroutineScope?` Scope of the coroutine calling this suspend function
     */
    suspend fun initialize(
        applicationContext: Context,
        locale: String?,
        scope: CoroutineScope?
    )
}

class ThreeDsService  : Service {
    private val sdk = ThreeDS2ServiceInstance.get();

    override suspend fun initialize(
        applicationContext: Context,
        locale: String?,
        scope: CoroutineScope?
    ) {
        val configParameters = ConfigParametersBuilder.Builder()
            .setEnvironment("basistheory-sandbox") // TODO: get this from ravelin
            .setApiToken("ApiToken")
            .build()

        runCatching {
            sdk.initialize(
                applicationContext,
                configParameters,
                locale ?: applicationContext.resources.configuration.locale.toString(),
                null, // UI customization not supported for V1
                null, // UI customization not supported for V1
                scope ?: CoroutineScope(context = Dispatchers.IO)
            )
        }.onSuccess {

            checkWarnings()


            Log.i("threedsservice", "3ds service initialized correctly")

            return
        }.onFailure {
            throw Error(it.localizedMessage)
        }
    }

    private fun checkWarnings() {
        val warnings = sdk.getWarnings();
        /**
         * https://developer.ravelin.com/psp/libraries-and-sdks/android/3ds-sdk/android/#warnings
         * SW01	The device is jailbroken.	HIGH
         * SW02	The integrity of the SDK has been tampered.	HIGH
         * SW03	An emulator is being used to run the App.	HIGH
         * SM04	A debugger is attached to the App.	MEDIUM
         * SW05	The OS or the OS version is not supported.	HIGH
         */
        val warningCodes = setOf("SW01", "SW02", "SW03", "SW04", "SW05");

        warnings?.forEach {
            if (it?.getID() in warningCodes) {
                throw error(it?.getMessage().toString());
            }
        }
    }
}