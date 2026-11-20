package com.example.ysukampus.data.session

interface DeviceIdStorage {
    fun getOrCreateDeviceId(): String
}