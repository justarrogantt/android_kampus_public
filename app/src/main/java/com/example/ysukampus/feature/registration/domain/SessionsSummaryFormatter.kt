package com.example.ysukampus.feature.registration.domain

import com.example.ysukampus.data.network.dto.TokensDTO

class SessionsSummaryFormatter {
    fun format(
        sessions: List<TokensDTO>,
        isCurrentSession: (TokensDTO) -> Boolean
    ): String {
        if (sessions.isEmpty()) return "Активных сессий нет"

        return buildString {
            appendLine("Активных сессий: ${sessions.size}/5")
            sessions.forEachIndexed { index, session ->
                append("${index + 1}. id=${session.id}, created_at=${session.createdAt}, device_info=${session.deviceInfo}")
                if (isCurrentSession(session)) append(" [ТЕКУЩАЯ]")
                appendLine()
            }
        }.trim()
    }
}
