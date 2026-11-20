package com.example.ysukampus.ui.main.update

import android.content.Context
import android.util.Log
import com.example.ysukampus.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appdistribution.FirebaseAppDistribution

class BetaUpdateManager(
    context: Context,
    private val appDistributionProvider: () -> FirebaseAppDistribution? = {
        if (BuildConfig.BUILD_TYPE != "beta") {
            null
        } else {
            val appContext = context.applicationContext
            val firebaseApp = FirebaseApp.getApps(appContext).firstOrNull()
                ?: FirebaseApp.initializeApp(appContext)
            if (firebaseApp != null) FirebaseAppDistribution.getInstance() else null
        }
    }
) {
    fun isSupportedBuild(): Boolean = BuildConfig.BUILD_TYPE == "beta"

    fun checkForUpdate(
        onUpdateAvailable: (displayVersion: String?, buildVersion: String?, releaseNotes: String?) -> Unit,
        onNoUpdate: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        if (!isSupportedBuild()) {
            Log.d(TAG, "Skipping Firebase App Distribution update check for non-beta build")
            onNoUpdate()
            return
        }

        try {
            val appDistribution = appDistributionProvider()
            if (appDistribution == null) {
                Log.w(TAG, "Firebase App Distribution instance is unavailable for beta build")
                onNoUpdate()
                return
            }

            if (appDistribution.isTesterSignedIn()) {
                fetchRelease(
                    appDistribution = appDistribution,
                    onUpdateAvailable = onUpdateAvailable,
                    onNoUpdate = onNoUpdate,
                    onError = onError
                )
                return
            }

            appDistribution.signInTester()
                .addOnSuccessListener {
                    fetchRelease(
                        appDistribution = appDistribution,
                        onUpdateAvailable = onUpdateAvailable,
                        onNoUpdate = onNoUpdate,
                        onError = onError
                    )
                }
                .addOnFailureListener { throwable ->
                    Log.e(TAG, "Tester sign-in failed", throwable)
                    onError(throwable)
                }
        } catch (throwable: Throwable) {
            Log.e(TAG, "Update check crashed", throwable)
            onError(throwable)
        }
    }

    fun startUpdate(
        onStarted: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        if (!isSupportedBuild()) {
            Log.d(TAG, "Skipping Firebase App Distribution update launch for non-beta build")
            return
        }

        try {
            val appDistribution = appDistributionProvider()
            if (appDistribution == null) {
                onError(IllegalStateException("Firebase App Distribution is unavailable for this build"))
                return
            }

            appDistribution.updateApp()
                .addOnSuccessListener {
                    onStarted()
                }
                .addOnFailureListener { throwable ->
                    Log.e(TAG, "Update start failed", throwable)
                    onError(throwable)
                }
        } catch (throwable: Throwable) {
            Log.e(TAG, "Update flow crashed", throwable)
            onError(throwable)
        }
    }

    private fun fetchRelease(
        appDistribution: FirebaseAppDistribution,
        onUpdateAvailable: (displayVersion: String?, buildVersion: String?, releaseNotes: String?) -> Unit,
        onNoUpdate: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        appDistribution.checkForNewRelease()
            .addOnSuccessListener { release ->
                if (release == null) {
                    onNoUpdate()
                    return@addOnSuccessListener
                }

                onUpdateAvailable(
                    release.getDisplayVersion(),
                    release.getVersionCode().toString(),
                    release.getReleaseNotes()
                )
            }
            .addOnFailureListener { throwable ->
                Log.e(TAG, "Release check failed", throwable)
                onError(throwable)
            }
    }

    private companion object {
        const val TAG = "BetaUpdate"
    }
}
