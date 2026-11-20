package com.example.ysukampus.ui.main.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ysukampus.data.auth.ProfileRepository
import com.example.ysukampus.data.network.ApiEnvironmentHolder
import com.example.ysukampus.data.network.dto.UserProfileDto
import com.example.ysukampus.data.network.dto.UserUpdateRequestDto
import com.example.ysukampus.data.schedule.ScheduleRepository
import com.example.ysukampus.data.session.CurrentUser
import com.example.ysukampus.data.session.CurrentUserStore
import com.example.ysukampus.data.session.TokenStorage
import com.example.ysukampus.data.session.currentUserId
import com.example.ysukampus.ui.main.schedule.ScheduleEntity
import com.example.ysukampus.ui.main.schedule.ScheduleEntityType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URI

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val scheduleRepository: ScheduleRepository,
    private val currentUserStore: CurrentUserStore,
    private val tokenStorage: TokenStorage
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    /**
     * Карточка пользователя, от которой пляшет весь экран.
     *
     * Живёт отдельно от [ProfileUiState]: там UI-представление, а здесь — то,
     * что уходит на бэк и в кэш (id, роль, oid сущности).
     */
    private var currentUser: CurrentUser? = null

    /**
     * id владельца сессии.
     *
     * Обычно приезжает вместе с карточкой, но у сессии, пережившей обновление
     * приложения, кэш старого формата — тогда достаём id из access-токена,
     * иначе профиль остался бы нередактируемым до повторного входа.
     */
    private var userId: Long? = null

    init {
        val cached = currentUserStore.get()
        cached?.let(::applyUser)
        userId = cached?.id ?: tokenStorage.currentUserId()
        // Редактировать можно, когда известны и id, и сама карточка; без кэша
        // кнопка включится, как только карточку принесёт refreshUser().
        _uiState.update { it.copy(canEdit = userId != null && currentUser != null) }
        refreshProfilePhoto()
        refreshUser()
    }

    // --- Аватар ---

    fun onAvatarClick() {
        _uiState.update { state ->
            if (state.isAvatarBusy) state else state.copy(avatarMenuVisible = true)
        }
    }

    fun onDismissAvatarMenu() {
        _uiState.update { it.copy(avatarMenuVisible = false) }
    }

    fun onUploadAvatarClick() {
        _uiState.update { it.copy(avatarMenuVisible = false) }
    }

    fun onAvatarSelected(uri: Uri) {
        val userId = userId ?: run {
            showMessage(SIGN_IN_AGAIN_MESSAGE)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(avatarMenuVisible = false, isAvatarUploading = true) }

            profileRepository.uploadProfilePhoto(userId, uri)
                .onSuccess { photo ->
                    currentUserStore.updateAvatarUrl(photo.avatarUrl)
                    currentUser = currentUser?.copy(avatarUrl = photo.avatarUrl)
                    _uiState.update {
                        it.copy(
                            avatarUrl = photo.avatarUrl,
                            avatarImageUrl = resolveAvatarImageUrl(photo.avatarUrl),
                            isAvatarUploading = false,
                            message = "Фото профиля обновлено"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isAvatarUploading = false,
                            message = error.message ?: "Не удалось загрузить фото"
                        )
                    }
                }
        }
    }

    fun onDeleteAvatarClick() {
        val userId = userId ?: run {
            showMessage(SIGN_IN_AGAIN_MESSAGE)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(avatarMenuVisible = false, isAvatarDeleting = true) }

            profileRepository.deleteProfilePhoto(userId)
                .onSuccess {
                    currentUserStore.updateAvatarUrl(null)
                    currentUser = currentUser?.copy(avatarUrl = null)
                    _uiState.update {
                        it.copy(
                            avatarUrl = null,
                            avatarImageUrl = null,
                            isAvatarDeleting = false,
                            message = "Фото профиля удалено"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isAvatarDeleting = false,
                            message = error.message ?: "Не удалось удалить фото"
                        )
                    }
                }
        }
    }

    // --- Шторка редактирования ---

    fun onEditProfileClick() {
        val user = currentUser ?: run {
            showMessage(SIGN_IN_AGAIN_MESSAGE)
            return
        }

        val draft = ProfileDraft(
            forumName = user.forumName,
            forumColor = ForumColors.safe(user.forumColor),
            entityOid = user.entityOid.orEmpty(),
            entityName = user.entityName.ifBlank { NO_ENTITY_NAME },
            entityType = if (user.isLecturer) ScheduleEntityType.Lecturer else ScheduleEntityType.Group,
            telegramNotificationsEnabled = user.telegramNotificationsEnabled
        )

        _uiState.update {
            it.copy(
                edit = EditProfileState(
                    saved = draft,
                    draft = draft,
                    pickerMode = draft.entityType
                )
            )
        }
    }

    fun onCloseEditProfile() {
        // Пока идёт сохранение или загрузка фото, шторку не закрываем: результат
        // придёт в ту же форму.
        val state = _uiState.value
        if (state.edit?.isSaving == true || state.isAvatarBusy) return

        _uiState.update { it.copy(edit = null) }
    }

    /** Ник ограничен теми же символами, что и на бэке: латиница, цифры и `_`. */
    fun onForumNameChange(value: String) {
        val trimmed = value.take(EditProfileState.FORUM_NAME_MAX_LENGTH)
        if (!FORUM_NAME_ALLOWED.matches(trimmed)) return

        updateDraft { it.copy(forumName = trimmed) }
    }

    fun onForumColorSelected(color: String) {
        updateDraft { it.copy(forumColor = ForumColors.safe(color)) }
    }

    fun onTelegramNotificationsToggle() {
        updateDraft { it.copy(telegramNotificationsEnabled = !it.telegramNotificationsEnabled) }
    }

    fun onOpenEntityPicker() {
        val edit = _uiState.value.edit ?: return
        if (edit.isSaving) return

        _uiState.update {
            it.copy(
                edit = edit.copy(
                    page = EditProfilePage.Picker,
                    pickerMode = edit.draft.entityType,
                    pickerQuery = ""
                )
            )
        }
        loadDirectory()
    }

    fun onCloseEntityPicker() {
        updateEdit { it.copy(page = EditProfilePage.Form) }
    }

    fun onPickerQueryChange(query: String) {
        updateEdit { it.copy(pickerQuery = query) }
    }

    fun onPickerModeChange(mode: ScheduleEntityType) {
        updateEdit { it.copy(pickerMode = mode) }
    }

    fun onEntitySelected(entity: ScheduleEntity) {
        val edit = _uiState.value.edit ?: return
        if (edit.isSaving) return

        _uiState.update {
            it.copy(
                edit = edit.copy(
                    page = EditProfilePage.Form,
                    draft = edit.draft.copy(
                        entityOid = entity.oid,
                        entityName = entity.name,
                        entityType = entity.type
                    )
                )
            )
        }
    }

    fun onSaveProfileClick() {
        val userId = userId ?: return
        val edit = _uiState.value.edit ?: return
        if (!edit.isSaveEnabled) return

        val draft = edit.draft
        val saved = edit.saved
        val isEntityChanged = draft.entityOid != saved.entityOid || draft.entityType != saved.entityType
        // Сущность уезжает только целиком: бэк сверяет role и entity_id между
        // собой (студент — группа, преподаватель — преподаватель), а нечисловой
        // oid отправлять бессмысленно — он не пройдёт валидацию.
        val entityId = draft.entityOid.toIntOrNull().takeIf { isEntityChanged }
        val body = UserUpdateRequestDto(
            forumName = draft.forumName.takeIf { it != saved.forumName },
            forumColor = draft.forumColor.takeIf { it != saved.forumColor },
            role = if (entityId != null) draft.entityType.toApiRole() else null,
            entityId = entityId
        )
        val hasProfileChanges = body.forumName != null ||
            body.forumColor != null ||
            body.entityId != null
        // Тумблер Telegram-уведомлений уезжает своей ручкой: карточка профиля
        // о нём не знает, а пустой `PUT ref/user/{id}` бэк отбивает 400.
        val isTelegramChanged =
            draft.telegramNotificationsEnabled != saved.telegramNotificationsEnabled

        viewModelScope.launch {
            updateEdit { it.copy(isSaving = true) }

            if (isTelegramChanged) {
                val result = profileRepository
                    .updateTelegramNotifications(draft.telegramNotificationsEnabled)
                val error = result.exceptionOrNull()
                if (error != null) {
                    resetEditAfterFailure(error.message ?: "Не удалось сохранить профиль")
                    return@launch
                }
                currentUserStore.updateTelegramNotifications(draft.telegramNotificationsEnabled)
                currentUser = currentUser?.copy(
                    telegramNotificationsEnabled = draft.telegramNotificationsEnabled
                )
            }

            if (!hasProfileChanges) {
                // Менялся только тумблер — форма уже актуальна, перечитывать нечего.
                _uiState.update { state ->
                    val edit = state.edit
                    state.copy(
                        edit = edit?.copy(saved = draft, isSaving = false),
                        message = "Профиль сохранён"
                    )
                }
                return@launch
            }

            profileRepository.updateUser(userId, body)
                .onSuccess { response ->
                    applyUpdatedUser(response, draft.telegramNotificationsEnabled)
                    _uiState.update { state ->
                        state.copy(
                            edit = state.edit?.copy(isSaving = false),
                            message = "Профиль сохранён"
                        )
                    }
                }
                .onFailure { error ->
                    resetEditAfterFailure(error.message ?: "Не удалось сохранить профиль")
                }
        }
    }

    /**
     * Возвращает форму к сохранённым значениям — как в вебке: иначе на экране
     * останется ник, которого на сервере нет.
     */
    private fun resetEditAfterFailure(message: String) {
        _uiState.update { state ->
            val current = state.edit
            state.copy(
                edit = current?.copy(
                    draft = current.saved,
                    page = EditProfilePage.Form,
                    isSaving = false
                ),
                message = message
            )
        }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    // --- Загрузка данных ---

    private fun loadDirectory() {
        val edit = _uiState.value.edit ?: return
        if (edit.directory != null || edit.isDirectoryLoading) return

        viewModelScope.launch {
            updateEdit { it.copy(isDirectoryLoading = true) }

            scheduleRepository.getDirectory()
                .onSuccess { directory ->
                    updateEdit { it.copy(directory = directory, isDirectoryLoading = false) }
                }
                .onFailure { error ->
                    updateEdit { it.copy(isDirectoryLoading = false) }
                    showMessage(error.message ?: "Не удалось загрузить список")
                }
        }
    }

    private fun refreshUser() {
        val userId = userId ?: return

        viewModelScope.launch {
            profileRepository.getUser(userId)
                .onSuccess { response ->
                    val telegramEnabled = currentUser?.telegramNotificationsEnabled ?: true
                    applyUpdatedUser(response, telegramEnabled)
                }
                .onFailure { error ->
                    // При живом кэше молчим: экран уже показывает данные, а
                    // фоновое обновление — не действие пользователя. Без кэша
                    // сказать надо: иначе профиль просто останется пустым.
                    if (currentUser == null) {
                        showMessage(error.message ?: "Не удалось загрузить профиль")
                    }
                }
        }
    }

    private fun refreshProfilePhoto() {
        viewModelScope.launch {
            profileRepository.getProfilePhoto()
                .onSuccess { photo ->
                    currentUserStore.updateAvatarUrl(photo?.avatarUrl)
                    currentUser = currentUser?.copy(avatarUrl = photo?.avatarUrl)
                    _uiState.update {
                        it.copy(
                            avatarUrl = photo?.avatarUrl,
                            avatarImageUrl = resolveAvatarImageUrl(photo?.avatarUrl)
                        )
                    }
                }
                .onFailure { error ->
                    // Как и в refreshUser: при живом кэше молчим. Аватар уже на
                    // экране из CurrentUserStore, и снекбар про неудачное фоновое
                    // обновление — единственное, что пользователь заметил бы
                    // офлайн на этом экране.
                    if (currentUser?.avatarUrl == null) {
                        showMessage(error.message ?: "Не удалось получить фото профиля")
                    }
                }
        }
    }

    /**
     * Раскладывает свежую карточку по кэшу, [currentUser] и состоянию экрана.
     * Открытая шторка при этом получает новые «сохранённые» значения.
     */
    private fun applyUpdatedUser(response: UserProfileDto, telegramNotificationsEnabled: Boolean) {
        currentUserStore.update(response, telegramNotificationsEnabled)
        val user = currentUserStore.get() ?: return
        applyUser(user)

        val draft = ProfileDraft(
            forumName = user.forumName,
            forumColor = ForumColors.safe(user.forumColor),
            entityOid = user.entityOid.orEmpty(),
            entityName = user.entityName.ifBlank { NO_ENTITY_NAME },
            entityType = if (user.isLecturer) ScheduleEntityType.Lecturer else ScheduleEntityType.Group,
            telegramNotificationsEnabled = user.telegramNotificationsEnabled
        )
        _uiState.update { state ->
            val edit = state.edit ?: return@update state
            state.copy(edit = edit.copy(saved = draft, draft = draft))
        }
    }

    private fun applyUser(user: CurrentUser) {
        currentUser = user
        userId = user.id ?: userId
        _uiState.update { state ->
            state.copy(
                name = user.forumName.ifBlank { state.name },
                entityName = user.entityName.ifBlank { NO_ENTITY_NAME },
                forumColor = ForumColors.safe(user.forumColor),
                canEdit = userId != null,
                // Фото приходит отдельной ручкой; ссылку из карточки берём
                // только пока своего ответа ещё нет.
                avatarUrl = state.avatarUrl ?: user.avatarUrl,
                avatarImageUrl = state.avatarImageUrl ?: resolveAvatarImageUrl(user.avatarUrl)
            )
        }
    }

    private fun updateDraft(transform: (ProfileDraft) -> ProfileDraft) {
        updateEdit { edit ->
            if (edit.isSaving) edit else edit.copy(draft = transform(edit.draft))
        }
    }

    private fun updateEdit(transform: (EditProfileState) -> EditProfileState) {
        _uiState.update { state ->
            val edit = state.edit ?: return@update state
            state.copy(edit = transform(edit))
        }
    }

    private fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    private fun ScheduleEntityType.toApiRole(): String = when (this) {
        ScheduleEntityType.Lecturer -> CurrentUser.ROLE_TEACHER
        ScheduleEntityType.Group -> CurrentUser.ROLE_STUDENT
    }

    private fun resolveAvatarImageUrl(avatarUrl: String?): String? {
        if (avatarUrl.isNullOrBlank()) return null
        if (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://")) {
            return avatarUrl
        }

        val baseUri = runCatching { URI(ApiEnvironmentHolder.baseUrl) }.getOrNull() ?: return avatarUrl
        val path = if (avatarUrl.startsWith("/")) avatarUrl else "/$avatarUrl"
        return "${baseUri.scheme}://${baseUri.authority}$path"
    }

    private companion object {
        val FORUM_NAME_ALLOWED = Regex("^[A-Za-z0-9_]*$")
        const val NO_ENTITY_NAME = "Не выбрано"
        const val SIGN_IN_AGAIN_MESSAGE = "Профиль недоступен: войдите в приложение заново"
    }
}
