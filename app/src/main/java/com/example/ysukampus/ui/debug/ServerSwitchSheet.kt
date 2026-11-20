package com.example.ysukampus.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ysukampus.R
import com.example.ysukampus.data.network.ApiEnvironment
import com.example.ysukampus.data.network.ApiEnvironmentHolder
import com.example.ysukampus.ui.main.BlurShellWhileVisible
import com.example.ysukampus.ui.main.rememberAnimatedSheetDismiss
import com.example.ysukampus.ui.main.KampusColors
import com.example.ysukampus.ui.theme.ProfileTypography
import com.example.ysukampus.ui.theme.ScheduleTypography
import com.example.ysukampus.ui.theme.YsukampusTheme

/**
 * Выбор бэкенда из профиля: тестовый ↔ прод.
 *
 * Почему шторка, а не чип с мгновенным переключением, как на экране входа:
 * смена сервера чистит токены и перезапускает процесс (см. [switchEnvironment]),
 * а в профиле рядом стоят обычные пункты меню, по которым тапают не глядя.
 * Шторка даёт прочитать, что произойдёт, и уйти свайпом, ничего не сломав.
 *
 * Выбор текущего сервера просто закрывает шторку: перезапускаться незачем.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSwitchSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    BlurShellWhileVisible(sheetState)
    val dismissAnimated = rememberAnimatedSheetDismiss(sheetState, onDismiss)
    val current = ApiEnvironmentHolder.environment

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = KampusColors.Surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = stringResource(R.string.server_switch_title),
                style = ScheduleTypography.ConfirmTitle,
                color = KampusColors.TextMain,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.server_switch_hint),
                style = ScheduleTypography.ConfirmText,
                color = KampusColors.TextMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            ApiEnvironment.entries.forEach { environment ->
                ServerOption(
                    environment = environment,
                    isSelected = environment == current,
                    onClick = {
                        if (environment == current) {
                            dismissAnimated()
                        } else {
                            switchEnvironment(context, environment)
                        }
                    },
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            Text(
                text = stringResource(R.string.server_switch_warning),
                style = ProfileTypography.MenuSubtitle,
                color = KampusColors.TextMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Строка выбора: название сервера, его адрес без схемы и кружок-отметка.
 *
 * Адрес показан намеренно — на глаз «Тестовый» и «Прод» различить нельзя, а у
 * тестера на руках может быть сборка, где секреты CI разъехались с ожиданиями.
 */
@Composable
private fun ServerOption(
    environment: ApiEnvironment,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clip(OPTION_SHAPE)
            .background(if (isSelected) KampusColors.FooterBackground else KampusColors.Surface)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) KampusColors.PrimaryBlue else KampusColors.Border,
                shape = OPTION_SHAPE
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = environment.title,
                style = ProfileTypography.MenuItem,
                color = KampusColors.TextMain
            )
            Text(
                text = environment.baseUrl.substringAfter("://").trimEnd('/'),
                style = ProfileTypography.MenuSubtitle,
                color = KampusColors.TextMuted,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (isSelected) KampusColors.PrimaryBlue else KampusColors.Surface)
                .border(
                    width = if (isSelected) 0.dp else 1.5.dp,
                    color = KampusColors.Border,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    painter = painterResource(R.drawable.baseline_check_36),
                    contentDescription = null,
                    tint = KampusColors.Surface,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

private val OPTION_SHAPE = RoundedCornerShape(14.dp)

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360, heightDp = 420)
@Composable
private fun ServerSwitchSheetPreview() {
    YsukampusTheme(dynamicColor = false) {
        ServerSwitchSheet(onDismiss = {})
    }
}
