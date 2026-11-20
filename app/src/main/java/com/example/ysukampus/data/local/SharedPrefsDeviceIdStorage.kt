package com.example.ysukampus.data.local

import android.content.SharedPreferences
import android.util.Log
import com.example.ysukampus.data.session.DeviceIdStorage
import java.util.UUID

class SharedPrefsDeviceIdStorage(
    private val prefs: SharedPreferences
) : DeviceIdStorage {
    override fun getOrCreateDeviceId(): String {
        val saved = prefs.getString(KEY_DEVICE_ID, null)

        return if (saved != null) {
            Log.d(TAG, "Using saved X-Device-Id: $saved")
            saved
        } else {
            val newId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
            Log.d(TAG, "Created new X-Device-Id: $newId")
            newId
        }
    }

    private companion object {
        const val TAG = "DeviceIdStorage"
        const val KEY_DEVICE_ID = "device_id"
    }
}
