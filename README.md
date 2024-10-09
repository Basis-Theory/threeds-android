# Basis Theory 3DS Android SDK

## Installation

Add the following to `dependencyResolutionManagement` in `settings.gradle`
```groovy
dependencyResolutionManagement {
    repositories {
        //...other repositories
        maven {
          setUrl("https://maven.pkg.github.com/basis-theory/3ds-android")
          credentials {
            username = // GH username
            password = // Personal Access Token (Classic) https://docs.github.com/en/packages/learn-github-packages/introduction-to-github-packages#authenticating-to-github-packages
            }
        }
        maven {
          setUrl("https://maven.ravelin.com/public/repositories/threeds2service/")
        }
    }
}
```

Add dependencies to your app's `build.gradle`
```groovy
dependencies {
   //... other deps
   implementation "com.basistheory:3ds-android:1.0.0-beta.0"
   implementation ("com.ravelin.threeds2service:threeds2service-sdk:1.4.2")
}
```

## Usage

**⚠️ Code for illustration purposes ⚠️**

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplication2Theme {
                initializeThreeDsService(this.applicationContext, this)
            }
        }
    }
}

@Composable
fun initializeThreeDsService(context: Context, activity: Activity) {
    val threeDsService = ThreeDsService.Builder()
        .withApiKey("<PUBLIC_API_KEY>")
        .withAuthenticationEndpoint("Your 3DS authentication endpoint") // https://developers.basistheory.com/docs/guides/process/authenticate-with-3ds#authenticating-a-session
        .withApplicationContext(context)
        .apply {
            // make sure withSandbox is removed in production environments
            if (BuildConfig.DEBUG) {
                withSandbox()
            }
        }
        .build()

    LaunchedEffect(Unit) {
        try {
            val warnings = threeDsService.initialize()

            if (!warnings.isNullOrEmpty()) {
                val session = threeDsService.createSession("Card Token ID")
                session?.let {
                    threeDsService.startChallenge(
                        it.id,
                        activity,
                        ::onCompletion, // success handler
                        ::onCompletion // failure handler
                    )
                } ?: Log.e("3DS_service", "Failed to create session.")
            }
        } catch (e: Exception) {
            Log.e("3DS_service", "Error initializing 3DS service: ${e.message}")
        }
    }
}

private fun onCompletion(result: ChallengeResponse) {
    Log.i(
        "3DS_service",
        "3DS status: ${result.status}, Details: ${result.details}, Session ID: ${result.id}"
    )
}
```

