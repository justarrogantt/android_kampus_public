package com.example.ysukampus.ui.admin.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.ysukampus.R
import com.example.ysukampus.ui.admin.AdminActionKind
import com.example.ysukampus.ui.admin.state.AdminModerationTab
import com.example.ysukampus.ui.admin.state.AdminModerationTargetType
import com.example.ysukampus.ui.admin.AdminPermission
import com.example.ysukampus.ui.admin.AdminRole
import com.example.ysukampus.ui.admin.AdminSelectOption
import com.example.ysukampus.ui.admin.AdminStatus
import com.example.ysukampus.ui.admin.AdminTab
import com.example.ysukampus.ui.theme.YsuAdminColors
import com.example.ysukampus.ui.theme.YsuAdminDimens
import com.example.ysukampus.ui.theme.YsuAdminTextStyles

@Composable
fun AdminHeader(
    modifier: Modifier = Modifier,
    onOpenMain: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(
                text = buildAnnotatedString {
                    append("ysu")
                    withStyle(SpanStyle(color = YsuAdminColors.Primary)) { append("k") }
                    append("ampus")
                },
                style = YsuAdminTextStyles.Logo,
                color = YsuAdminColors.Text
            )
            Text("АДМИН-ПАНЕЛЬ", style = YsuAdminTextStyles.AdminSubtitle, color = YsuAdminColors.TextMuted)
        }
        if (onOpenMain != null) {
            AdminHomeButton(onClick = onOpenMain)
        }
    }
}

@Composable
private fun AdminHomeButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(YsuAdminColors.Primary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_main_home),
            contentDescription = "Вернуться на главную",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun AdminTabHeader(
    currentTab: AdminTab,
    totals: Map<AdminTab, Int>,
    /** Вкладки, открытые правами пользователя; остальные не рисуем, как вебка. */
    tabs: List<AdminTab> = AdminTab.entries,
    onTabSelected: (AdminTab) -> Unit,
    onAddClick: (() -> Unit)?,
    onSecondaryActionClick: (() -> Unit)? = null,
    secondaryActionIconText: String = "",
    isRefreshing: Boolean = false,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(currentTab.title, style = YsuAdminTextStyles.ScreenTitle, color = YsuAdminColors.Text)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CountChip("Всего: ${totals[currentTab] ?: 0}")
                if (onAddClick != null) SmallIconButton("+", onAddClick)
                if (onSecondaryActionClick != null) {
                    SmallIconButton(secondaryActionIconText, onSecondaryActionClick)
                }
                SmallIconButton(
                    text = if (isRefreshing) "…" else "↻",
                    onClick = onRefreshClick,
                    enabled = !isRefreshing
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tabs.forEach { tab ->
                val active = tab == currentTab
                Box(
                    modifier = Modifier
                        .height(YsuAdminDimens.NavHeight)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (active) YsuAdminColors.Primary else YsuAdminColors.Surface)
                        .border(1.dp, if (active) Color.Transparent else YsuAdminColors.Border, RoundedCornerShape(18.dp))
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(tab.title, style = YsuAdminTextStyles.Tab, color = if (active) Color.White else YsuAdminColors.PrimaryDark)
                }
            }
        }
    }
}

@Composable
private fun CountChip(text: String) {
    Box(
        modifier = Modifier
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(YsuAdminColors.PrimaryBright)
            .border(1.dp, YsuAdminColors.Border, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = YsuAdminTextStyles.TotalCount, color = YsuAdminColors.PrimaryDark)
    }
}

@Composable
fun SmallIconButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .size(YsuAdminDimens.SmallIconButton)
            .clip(RoundedCornerShape(999.dp))
            .background(if (enabled) YsuAdminColors.Primary else YsuAdminColors.Border)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, style = YsuAdminTextStyles.Count)
    }
}

@Composable
fun AdminSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    AdminInputFrame(modifier.height(YsuAdminDimens.SearchHeight), enabled) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("⌕", color = YsuAdminColors.PrimaryDark, style = YsuAdminTextStyles.Input)
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = YsuAdminTextStyles.Input.copy(color = YsuAdminColors.Text),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(placeholder, style = YsuAdminTextStyles.Input, color = YsuAdminColors.TextMuted)
                    inner()
                }
            )
        }
    }
}

/**
 * Подвкладки модерации. [tabs] задаётся снаружи, потому что «История» доступна
 * только с правом `get-moderation-history`; [dotTabs] — точки-индикаторы
 * непустых списков, как в вебке.
 */
@Composable
fun AdminModerationSubTabs(
    currentTab: AdminModerationTab,
    onTabSelected: (AdminModerationTab) -> Unit,
    modifier: Modifier = Modifier,
    tabs: List<AdminModerationTab> = AdminModerationTab.entries,
    dotTabs: Set<AdminModerationTab> = emptySet()
) {
    AdminSegmentedTabs(
        titles = tabs.map { it.title },
        selectedIndex = tabs.indexOf(currentTab),
        onSelected = { index -> onTabSelected(tabs[index]) },
        modifier = modifier,
        dotIndexes = dotTabs.map { tabs.indexOf(it) }.filter { it >= 0 }.toSet()
    )
}

/**
 * Сегментированный переключатель вебки (`AdminSegmented`): подвкладки модерации,
 * разделы «Доступов» и виды статистики рисуются им одинаково. [dotIndexes] —
 * точки-индикаторы непустых разделов.
 */
@Composable
fun AdminSegmentedTabs(
    titles: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    dotIndexes: Set<Int> = emptySet()
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(YsuAdminColors.BackgroundSecondary)
            .border(1.dp, YsuAdminColors.Border, RoundedCornerShape(18.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        titles.forEachIndexed { index, title ->
            val isActive = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(YsuAdminDimens.NavHeight)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isActive) YsuAdminColors.Primary else YsuAdminColors.Surface)
                    .clickable { onSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = title,
                        style = YsuAdminTextStyles.Tab,
                        color = if (isActive) Color.White else YsuAdminColors.PrimaryDark
                    )
                    if (index in dotIndexes) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (isActive) Color.White else YsuAdminColors.Primary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminFreshContentPill(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(YsuAdminColors.Primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 0) "↑ Свежие посты" else "↑ Свежие посты",
            style = YsuAdminTextStyles.ActionButton,
            color = Color.White
        )
    }
}

/** Живой размер очереди модерации: приезжает событием `moderation_count`. */
@Composable
fun AdminModerationQueueCounter(
    count: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(YsuAdminColors.PrimaryBright)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.admin_moderation_queue_count, count),
            style = YsuAdminTextStyles.Badge,
            color = YsuAdminColors.TextSecondary
        )
    }
}

/**
 * Карточка списка модерации: иконка объекта, автор (или модератор в истории),
 * подпись, дата и бейдж статуса. [isNew] подсвечивает то, что появилось после
 * прошлого захода на подвкладку.
 */
@Composable
fun AdminModerationCard(
    iconTargetType: AdminModerationTargetType?,
    title: String,
    subtitle: String,
    createdAt: String,
    badgeText: String,
    badgeStatus: AdminStatus,
    onClick: (() -> Unit)? = null,
    isNew: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cardModifier = modifier.fillMaxWidth().then(
        if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    )
    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(YsuAdminDimens.CardRadius),
        colors = CardDefaults.cardColors(
            containerColor = if (isNew) YsuAdminColors.BackgroundSecondary else YsuAdminColors.Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(YsuAdminDimens.CardPadding),
            verticalAlignment = Alignment.Top
        ) {
            AdminModerationIcon(targetType = iconTargetType)
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = YsuAdminTextStyles.CardTitle,
                    color = YsuAdminColors.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = YsuAdminTextStyles.CardSubtitle,
                    color = YsuAdminColors.PrimaryDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = createdAt,
                    style = YsuAdminTextStyles.CardSubtitle,
                    color = YsuAdminColors.PrimaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            AdminStatusBadge(text = badgeText, status = badgeStatus)
        }
    }
}

@Composable
fun AdminEntityCard(
    iconText: String,
    title: String,
    subtitle: String,
    statusText: String? = null,
    status: AdminStatus = AdminStatus.Active,
    chips: List<String> = emptyList(),
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(YsuAdminDimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = YsuAdminColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(YsuAdminDimens.CardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBox(iconText)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = YsuAdminTextStyles.CardTitle, color = YsuAdminColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(subtitle, style = YsuAdminTextStyles.CardSubtitle, color = YsuAdminColors.PrimaryDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (statusText != null) AdminStatusBadge(statusText, status)
            }
            if (chips.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { chips.forEach { AdminRoleChip(it) } }
            }
        }
    }
}

@Composable
fun AdminHistoryEntityCard(
    iconText: String,
    title: String,
    subtitle: String,
    changedAt: String,
    badgeText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(YsuAdminDimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = YsuAdminColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(YsuAdminDimens.CardPadding),
            verticalAlignment = Alignment.Top
        ) {
            IconBox(iconText)
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    title,
                    style = YsuAdminTextStyles.CardTitle,
                    color = YsuAdminColors.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    style = YsuAdminTextStyles.CardSubtitle,
                    color = YsuAdminColors.PrimaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    changedAt,
                    style = YsuAdminTextStyles.CardSubtitle,
                    color = YsuAdminColors.PrimaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            AdminHistoryTypeBadge(badgeText)
        }
    }
}

@Composable
fun AdminRequestLogCard(
    method: String,
    path: String,
    controllerMethod: String,
    calledAt: String,
    status: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(YsuAdminDimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = YsuAdminColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(YsuAdminDimens.CardPadding),
            verticalAlignment = Alignment.Top
        ) {
            IconBox("⚙")
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    "$method $path",
                    style = YsuAdminTextStyles.CardTitle,
                    color = YsuAdminColors.Text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    controllerMethod,
                    style = YsuAdminTextStyles.CardSubtitle,
                    color = YsuAdminColors.PrimaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    calledAt,
                    style = YsuAdminTextStyles.CardSubtitle,
                    color = YsuAdminColors.PrimaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            val statusState = when (status) {
                in 200..299 -> AdminStatus.Active
                in 300..499 -> AdminStatus.SoftDeleted
                else -> AdminStatus.HardDanger
            }
            AdminStatusBadge(text = status.toString(), status = statusState)
        }
    }
}

@Composable
fun AdminHistoryTypeBadge(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(YsuAdminDimens.BadgeHeight)
            .clip(RoundedCornerShape(999.dp))
            .background(YsuAdminColors.PrimaryBright)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = YsuAdminTextStyles.Badge, color = YsuAdminColors.PrimaryDark)
    }
}

@Composable
private fun IconBox(text: String) {
    Box(
        Modifier
            .size(YsuAdminDimens.CardIconSize)
            .clip(RoundedCornerShape(YsuAdminDimens.CardIconRadius))
            .background(YsuAdminColors.PrimaryBright)
            .border(1.dp, YsuAdminColors.Border, RoundedCornerShape(YsuAdminDimens.CardIconRadius)),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = YsuAdminColors.PrimaryDark, style = YsuAdminTextStyles.ScreenTitle)
    }
}

/** `null` — объект события истории не пост и не комментарий, а пользователь. */
@Composable
private fun AdminModerationIcon(targetType: AdminModerationTargetType?) {
    val painterRes = when (targetType) {
        AdminModerationTargetType.Post -> R.drawable.baseline_article_24
        AdminModerationTargetType.Comment -> R.drawable.ic_main_chat
        null -> R.drawable.ic_profile_avatar
    }
    Box(
        modifier = Modifier
            .size(YsuAdminDimens.CardIconSize)
            .clip(RoundedCornerShape(YsuAdminDimens.CardIconRadius))
            .background(YsuAdminColors.BackgroundTertiary)
            .border(1.dp, YsuAdminColors.Border, RoundedCornerShape(YsuAdminDimens.CardIconRadius)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(painterRes),
            contentDescription = null,
            tint = YsuAdminColors.PrimaryDark,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun AdminStatusBadge(text: String, status: AdminStatus, modifier: Modifier = Modifier) {
    val (fg, bg) = when (status) {
        AdminStatus.Active -> YsuAdminColors.Success to YsuAdminColors.SuccessBg
        AdminStatus.SoftDeleted -> YsuAdminColors.Warning to YsuAdminColors.WarningBg
        AdminStatus.HardDanger -> YsuAdminColors.Danger to YsuAdminColors.DangerBg
        AdminStatus.Primary -> YsuAdminColors.PrimaryDark to YsuAdminColors.PrimaryBright
    }
    Box(
        modifier = modifier
            .height(YsuAdminDimens.BadgeHeight)
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, style = YsuAdminTextStyles.Badge, color = fg) }
}

@Composable
fun AdminRoleChip(text: String, isDeleted: Boolean = false) {
    AdminStatusBadge(text, if (isDeleted) AdminStatus.SoftDeleted else AdminStatus.Primary)
}

@Composable
fun AdminDialogFrame(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x66111827)),
        contentAlignment = Alignment.Center
    ) {
        AdminDialogPanel(title = title, onClose = onClose, modifier = modifier, content = content)
    }
}

@Composable
private fun AdminDialogPanel(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(0.90f).fillMaxHeight(0.84f),
        shape = RoundedCornerShape(YsuAdminDimens.DialogRadius),
        colors = CardDefaults.cardColors(containerColor = YsuAdminColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(YsuAdminDimens.DialogPadding)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = YsuAdminTextStyles.DialogTitle, color = YsuAdminColors.Text)
                Box(
                    modifier = Modifier
                        .size(YsuAdminDimens.CloseButton)
                        .clip(RoundedCornerShape(7.dp))
                        .background(YsuAdminColors.PrimaryBright)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) { Text("×", color = YsuAdminColors.PrimaryDark, style = YsuAdminTextStyles.ScreenTitle) }
            }
            Spacer(Modifier.height(16.dp))
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), content = content)
        }
    }
}

@Composable
fun AdminSheetBlock(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(YsuAdminDimens.SheetBlockRadius))
            .background(YsuAdminColors.Surface)
            .border(1.dp, YsuAdminColors.BorderStrong, RoundedCornerShape(YsuAdminDimens.SheetBlockRadius))
            .padding(YsuAdminDimens.SheetBlockPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
fun AdminSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = YsuAdminTextStyles.SectionLabel, color = YsuAdminColors.Text)
}

@Composable
fun AdminEmptyStateCard(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(YsuAdminColors.Surface)
            .border(1.dp, YsuAdminColors.Border, RoundedCornerShape(16.dp))
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(title, style = YsuAdminTextStyles.CardTitle, color = YsuAdminColors.Text)
        Text(subtitle, style = YsuAdminTextStyles.CardSubtitle, color = YsuAdminColors.TextSecondary)
    }
}

@Composable
fun AdminLoadingStateCard(text: String = "Загрузка данных...", modifier: Modifier = Modifier) {
    AdminEmptyStateCard(
        title = text,
        subtitle = "Данные появятся после загрузки",
        modifier = modifier
    )
}

@Composable
fun AdminCodeBlock(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(YsuAdminColors.BackgroundSecondary)
            .border(1.dp, YsuAdminColors.Border, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            style = YsuAdminTextStyles.CardSubtitle.copy(color = YsuAdminColors.Text),
            maxLines = Int.MAX_VALUE
        )
    }
}

@Composable
fun AdminFeedbackCard(
    title: String,
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val foreground = if (isError) YsuAdminColors.Danger else YsuAdminColors.Success
    val background = if (isError) YsuAdminColors.DangerBg else YsuAdminColors.SuccessBg
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .border(1.dp, foreground.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = YsuAdminTextStyles.CardTitle, color = foreground)
            Text(message, style = YsuAdminTextStyles.CardSubtitle, color = YsuAdminColors.Text, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(YsuAdminColors.Surface)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Text("×", style = YsuAdminTextStyles.CardTitle, color = foreground)
        }
    }
}

@Composable
fun AdminTextField(value: String, onValueChange: (String) -> Unit, placeholder: String? = null, modifier: Modifier = Modifier, enabled: Boolean = true) {
    AdminInputFrame(modifier.height(YsuAdminDimens.InputHeight), enabled) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = YsuAdminTextStyles.Input.copy(color = YsuAdminColors.Text),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty() && placeholder != null) Text(placeholder, style = YsuAdminTextStyles.Input, color = YsuAdminColors.TextMuted)
                    inner()
                }
            }
        )
    }
}

@Composable
fun AdminTextArea(value: String, onValueChange: (String) -> Unit, placeholder: String? = null, modifier: Modifier = Modifier, enabled: Boolean = true) {
    AdminInputFrame(modifier.heightIn(min = YsuAdminDimens.TextAreaMinHeight), enabled) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            textStyle = YsuAdminTextStyles.Input.copy(color = YsuAdminColors.Text),
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            decorationBox = { inner ->
                if (value.isEmpty() && placeholder != null) Text(placeholder, style = YsuAdminTextStyles.Input, color = YsuAdminColors.TextMuted)
                inner()
            }
        )
    }
}

@Composable
private fun AdminInputFrame(modifier: Modifier, enabled: Boolean, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(if (enabled) YsuAdminColors.Surface else YsuAdminColors.BackgroundSecondary)
            .border(1.dp, YsuAdminColors.BorderStrong, RoundedCornerShape(15.dp)),
        contentAlignment = Alignment.CenterStart,
        content = content
    )
}

@Composable
fun AdminSelect(
    value: AdminSelectOption?,
    placeholder: String,
    options: List<AdminSelectOption>,
    onValueChange: (AdminSelectOption?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    forceExpanded: Boolean = false
) {
    var expanded by remember { mutableStateOf(forceExpanded) }
    val useInlinePreviewMenu = LocalInspectionMode.current && forceExpanded
    val menuScrollState = rememberScrollState()
    Box(modifier = modifier.fillMaxWidth()) {
        Column {
            AdminInputFrame(Modifier.height(YsuAdminDimens.InputHeight).clickable(enabled = enabled) { expanded = !expanded }, enabled) {
                Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(value?.title ?: placeholder, modifier = Modifier.weight(1f), style = YsuAdminTextStyles.Input, color = YsuAdminColors.Text)
                    Box(
                        modifier = Modifier.size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(
                            modifier = Modifier
                                .size(14.dp)
                                .rotate(if (expanded) 180f else 0f),
                        ) {
                            val strokeWidth = 2.dp.toPx()
                            drawLine(
                                color = YsuAdminColors.Primary,
                                start = Offset(size.width * 0.12f, size.height * 0.32f),
                                end = Offset(size.width * 0.5f, size.height * 0.7f),
                                strokeWidth = strokeWidth,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = YsuAdminColors.Primary,
                                start = Offset(size.width * 0.5f, size.height * 0.7f),
                                end = Offset(size.width * 0.88f, size.height * 0.32f),
                                strokeWidth = strokeWidth,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
            if (useInlinePreviewMenu && expanded) {
                AdminInlineDropdownMenu(
                    placeholder = placeholder,
                    options = options,
                    onValueChange = onValueChange,
                    onCollapse = { expanded = false }
                )
            }
        }
        if (!useInlinePreviewMenu) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .padding(vertical = AdminSelectMenuVerticalGap)
                    .heightIn(max = AdminSelectMenuMaxHeight),
                shape = AdminSelectMenuShape,
                containerColor = YsuAdminColors.Surface,
                border = BorderStroke(1.dp, YsuAdminColors.BorderStrong),
                scrollState = menuScrollState
            ) {
                DropdownMenuItem(
                    text = { Text(placeholder, style = YsuAdminTextStyles.Input, color = Color.White) },
                    onClick = { onValueChange(null); expanded = false },
                    modifier = Modifier.background(YsuAdminColors.Primary)
                )
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.title, style = YsuAdminTextStyles.Input, color = YsuAdminColors.Text) },
                        onClick = { onValueChange(option); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminInlineDropdownMenu(
    placeholder: String,
    options: List<AdminSelectOption>,
    onValueChange: (AdminSelectOption?) -> Unit,
    onCollapse: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AdminSelectMenuVerticalGap)
            .heightIn(max = AdminSelectMenuMaxHeight)
            .clip(AdminSelectMenuShape)
            .background(YsuAdminColors.Surface, AdminSelectMenuShape)
            .border(1.dp, YsuAdminColors.BorderStrong, AdminSelectMenuShape)
            .verticalScroll(rememberScrollState())
    ) {
        DropdownMenuItem(
            text = { Text(placeholder, style = YsuAdminTextStyles.Input, color = Color.White) },
            onClick = {
                onValueChange(null)
                onCollapse()
            },
            modifier = Modifier.background(YsuAdminColors.Primary)
        )
        options.forEach { option ->
            DropdownMenuItem(
                text = { Text(option.title, style = YsuAdminTextStyles.Input, color = YsuAdminColors.Text) },
                onClick = {
                    onValueChange(option)
                    onCollapse()
                }
            )
        }
    }
}

private val AdminSelectMenuMaxHeight = 320.dp
private val AdminSelectMenuVerticalGap = 10.dp
private val AdminSelectMenuShape = RoundedCornerShape(12.dp)

@Composable
fun AdminPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(YsuAdminDimens.ActionButtonHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) Brush.linearGradient(listOf(YsuAdminColors.Primary, YsuAdminColors.PrimaryLight)) else Brush.linearGradient(listOf(YsuAdminColors.BackgroundTertiary, YsuAdminColors.BackgroundTertiary)))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, style = YsuAdminTextStyles.ActionButton, color = Color.White) }
}

@Composable
fun AdminActionButton(text: String, kind: AdminActionKind, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val (fg, bg) = when (kind) {
        AdminActionKind.Primary -> YsuAdminColors.PrimaryDark to YsuAdminColors.PrimaryBright
        AdminActionKind.SoftWarning -> YsuAdminColors.Warning to YsuAdminColors.WarningBg
        AdminActionKind.Danger -> YsuAdminColors.Danger to YsuAdminColors.DangerBg
        AdminActionKind.Restore -> YsuAdminColors.Success to YsuAdminColors.SuccessBg
    }
    Box(
        modifier = modifier
            .height(YsuAdminDimens.ActionButtonHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, style = YsuAdminTextStyles.ActionButton, color = fg) }
}

@Composable
fun RoleListItem(role: AdminRole, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    EntityListItem(role.name, role.slug, if (role.isDeleted) "удалена" else "активна", role.isDeleted, selected, onClick, modifier)
}

@Composable
fun PermissionListItem(permission: AdminPermission, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    EntityListItem(permission.name, permission.slug, if (permission.isDeleted) "удалено" else "активно", permission.isDeleted, selected, onClick, modifier)
}

@Composable
private fun EntityListItem(title: String, subtitle: String, statusText: String, deleted: Boolean, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(YsuAdminColors.Surface)
            .border(1.dp, if (selected) YsuAdminColors.PrimaryLight else YsuAdminColors.Border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = YsuAdminTextStyles.CardTitle, color = YsuAdminColors.Text)
            Text(subtitle, style = YsuAdminTextStyles.CardSubtitle, color = YsuAdminColors.PrimaryDark)
        }
        AdminStatusBadge(statusText, if (deleted) AdminStatus.SoftDeleted else AdminStatus.Active)
    }
}
