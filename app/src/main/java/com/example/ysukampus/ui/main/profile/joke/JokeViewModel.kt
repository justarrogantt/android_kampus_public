package com.example.ysukampus.ui.main.profile.joke

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ysukampus.data.auth.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Шторка «Расскажи анекдот»: `GET profile/joke`, порт `Profile/BottomSheet/Joke` вебки. */
class JokeViewModel(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(JokeUiState())
    val uiState: StateFlow<JokeUiState> = _uiState.asStateFlow()

    init {
        loadJoke()
    }

    fun onAnotherClick() {
        loadJoke()
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    private fun loadJoke() {
        if (_uiState.value.isRequesting) return
        _uiState.update { it.copy(isRequesting = true) }

        viewModelScope.launch {
            profileRepository.getJoke(_uiState.value.seenIds)
                .onSuccess { joke ->
                    _uiState.update { state ->
                        // Бэк уже начал новый круг — старый список показанных не годится.
                        val seenIds = if (state.seenIds.contains(joke.id)) {
                            listOf(joke.id)
                        } else {
                            state.seenIds + joke.id
                        }
                        state.copy(
                            joke = joke,
                            seenIds = seenIds,
                            isLoading = false,
                            isRequesting = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRequesting = false,
                            message = error.message ?: DEFAULT_ERROR_MESSAGE
                        )
                    }
                }
        }
    }

    private companion object {
        const val DEFAULT_ERROR_MESSAGE = "Не удалось загрузить анекдот"
    }
}
