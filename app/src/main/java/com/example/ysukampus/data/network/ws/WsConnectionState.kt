package com.example.ysukampus.data.network.ws

/** Состояние любого сокета приложения (модератор, персональный канал). */
enum class WsConnectionState {
    Disconnected,
    Connecting,
    Connected,
    Reconnecting,
    Failed
}
