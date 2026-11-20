package com.example.ysukampus.ui.main.profile.joke

import com.example.ysukampus.data.auth.ProfileJoke

/** Снимок шторки «Расскажи анекдот». */
data class JokeUiState(
    /** Текущий анекдот. `null` — ещё едет первый запрос. */
    val joke: ProfileJoke? = null,
    /** Первая загрузка: вместо текста показываем скелетон. */
    val isLoading: Boolean = true,
    /** Идёт запрос — блокирует повторный тап «Другой». */
    val isRequesting: Boolean = false,
    /** Id уже показанных анекдотов — уходят в `seen`, чтобы бэк не повторялся. */
    val seenIds: List<Int> = emptyList(),
    val message: String? = null
)
