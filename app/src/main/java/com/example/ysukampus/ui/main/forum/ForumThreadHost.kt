package com.example.ysukampus.ui.main.forum

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ysukampus.R
import com.example.ysukampus.ui.main.KampusColors
import com.example.ysukampus.ui.main.KampusMotion
import com.example.ysukampus.ui.main.forum.components.ForumImageViewer

/**
 * Тред и всё, что должно пережить смену вкладки.
 *
 * Тред — не часть вкладки «Форум», а слой над оболочкой: так сделано и в вебке
 * (`ThreadOverlay` на уровне `MainPage`). Иначе открытие комментариев с главной
 * пришлось бы склеивать из двух движений подряд — переключения вкладки и
 * появления треда, — и одно и то же действие выглядело бы по-разному в
 * зависимости от того, откуда пришли.
 *
 * Здесь же живут окна жалобы/удаления поста, просмотрщик картинок и снекбар
 * форума: всё это можно вызвать из открытого треда, когда вкладка «Форум» не
 * нарисована вообще.
 *
 * Хост держат в композиции всегда — он смотрит на `openedPostId` общей
 * [ForumViewModel] и сам решает, показывать ли тред.
 */
@Composable
fun ForumThreadHost(
    viewModel: ForumViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val isThreadOpen = uiState.openedPostId != null

    // Пока тред уезжает с экрана, ViewModel уже обнулила его состояние: без
    // запомненного снимка уходящий экран мигнул бы пустым скелетоном.
    var lastThread by remember { mutableStateOf(uiState.thread) }
    LaunchedEffect(uiState.openedPostId, uiState.thread) {
        if (isThreadOpen) lastThread = uiState.thread
    }

    // Системный «назад» закрывает тред, а не приложение.
    BackHandler(enabled = isThreadOpen, onBack = viewModel::onThreadClose)

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.onMessageShown()
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Тред наезжает справа и уходит туда же — одинаково и из ленты, и с
        // главной, и по тапу в уведомлении.
        AnimatedVisibility(
            visible = isThreadOpen,
            enter = slideInHorizontally(
                animationSpec = KampusMotion.enter(KampusMotion.ScreenSlideInMs)
            ) { width -> width },
            exit = slideOutHorizontally(
                animationSpec = KampusMotion.exit<IntOffset>(KampusMotion.ScreenSlideOutMs)
            ) { width -> width }
        ) {
            ThreadScreen(
                // Уходящая копия рисуется уже с закрытым тредом в состоянии.
                state = if (isThreadOpen) uiState.thread else lastThread,
                onBack = viewModel::onThreadClose,
                onRetry = viewModel::onThreadRetry,
                onLoadMore = viewModel::onThreadLoadMore,
                onLikeClick = viewModel::onLikeClick,
                onPostReportClick = viewModel::onReportClick,
                onPostDeleteClick = viewModel::onDeleteClick,
                onDraftChange = viewModel::onCommentDraftChange,
                onImagePicked = viewModel::onCommentImagePicked,
                onImageRemoved = viewModel::onCommentImageRemoved,
                onSendComment = viewModel::onSendComment,
                onImageClick = viewModel::onImageClick,
                onCommentReportClick = viewModel::onCommentReportClick,
                onCommentDeleteClick = viewModel::onCommentDeleteClick,
                onDismissCommentConfirmation = viewModel::onDismissCommentConfirmation,
                onConfirmCommentReport = viewModel::onConfirmCommentReport,
                onConfirmCommentDelete = viewModel::onConfirmCommentDelete,
                onFocusCommentHandled = viewModel::onFocusCommentHandled,
                // Вкладка под тредом остаётся на месте — без своего фона тред
                // просвечивал бы насквозь.
                modifier = Modifier.background(KampusColors.AppBackground)
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (uiState.reportPostId != null) {
        ForumConfirmationDialog(
            title = stringResource(R.string.forum_report_title),
            text = stringResource(R.string.forum_report_text),
            confirmLabel = stringResource(R.string.forum_report),
            onConfirm = viewModel::onConfirmReport,
            onDismiss = viewModel::onDismissConfirmation
        )
    }

    if (uiState.deletePostId != null) {
        ForumConfirmationDialog(
            title = stringResource(R.string.forum_delete_title),
            text = stringResource(R.string.forum_delete_text),
            confirmLabel = stringResource(R.string.forum_delete),
            onConfirm = viewModel::onConfirmDelete,
            onDismiss = viewModel::onDismissConfirmation
        )
    }

    // Просмотрщик — поверх всего: и ленты, и открытого треда.
    val viewerImageUrl = uiState.viewerImageUrl
    if (viewerImageUrl != null) {
        ForumImageViewer(
            imageUrl = viewerImageUrl,
            isSaving = uiState.isSavingImage,
            onClose = viewModel::onImageViewerClose,
            onDownload = viewModel::onImageDownload,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        )
    }
}
