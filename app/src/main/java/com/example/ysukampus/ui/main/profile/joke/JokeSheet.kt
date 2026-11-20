package com.example.ysukampus.ui.main.profile.joke

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ysukampus.R
import com.example.ysukampus.data.auth.ProfileJoke
import com.example.ysukampus.ui.main.BlurShellWhileVisible
import com.example.ysukampus.ui.main.rememberAnimatedSheetDismiss
import com.example.ysukampus.ui.main.KampusColors
import com.example.ysukampus.ui.main.home.HomeColors
import com.example.ysukampus.ui.theme.ForumTypography
import com.example.ysukampus.ui.theme.ProfileTypography
import com.example.ysukampus.ui.theme.ScheduleTypography
import com.example.ysukampus.ui.theme.YsukampusTheme

/**
 * Шторка «Расскажи анекдот» — порт `Profile/BottomSheet/Joke` вебки.
 *
 * Первый анекдот грузится сразу при открытии, «Другой» присылает следующий,
 * не повторяя уже показанные в этом сеансе ([JokeUiState.seenIds]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JokeSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val viewModel: JokeViewModel = viewModel(
        factory = remember(context) { JokeViewModelFactory(context) }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    BlurShellWhileVisible(sheetState)
    val dismissAnimated = rememberAnimatedSheetDismiss(sheetState, onDismiss)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = KampusColors.Surface
    ) {
        JokeContent(
            uiState = uiState,
            onAnotherClick = viewModel::onAnotherClick,
            onMessageShown = viewModel::onMessageShown,
            onClose = dismissAnimated
        )
    }
}

@Composable
private fun JokeContent(
    uiState: JokeUiState,
    onAnotherClick: () -> Unit,
    onMessageShown: () -> Unit,
    onClose: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onMessageShown()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 24.dp)
            .navigationBarsPadding()
    ) {
        Text(
            text = stringResource(R.string.joke_title),
            style = ScheduleTypography.ConfirmTitle,
            color = KampusColors.TextMain
        )

        // Как в вебке (.textBox): без своей подложки — просто текст на фоне шторки.
        // Минимальная высота держит место только под спиннер первой загрузки —
        // короткий анекдот не должен тянуть за собой пустоту вебки, рассчитанную
        // на текст подлиннее.
        val joke = uiState.joke
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 4.dp)
                .then(
                    if (uiState.isLoading || joke == null) Modifier.heightIn(min = 80.dp) else Modifier
                )
        ) {
            if (uiState.isLoading || joke == null) {
                CircularProgressIndicator(
                    color = HomeColors.Primary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text(
                    text = joke.text,
                    style = ForumTypography.Body,
                    color = KampusColors.TextMain
                )
            }
        }

        // Вторичная кнопка — с обводкой, чтобы не сливалась с фоном шторки (как в вебке).
        OutlinedButton(
            onClick = onAnotherClick,
            enabled = !uiState.isRequesting,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, HomeColors.Border),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = KampusColors.Surface,
                contentColor = HomeColors.TextAccent,
                disabledContentColor = HomeColors.TextMuted
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(48.dp)
        ) {
            Text(
                text = if (uiState.isRequesting) {
                    stringResource(R.string.joke_loading)
                } else {
                    stringResource(R.string.joke_another)
                },
                style = ProfileTypography.EditButton
            )
        }

        Button(
            onClick = onClose,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = HomeColors.Primary,
                contentColor = HomeColors.OnPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(48.dp)
        ) {
            Text(
                text = stringResource(R.string.joke_close),
                style = ProfileTypography.EditButton
            )
        }

        SnackbarHost(hostState = snackbarHostState)
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun JokeContentPreview() {
    YsukampusTheme(dynamicColor = false) {
        JokeContent(
            uiState = JokeUiState(
                joke = ProfileJoke(id = 1, text = "Штирлиц шёл по коридору. Коридор был длинным."),
                isLoading = false
            ),
            onAnotherClick = {},
            onMessageShown = {},
            onClose = {}
        )
    }
}
