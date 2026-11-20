package com.example.ysukampus.data.session

import android.content.Context
import com.example.ysukampus.data.local.EncryptedPrefsTokenStorage

object SessionManagerProvider {
    @Volatile
    private var sessionManager: SessionManager? = null

    fun get(context: Context): SessionManager =
        sessionManager ?: synchronized(this) {
            sessionManager ?: SessionManager(
                tokenStorage = EncryptedPrefsTokenStorage(context.applicationContext)
            ).also { sessionManager = it }
        }
}
