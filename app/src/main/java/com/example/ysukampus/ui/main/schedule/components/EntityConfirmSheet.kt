package com.example.ysukampus.ui.main.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ysukampus.R
import com.example.ysukampus.ui.main.BlurShellWhileVisible
import com.example.ysukampus.ui.main.rememberAnimatedSheetDismiss
import com.example.ysukampus.ui.main.KampusColors
import com.example.ysukampus.ui.main.schedule.ScheduleColors
import com.example.ysukampus.ui.main.schedule.ScheduleEntity
import com.example.ysukampus.ui.main.schedule.ScheduleEntityType
import com.example.ysukampus.ui.theme.ScheduleTypography
import com.example.ysukampus.ui.theme.YsukampusTheme

/**
 * Подтверждение перехода на чужое расписание.
 *
 * Строки в карточке пары идут вплотную, и случайный тап по имени уводил бы
 * с экрана — поэтому сначала спрашиваем. Как и пикер, шторка сделана на
 * [ModalBottomSheet]: свайп вниз и системная кнопка «назад» приходят из коробки
 * и отменяют переход тем же [onDismiss].
 *
 * Компонент презентационный: знает только про сущность и два события.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntityConfirmSheet(
    entity: ScheduleEntity,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    BlurShellWhileVisible(sheetState)
    val dismissAnimated = rememberAnimatedSheetDismiss(sheetState, onDismiss)
    val isLecturer = entity.type == ScheduleEntityType.Lecturer

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = KampusColors.Surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(
                    if (isLecturer) R.drawable.ic_schedule_teacher else R.drawable.ic_schedule_audience
                ),
                contentDescription = null,
                tint = KampusColors.TextMain,
                modifier = Modifier.size(32.dp)
            )

            Text(
                text = entity.name,
                style = ScheduleTypography.ConfirmTitle,
                color = KampusColors.TextMain,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )

            Text(
                text = stringResource(
                    if (isLecturer) {
                        R.string.schedule_entity_confirm_lecturer
                    } else {
                        R.string.schedule_entity_confirm_group
                    }
                ),
                style = ScheduleTypography.ConfirmText,
                color = KampusColors.TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            ConfirmButton(
                text = stringResource(R.string.schedule_entity_confirm_submit),
                textColor = KampusColors.Surface,
                decoration = Modifier.background(KampusColors.PrimaryBlue, BUTTON_SHAPE),
                onClick = onConfirm,
                modifier = Modifier.padding(top = 24.dp)
            )

            ConfirmButton(
                text = stringResource(R.string.schedule_entity_confirm_cancel),
                textColor = ScheduleColors.TextAccent,
                decoration = Modifier.border(1.5.dp, KampusColors.Border, BUTTON_SHAPE),
                onClick = dismissAnimated,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

private val BUTTON_SHAPE = RoundedCornerShape(14.dp)

/**
 * Кнопка шторки. Размер, форма и область тапа общие, а заливку или обводку
 * приносит [decoration] — она применяется уже после того, как кнопка растянута
 * по ширине, чтобы фон и рамка совпадали с областью тапа.
 */
@Composable
private fun ConfirmButton(
    text: String,
    textColor: Color,
    decoration: Modifier,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .then(decoration)
            .clip(BUTTON_SHAPE)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = ScheduleTypography.ConfirmButton,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360, heightDp = 420)
@Composable
private fun EntityConfirmSheetGroupPreview() {
    YsukampusTheme(dynamicColor = false) {
        EntityConfirmSheet(
            entity = ScheduleEntity("2", "ИВТ41б", ScheduleEntityType.Group),
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360, heightDp = 420)
@Composable
private fun EntityConfirmSheetLecturerPreview() {
    YsukampusTheme(dynamicColor = false) {
        EntityConfirmSheet(
            entity = ScheduleEntity("l-1", "Иванов И. И.", ScheduleEntityType.Lecturer),
            onConfirm = {},
            onDismiss = {}
        )
    }
}
