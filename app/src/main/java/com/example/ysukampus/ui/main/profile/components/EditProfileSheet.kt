package com.example.ysukampus.ui.main.profile.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ysukampus.R
import com.example.ysukampus.ui.main.BlurShellWhileVisible
import com.example.ysukampus.ui.main.KampusColors
import com.example.ysukampus.ui.main.KampusSheet
import com.example.ysukampus.ui.main.profile.EditProfilePage
import com.example.ysukampus.ui.main.profile.EditProfileState
import com.example.ysukampus.ui.main.profile.ForumColors
import com.example.ysukampus.ui.main.profile.ProfileDraft
import com.example.ysukampus.ui.main.schedule.ScheduleEntity
import com.example.ysukampus.ui.main.schedule.ScheduleEntityType
import com.example.ysukampus.ui.main.schedule.components.GroupPickerContent
import com.example.ysukampus.ui.theme.ProfileTypography
import com.example.ysukampus.ui.theme.ScheduleTypography
import com.example.ysukampus.ui.theme.YsukampusTheme

/**
 * Шторка редактирования профиля — порт `Profile/BottomSheet/EditProfile` из вебки.
 *
 * Как и там, это одна шторка с двумя страницами: форма и выбор группы или
 * преподавателя. Отдельной шторкой пикер не делаем — вложенный
 * [ModalBottomSheet] на Android ведёт себя хуже, чем смена страницы внутри.
 * Все поля правятся только в черновике, на сервер их отправляет общая кнопка.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileSheet(
    state: EditProfileState,
    avatarImageUrl: String?,
    hasAvatar: Boolean,
    isAvatarBusy: Boolean,
    avatarMenuVisible: Boolean,
    onDismiss: () -> Unit,
    onAvatarClick: () -> Unit,
    onDismissAvatarMenu: () -> Unit,
    onPickPhoto: () -> Unit,
    onDeletePhoto: () -> Unit,
    onForumNameChange: (String) -> Unit,
    onForumColorSelected: (String) -> Unit,
    onOpenEntityPicker: () -> Unit,
    onCloseEntityPicker: () -> Unit,
    onPickerQueryChange: (String) -> Unit,
    onPickerModeChange: (ScheduleEntityType) -> Unit,
    onEntitySelected: (ScheduleEntity) -> Unit,
    onTelegramNotificationsToggle: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    BlurShellWhileVisible(sheetState)
    val isBusy = state.isSaving || isAvatarBusy

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = KampusColors.AppBackground,
        modifier = modifier
    ) {
        // Со страницы пикера системная кнопка «назад» возвращает к форме,
        // а не закрывает всю шторку.
        BackHandler(enabled = state.page == EditProfilePage.Picker) { onCloseEntityPicker() }

        Column(
            modifier = Modifier
                .fillMaxHeight(KampusSheet.HeightFraction)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Crossfade(
                targetState = state.page,
                label = "edit-profile-page",
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    EditProfilePage.Form -> FormPage(
                        state = state,
                        avatarImageUrl = avatarImageUrl,
                        hasAvatar = hasAvatar,
                        isAvatarBusy = isAvatarBusy,
                        avatarMenuVisible = avatarMenuVisible,
                        isBusy = isBusy,
                        onAvatarClick = onAvatarClick,
                        onDismissAvatarMenu = onDismissAvatarMenu,
                        onPickPhoto = onPickPhoto,
                        onDeletePhoto = onDeletePhoto,
                        onForumNameChange = onForumNameChange,
                        onForumColorSelected = onForumColorSelected,
                        onOpenEntityPicker = onOpenEntityPicker,
                        onTelegramNotificationsToggle = onTelegramNotificationsToggle,
                        onSaveClick = onSaveClick
                    )

                    EditProfilePage.Picker -> PickerPage(
                        state = state,
                        onBack = onCloseEntityPicker,
                        onPickerQueryChange = onPickerQueryChange,
                        onPickerModeChange = onPickerModeChange,
                        onEntitySelected = onEntitySelected
                    )
                }
            }
        }
    }
}

@Composable
private fun FormPage(
    state: EditProfileState,
    avatarImageUrl: String?,
    hasAvatar: Boolean,
    isAvatarBusy: Boolean,
    avatarMenuVisible: Boolean,
    isBusy: Boolean,
    onAvatarClick: () -> Unit,
    onDismissAvatarMenu: () -> Unit,
    onPickPhoto: () -> Unit,
    onDeletePhoto: () -> Unit,
    onForumNameChange: (String) -> Unit,
    onForumColorSelected: (String) -> Unit,
    onOpenEntityPicker: () -> Unit,
    onTelegramNotificationsToggle: () -> Unit,
    onSaveClick: () -> Unit
) {
    val draft = state.draft
    val accent = ForumColors.toComposeColor(draft.forumColor)

    Column(modifier = Modifier.fillMaxHeight()) {
        Text(
            text = stringResource(R.string.profile_edit_title),
            style = ProfileTypography.SheetTitle,
            color = KampusColors.TextMain,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            AvatarBlock(
                avatarImageUrl = avatarImageUrl,
                accent = accent,
                hasAvatar = hasAvatar,
                isAvatarBusy = isAvatarBusy,
                menuVisible = avatarMenuVisible,
                enabled = !isBusy,
                onAvatarClick = onAvatarClick,
                onDismissMenu = onDismissAvatarMenu,
                onPickPhoto = onPickPhoto,
                onDeletePhoto = onDeletePhoto
            )

            Spacer(Modifier.height(18.dp))

            FieldLabel(stringResource(R.string.profile_edit_name_label))
            OutlinedTextField(
                value = draft.forumName,
                onValueChange = onForumNameChange,
                singleLine = true,
                // Не `enabled = false`: выключенное поле блекнет, а во время
                // сохранения текст должен остаться читаемым. readOnly просто
                // запрещает правку.
                readOnly = isBusy,
                shape = RoundedCornerShape(14.dp),
                textStyle = ProfileTypography.FieldValue,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = KampusColors.PrimaryBlue,
                    unfocusedBorderColor = KampusColors.Border,
                    focusedTextColor = KampusColors.TextMain,
                    unfocusedTextColor = KampusColors.TextMain,
                    cursorColor = KampusColors.PrimaryBlue,
                    focusedContainerColor = KampusColors.Surface,
                    unfocusedContainerColor = KampusColors.Surface,
                    disabledContainerColor = KampusColors.Surface
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.profile_edit_name_hint),
                style = ProfileTypography.FieldHint,
                // Подсказка краснеет ровно тогда, когда ник не пройдёт валидацию бэка.
                color = if (state.isForumNameValid) KampusColors.TextMuted else Color(0xFFD14343)
            )

            Spacer(Modifier.height(16.dp))

            FieldLabel(stringResource(R.string.profile_edit_color_label))
            ForumColorWheel(
                selected = draft.forumColor,
                enabled = !isBusy,
                onSelect = onForumColorSelected
            )

            Spacer(Modifier.height(16.dp))

            FieldLabel(stringResource(R.string.profile_edit_entity_label))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(KampusColors.Surface)
                    .border(1.dp, KampusColors.Border, RoundedCornerShape(14.dp))
                    .clickable(enabled = !isBusy, onClick = onOpenEntityPicker)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = draft.entityName,
                    style = ProfileTypography.FieldValue,
                    color = KampusColors.TextMain,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = KampusColors.MutedBlue,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.height(18.dp))

            FieldLabel(stringResource(R.string.profile_edit_notifications_label))
            TelegramNotificationsRow(
                enabled = draft.telegramNotificationsEnabled,
                isEnabledForInput = !isBusy,
                onToggle = onTelegramNotificationsToggle
            )

            Spacer(Modifier.height(16.dp))
        }

        SaveButton(
            text = if (state.isSaving) {
                stringResource(R.string.profile_edit_saving)
            } else {
                stringResource(R.string.profile_edit_save)
            },
            enabled = state.isSaveEnabled && !isAvatarBusy,
            onClick = onSaveClick
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PickerPage(
    state: EditProfileState,
    onBack: () -> Unit,
    onPickerQueryChange: (String) -> Unit,
    onPickerModeChange: (ScheduleEntityType) -> Unit,
    onEntitySelected: (ScheduleEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxHeight()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = stringResource(R.string.profile_edit_back),
                    tint = KampusColors.TextMain,
                    // Стрелки «назад» в проекте нет — разворачиваем ту же вправо.
                    modifier = Modifier
                        .size(20.dp)
                        .scale(scaleX = -1f, scaleY = 1f)
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(
                    if (state.pickerMode == ScheduleEntityType.Group) {
                        R.string.schedule_picker_group_title
                    } else {
                        R.string.schedule_picker_lecturer_title
                    }
                ),
                style = ScheduleTypography.PickerTitle,
                color = KampusColors.TextMain
            )
        }

        GroupPickerContent(
            members = state.pickerMembers,
            mode = state.pickerMode,
            query = state.pickerQuery,
            isLoading = state.isDirectoryLoading,
            selectedOid = state.draft.entityOid,
            onQueryChange = onPickerQueryChange,
            onModeChange = onPickerModeChange,
            onSelect = onEntitySelected
        )
    }
}

@Composable
private fun AvatarBlock(
    avatarImageUrl: String?,
    accent: Color,
    hasAvatar: Boolean,
    isAvatarBusy: Boolean,
    menuVisible: Boolean,
    enabled: Boolean,
    onAvatarClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onPickPhoto: () -> Unit,
    onDeletePhoto: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(KampusColors.Surface)
                    .border(4.dp, accent, CircleShape)
                    .clickable(enabled = enabled, onClick = onAvatarClick),
                contentAlignment = Alignment.Center
            ) {
                if (avatarImageUrl != null) {
                    AsyncImage(
                        model = avatarImageUrl,
                        contentDescription = stringResource(R.string.profile_avatar_content_description),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_profile_avatar),
                        contentDescription = stringResource(R.string.profile_avatar_content_description),
                        tint = accent,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            if (isAvatarBusy) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0x80000000)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        strokeWidth = 2.5.dp,
                        color = Color.White
                    )
                }
            }
        }

        Box {
            Text(
                text = when {
                    isAvatarBusy -> stringResource(R.string.profile_edit_photo_saving)
                    hasAvatar -> stringResource(R.string.profile_edit_photo_change)
                    else -> stringResource(R.string.profile_avatar_upload)
                },
                style = ProfileTypography.EditButton,
                color = KampusColors.PrimaryBlue,
                modifier = Modifier
                    .padding(top = 9.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = enabled, onClick = onAvatarClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )

            DropdownMenu(expanded = menuVisible, onDismissRequest = onDismissMenu) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.profile_avatar_upload)) },
                    onClick = onPickPhoto,
                    enabled = !isAvatarBusy
                )
                if (hasAvatar) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.profile_avatar_delete)) },
                        onClick = onDeletePhoto,
                        enabled = !isAvatarBusy
                    )
                }
            }
        }
    }
}

/**
 * Колесо форумных цветов.
 *
 * Вебка крутит его вручную по `scrollLeft`; здесь ту же механику даёт
 * [LazyRow] со snap-фling'ом: симметричные отступы по краям превращают
 * «прилипание к началу вьюпорта» в «прилипание к центру», а выбранным
 * считается цвет, ближайший к центральному кольцу.
 */
@Composable
private fun ForumColorWheel(
    selected: String,
    enabled: Boolean,
    onSelect: (String) -> Unit
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = ForumColors.indexOf(selected)
    )
    val snapLayoutInfoProvider = remember(listState) {
        val base = SnapLayoutInfoProvider(listState, SnapPosition.Center)
        object : SnapLayoutInfoProvider by base {
            /**
             * Один бросок — один цвет.
             *
             * По умолчанию снап сначала проигрывает фазу разгона (decay) и
             * только потом прилипает, поэтому палитра из десяти точек
             * пролетает от края до края от любого резкого движения. Нулевой
             * подлёт убирает разгон: остаётся прилипание к соседней точке.
             */
            override fun calculateApproachOffset(velocity: Float, decayOffset: Float): Float = 0f
        }
    }
    val flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider)
    val centerIndex by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            val viewportCenter = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            layout.visibleItemsInfo
                .minByOrNull { kotlin.math.abs(it.offset + it.size / 2 - viewportCenter) }
                ?.index
                ?: ForumColors.indexOf(selected)
        }
    }

    LaunchedEffect(centerIndex) {
        val color = ForumColors.values.getOrNull(centerIndex) ?: return@LaunchedEffect
        if (color != selected) onSelect(color)
    }

    // Выбор тапом и восстановление позиции после закрытия/открытия шторки:
    // прокручиваем только когда палец не на экране, иначе подерёмся со скроллом.
    LaunchedEffect(selected) {
        val target = ForumColors.indexOf(selected)
        if (!listState.isScrollInProgress && target != centerIndex) {
            listState.animateScrollToItem(target)
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val sidePadding = (maxWidth - ITEM_WIDTH) / 2

        LazyRow(
            state = listState,
            flingBehavior = flingBehavior,
            userScrollEnabled = enabled,
            contentPadding = PaddingValues(horizontal = sidePadding),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(ForumColors.values.size) { index ->
                val color = ForumColors.values[index]
                val isActive = index == centerIndex
                val scale by animateFloatAsState(
                    targetValue = if (isActive) 1.35f else 0.62f,
                    label = "color-dot-scale"
                )
                val dotAlpha by animateFloatAsState(
                    targetValue = if (isActive) 1f else 0.45f,
                    label = "color-dot-alpha"
                )

                Box(
                    modifier = Modifier
                        .width(ITEM_WIDTH)
                        .height(64.dp)
                        .clickable(enabled = enabled) { onSelect(color) },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .scale(scale)
                            .alpha(dotAlpha)
                            .clip(CircleShape)
                            .background(ForumColors.toComposeColor(color))
                    )
                }
            }
        }

        // Кольцо-прицел в центре: показывает, какой цвет станет выбранным.
        Box(
            modifier = Modifier
                .size(46.dp)
                .border(2.dp, KampusColors.MutedBlue, CircleShape)
        )
    }
}

@Composable
private fun TelegramNotificationsRow(
    enabled: Boolean,
    isEnabledForInput: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(KampusColors.Surface)
            .border(1.dp, KampusColors.Border, RoundedCornerShape(14.dp))
            .clickable(enabled = isEnabledForInput, onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0xFFEAF2FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.telegramicon),
                contentDescription = null,
                tint = KampusColors.PrimaryBlue,
                modifier = Modifier.size(17.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.profile_edit_telegram_title),
                style = ProfileTypography.MenuItem,
                color = KampusColors.TextMain
            )
            Text(
                text = stringResource(R.string.profile_edit_telegram_description),
                style = ProfileTypography.MenuSubtitle,
                color = KampusColors.TextMuted
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = { onToggle() },
            enabled = isEnabledForInput,
            // Выключенные цвета заданы теми же, что и включённые: дефолтные
            // тускло-чёрные превращали тумблер во время сохранения в кляксу.
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = KampusColors.PrimaryBlue,
                checkedBorderColor = KampusColors.PrimaryBlue,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = KampusColors.MutedBlue,
                uncheckedBorderColor = KampusColors.MutedBlue,
                disabledCheckedThumbColor = Color.White,
                disabledCheckedTrackColor = KampusColors.PrimaryBlue,
                disabledCheckedBorderColor = KampusColors.PrimaryBlue,
                disabledUncheckedThumbColor = Color.White,
                disabledUncheckedTrackColor = KampusColors.MutedBlue,
                disabledUncheckedBorderColor = KampusColors.MutedBlue
            )
        )
    }
}

@Composable
private fun SaveButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val background by animateColorAsState(
        targetValue = if (enabled) KampusColors.PrimaryBlue else KampusColors.MutedBlue,
        label = "save-button-background"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = ProfileTypography.SaveButton, color = Color.White)
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = ProfileTypography.FieldLabel,
        color = KampusColors.PrimaryBlue,
        modifier = Modifier.padding(bottom = 7.dp)
    )
}

private val ITEM_WIDTH = 56.dp

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun EditProfileSheetPreview() {
    val draft = ProfileDraft(
        forumName = "sakura_dev",
        forumColor = "#8b5cf6",
        entityOid = "1",
        entityName = "ПИ41б",
        entityType = ScheduleEntityType.Group,
        telegramNotificationsEnabled = true
    )

    YsukampusTheme(dynamicColor = false) {
        EditProfileSheet(
            state = EditProfileState(saved = draft, draft = draft.copy(forumName = "sakura")),
            avatarImageUrl = null,
            hasAvatar = false,
            isAvatarBusy = false,
            avatarMenuVisible = false,
            onDismiss = {},
            onAvatarClick = {},
            onDismissAvatarMenu = {},
            onPickPhoto = {},
            onDeletePhoto = {},
            onForumNameChange = {},
            onForumColorSelected = {},
            onOpenEntityPicker = {},
            onCloseEntityPicker = {},
            onPickerQueryChange = {},
            onPickerModeChange = {},
            onEntitySelected = {},
            onTelegramNotificationsToggle = {},
            onSaveClick = {}
        )
    }
}
