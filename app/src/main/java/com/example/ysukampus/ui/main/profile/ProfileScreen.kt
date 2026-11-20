package com.example.ysukampus.ui.main.profile

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ysukampus.BuildConfig
import com.example.ysukampus.R
import com.example.ysukampus.crash.CrashReporting
import com.example.ysukampus.data.network.ApiEnvironmentHolder
import com.example.ysukampus.ui.main.KampusColors
import com.example.ysukampus.ui.main.notifications.components.NotificationsMenuBadge
import com.example.ysukampus.ui.main.profile.components.EditProfileSheet
import com.example.ysukampus.ui.main.profile.joke.JokeSheet
import com.example.ysukampus.ui.debug.ServerSwitchSheet
import com.example.ysukampus.ui.theme.ProfileTypography
import com.example.ysukampus.ui.theme.YsukampusTheme
import coil.compose.AsyncImage

@Composable
fun ProfileScreen(
    onOpenAdmin: () -> Unit,
    isUpdateAvailable: Boolean,
    isUpdateActionInProgress: Boolean,
    onUpdateClick: () -> Unit,
    /** «Открыть» — своя карточка форума с последней активностью. */
    onOpenMyProfile: () -> Unit = {},
    /** Пункт «Уведомления»: открывает шторку, живущую в оболочке. */
    onOpenNotifications: () -> Unit = {},
    /**
     * Бейдж на пункте уведомлений: непрочитанные числом, а очередь модерации —
     * пустым кружком (так же считает вебка).
     */
    showNotificationsBadge: Boolean = false,
    notificationsBadgeLabel: String = "",
    modifier: Modifier = Modifier,
    uiState: ProfileUiState = ProfileUiState(),
    actions: ProfileActions = ProfileActions()
) {
    val context = LocalContext.current
    // Локальное состояние, а не поле ProfileUiState: выбор сервера ничего не
    // знает о профиле и живёт ровно до перезапуска процесса.
    var serverSheetVisible by remember { mutableStateOf(false) }
    var testCrashDialogVisible by remember { mutableStateOf(false) }
    // Анекдот — та же схема: своя сеть, свой ViewModel, экрану профиля об этом знать незачем.
    var jokeSheetVisible by remember { mutableStateOf(false) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            actions.onAvatarSelected(uri)
        }
    }
    val pickPhoto = {
        actions.onUploadAvatarClick()
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        actions.onMessageShown()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KampusColors.AppBackground)
            .statusBarsPadding()
            .padding(horizontal = 14.dp)
            .padding(top = 1.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Профиль",
            color = KampusColors.TextMain,
            style = ProfileTypography.ScreenTitle
        )
        Spacer(Modifier.height(30.dp))
        ProfileAvatar(
            avatarImageUrl = uiState.avatarImageUrl,
            // Тот же кружок выбранного цвета, что и в шторке редактирования.
            accent = ForumColors.toComposeColor(uiState.forumColor),
            isBusy = uiState.isAvatarBusy,
            // Меню фото общее с шторкой редактирования, поэтому пока она открыта,
            // выпадашка рисуется там, а не под аватаром экрана.
            menuVisible = uiState.avatarMenuVisible && uiState.edit == null,
            hasAvatar = uiState.avatarUrl != null,
            onAvatarClick = actions.onAvatarClick,
            onDismissMenu = actions.onDismissAvatarMenu,
            onUploadClick = pickPhoto,
            onDeleteClick = actions.onDeleteAvatarClick
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = uiState.name,
            color = KampusColors.TextMain,
            style = ProfileTypography.UserName
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = uiState.entityName,
            color = KampusColors.PrimaryBlue,
            style = ProfileTypography.GroupName
        )
        Spacer(Modifier.height(14.dp))
        // Две пилюли, как в вебке: «Открыть» показывает свой профиль форума,
        // «Редактировать» открывает шторку правки карточки.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProfileActionButton(
                iconRes = R.drawable.ic_profile_avatar,
                title = stringResource(R.string.my_profile_open),
                onClick = onOpenMyProfile
            )
            ProfileActionButton(
                iconRes = R.drawable.ic_profile_edit,
                title = stringResource(R.string.profile_edit_short),
                enabled = uiState.canEdit,
                onClick = actions.onEditProfileClick
            )
        }
        Spacer(Modifier.height(18.dp))
        ProfileUpdateCard(
            isVisible = isUpdateAvailable,
            enabled = !isUpdateActionInProgress,
            onClick = onUpdateClick
        )
        ProfileMenuItem(
            iconRes = R.drawable.ic_notifications_outline,
            title = stringResource(R.string.notifications_title),
            contentDescription = stringResource(R.string.notifications_open),
            badgeLabel = notificationsBadgeLabel.takeIf { showNotificationsBadge },
            onClick = onOpenNotifications
        )
        ProfileMenuItem(
            iconRes = R.drawable.ic_profile_smile,
            title = stringResource(R.string.joke_menu_title),
            contentDescription = stringResource(R.string.joke_menu_description),
            onClick = { jokeSheetVisible = true }
        )
        // Выбор темы оформления — отдельная фича, к бете не подключена (см.
        // PROJECT_MAP): пункт меню убран целиком, а не спрятан за тостом —
        // тестеру честнее не видеть кнопку, чем нажимать на заглушку.
        // Выбор бэкенда стоит обычной строкой меню, а не чипом у версии: в
        // сборке для тестеров это рабочий инструмент — посмотреть, как ведёт
        // себя пост на тесте, — а не отладочная мелочь в углу.
        if (ApiEnvironmentHolder.isSwitchAvailable) {
            ProfileMenuItem(
                iconRes = R.drawable.ic_profile_server,
                title = stringResource(R.string.server_switch_title),
                subtitle = ApiEnvironmentHolder.environment.title,
                contentDescription = stringResource(R.string.server_switch_open),
                onClick = { serverSheetVisible = true }
            )
        }
        ProfileMenuItem(
            iconRes = R.drawable.a4dev_logo,
            title = "Студия a4dev",
            subtitle = "Сделано с душой",
            contentDescription = "Открыть студию a4dev",
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/a4development"))
                try {
                    context.startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show()
                }
            }
        )
        Spacer(Modifier.height(12.dp))
        // Настоящий versionName вместо зашитой «v0.1 beta»: тестер, присылая
        // баг, называет то, что видит, а раздач в App Distribution уже десятки.
        // Долгий тап по версии — единственный вход в тестовый краш, и только в
        // сборках с Crashlytics. Обычный тап здесь уже занят админкой, а
        // случайно удержать подпись и подтвердить диалог тестер не сможет.
        Text(
            text = "v${BuildConfig.VERSION_NAME}",
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .then(
                    if (CrashReporting.IS_ENABLED) {
                        Modifier.combinedClickable(
                            onClick = onOpenAdmin,
                            onLongClick = { testCrashDialogVisible = true }
                        )
                    } else {
                        Modifier.clickable(onClick = onOpenAdmin)
                    }
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            color = Color(0xFFB7D2FA),
            textAlign = TextAlign.Center,
            style = ProfileTypography.AppVersion
        )
    }

    if (serverSheetVisible) {
        ServerSwitchSheet(onDismiss = { serverSheetVisible = false })
    }

    if (testCrashDialogVisible) {
        TestCrashDialog(onDismiss = { testCrashDialogVisible = false })
    }

    if (jokeSheetVisible) {
        JokeSheet(onDismiss = { jokeSheetVisible = false })
    }

    if (uiState.edit != null) {
        EditProfileSheet(
            state = uiState.edit,
            avatarImageUrl = uiState.avatarImageUrl,
            hasAvatar = uiState.avatarUrl != null,
            isAvatarBusy = uiState.isAvatarBusy,
            avatarMenuVisible = uiState.avatarMenuVisible,
            onDismiss = actions.onCloseEditProfile,
            onAvatarClick = actions.onAvatarClick,
            onDismissAvatarMenu = actions.onDismissAvatarMenu,
            onPickPhoto = pickPhoto,
            onDeletePhoto = actions.onDeleteAvatarClick,
            onForumNameChange = actions.onForumNameChange,
            onForumColorSelected = actions.onForumColorSelected,
            onOpenEntityPicker = actions.onOpenEntityPicker,
            onCloseEntityPicker = actions.onCloseEntityPicker,
            onPickerQueryChange = actions.onPickerQueryChange,
            onPickerModeChange = actions.onPickerModeChange,
            onEntitySelected = actions.onEntitySelected,
            onTelegramNotificationsToggle = actions.onTelegramNotificationsToggle,
            onSaveClick = actions.onSaveProfileClick
        )
    }
}

@Composable
private fun ProfileAvatar(
    avatarImageUrl: String?,
    accent: Color,
    isBusy: Boolean,
    menuVisible: Boolean,
    hasAvatar: Boolean,
    onAvatarClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onUploadClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(76.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(KampusColors.Surface)
                .border(3.dp, accent, CircleShape)
                .clickable(enabled = !isBusy, onClick = onAvatarClick),
            contentAlignment = Alignment.Center
        ) {
            if (avatarImageUrl != null) {
                AsyncImage(
                    model = avatarImageUrl,
                    contentDescription = stringResource(R.string.profile_avatar_content_description),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_profile_avatar),
                    contentDescription = stringResource(R.string.profile_avatar_content_description),
                    tint = accent,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        DropdownMenu(
            expanded = menuVisible,
            onDismissRequest = onDismissMenu
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.profile_avatar_upload)) },
                onClick = onUploadClick,
                enabled = !isBusy
            )
            if (hasAvatar) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.profile_avatar_delete)) },
                    onClick = onDeleteClick,
                    enabled = !isBusy
                )
            }
        }

        if (isBusy) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color(0x80000000)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.5.dp,
                    color = Color.White
                )
            }
        }
    }
}

/** Пилюля действия под ником: иконка и подпись, как в вебке. */
@Composable
private fun ProfileActionButton(
    @DrawableRes iconRes: Int,
    title: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFE0EBFF),
            contentColor = KampusColors.PrimaryBlue
        ),
        border = BorderStroke(1.dp, Color(0xFFB8D2FF)),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(text = title, style = ProfileTypography.EditButton)
    }
}

/**
 * Подтверждение тестового краша. Второй осознанный шаг после долгого тапа по
 * версии: само падение необратимо, и увидеть диалог случайно нельзя.
 */
@Composable
private fun TestCrashDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.test_crash_title)) },
        text = { Text(text = stringResource(R.string.test_crash_message)) },
        confirmButton = {
            TextButton(onClick = { CrashReporting.triggerTestCrash() }) {
                Text(text = stringResource(R.string.test_crash_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.test_crash_cancel))
            }
        }
    )
}

@Composable
private fun ProfileMenuItem(
    @DrawableRes iconRes: Int,
    title: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    /** `null` — бейдж не рисуется; пустая строка — кружок без числа. */
    badgeLabel: String? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = KampusColors.Surface),
        border = BorderStroke(1.dp, KampusColors.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0xFFEAF2FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = contentDescription,
                    tint = if (iconRes == R.drawable.a4dev_logo) {
                        Color.Unspecified
                    } else {
                        KampusColors.PrimaryBlue
                    },
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = KampusColors.TextMain,
                    style = ProfileTypography.MenuItem
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = KampusColors.TextMuted,
                        style = ProfileTypography.MenuSubtitle
                    )
                }
            }
            if (badgeLabel != null) {
                NotificationsMenuBadge(label = badgeLabel)
                Spacer(Modifier.width(10.dp))
            }
            Icon(
                painter = painterResource(R.drawable.ic_arrow_right),
                contentDescription = "$title: открыть",
                tint = KampusColors.MutedBlue,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun ProfileScreenPreview() {
    YsukampusTheme(dynamicColor = false) {
        ProfileScreen(
            onOpenAdmin = {},
            isUpdateAvailable = true,
            isUpdateActionInProgress = false,
            onUpdateClick = {},
            uiState = ProfileUiState(avatarUrl = "/api/profile/avatar/123?v=12", avatarImageUrl = "https://example.com/api/profile/avatar/123?v=12")
        )
    }
}
