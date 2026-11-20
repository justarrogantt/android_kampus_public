package com.example.ysukampus.ui.main.profile

import android.net.Uri
import com.example.ysukampus.ui.main.schedule.ScheduleEntity
import com.example.ysukampus.ui.main.schedule.ScheduleEntityType

/**
 * Действия экрана профиля одним объектом.
 *
 * Каждое поле — отдельный параметр [ProfileScreen], а раньше и [com.example.ysukampus.ui.main.MainScreen];
 * после появления шторки редактирования их стало полтора десятка, и тащить
 * их через оболочку по одному бессмысленно. Значения по умолчанию оставляют
 * `@Preview` независимыми от ViewModel.
 */
data class ProfileActions(
    val onAvatarClick: () -> Unit = {},
    val onDismissAvatarMenu: () -> Unit = {},
    val onUploadAvatarClick: () -> Unit = {},
    val onAvatarSelected: (Uri) -> Unit = {},
    val onDeleteAvatarClick: () -> Unit = {},
    val onEditProfileClick: () -> Unit = {},
    val onCloseEditProfile: () -> Unit = {},
    val onForumNameChange: (String) -> Unit = {},
    val onForumColorSelected: (String) -> Unit = {},
    val onOpenEntityPicker: () -> Unit = {},
    val onCloseEntityPicker: () -> Unit = {},
    val onPickerQueryChange: (String) -> Unit = {},
    val onPickerModeChange: (ScheduleEntityType) -> Unit = {},
    val onEntitySelected: (ScheduleEntity) -> Unit = {},
    val onTelegramNotificationsToggle: () -> Unit = {},
    val onSaveProfileClick: () -> Unit = {},
    val onMessageShown: () -> Unit = {}
)

/** Связывает действия экрана с ViewModel — вызывается один раз на композицию. */
fun ProfileViewModel.asProfileActions(): ProfileActions = ProfileActions(
    onAvatarClick = ::onAvatarClick,
    onDismissAvatarMenu = ::onDismissAvatarMenu,
    onUploadAvatarClick = ::onUploadAvatarClick,
    onAvatarSelected = ::onAvatarSelected,
    onDeleteAvatarClick = ::onDeleteAvatarClick,
    onEditProfileClick = ::onEditProfileClick,
    onCloseEditProfile = ::onCloseEditProfile,
    onForumNameChange = ::onForumNameChange,
    onForumColorSelected = ::onForumColorSelected,
    onOpenEntityPicker = ::onOpenEntityPicker,
    onCloseEntityPicker = ::onCloseEntityPicker,
    onPickerQueryChange = ::onPickerQueryChange,
    onPickerModeChange = ::onPickerModeChange,
    onEntitySelected = ::onEntitySelected,
    onTelegramNotificationsToggle = ::onTelegramNotificationsToggle,
    onSaveProfileClick = ::onSaveProfileClick,
    onMessageShown = ::onMessageShown
)
