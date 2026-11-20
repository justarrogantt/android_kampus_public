package com.example.ysukampus.feature.registration.ui

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ysukampus.ui.admin.AdminPanelScreen
import com.example.ysukampus.R
import com.example.ysukampus.data.session.AuthState
import com.example.ysukampus.data.session.SessionManagerProvider
import com.example.ysukampus.feature.registration.ui.registrationScreens.DevLoginScreen
import com.example.ysukampus.feature.registration.ui.registrationScreens.ErrorRegistrationScreen
import com.example.ysukampus.feature.registration.ui.registrationScreens.SessionCheckScreen
import com.example.ysukampus.feature.registration.ui.registrationScreens.SuccessRegistrationScreen
import com.example.ysukampus.feature.registration.ui.registrationScreens.TelegramRegistrationScreen
import com.example.ysukampus.ui.main.MainScreen
import com.example.ysukampus.ui.main.profile.ProfileViewModel
import com.example.ysukampus.ui.main.profile.ProfileViewModelFactory
import com.example.ysukampus.ui.main.profile.asProfileActions
import com.example.ysukampus.ui.debug.ServerSwitcher
import com.example.ysukampus.ui.main.update.BetaUpdateViewModel
import com.example.ysukampus.ui.main.update.BetaUpdateViewModelFactory
import com.example.ysukampus.ui.theme.YsuAdminTheme

@Composable
fun RegistrationFlowScreen(
    appContext: Context
) {
    var showAdminPanel by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        val sessionManager = remember(appContext) {
            SessionManagerProvider.get(appContext)
        }
        val registrationViewModel: RegistrationViewModel = viewModel(
            factory = RegistrationViewModelFactory(appContext)
        )
        val authState by sessionManager.authState.collectAsState()

        LaunchedEffect(Unit) {
            registrationViewModel.onAppStarted()
        }

        LaunchedEffect(authState, showAdminPanel, registrationViewModel.registrationUiState) {
            val isInAuthorizedArea =
                showAdminPanel || registrationViewModel.registrationUiState is RegistrationUiState.AuthorizedActions
            if (authState == AuthState.NotAuthorized && isInAuthorizedArea) {
                showAdminPanel = false
                registrationViewModel.onSessionExpired()
            }
        }

        val state = registrationViewModel.registrationUiState

        Box(modifier = Modifier.fillMaxSize()) {
            when (state) {
                is RegistrationUiState.AuthorizedActions -> {
                    AuthorizedMainRoute(
                        appContext = appContext,
                        showAdminPanel = showAdminPanel,
                        onOpenAdmin = { showAdminPanel = true },
                        onCloseAdmin = { showAdminPanel = false }
                    )
                }
                RegistrationUiState.CheckingSession -> SessionCheckScreen()
                RegistrationUiState.Idle -> TelegramRegistrationScreen(
                    code = "",
                    demoMessage = null,
                    isCodeReady = false,
                    onGetNewCodeClick = registrationViewModel::getAuthentificationCode
                )
                RegistrationUiState.DevLoading -> DevLoginScreen(
                    message = appContext.getString(R.string.dev_login_loading_text),
                    isLoading = true,
                    onLoginClick = registrationViewModel::onRetryRegistration
                )
                RegistrationUiState.LoadingCode -> TelegramRegistrationScreen(
                    code = "Ожидаем код..",
                    demoMessage = null,
                    isCodeReady = false,
                    onGetNewCodeClick = registrationViewModel::getAuthentificationCode
                )
                is RegistrationUiState.DevLoginPrompt -> DevLoginScreen(
                    message = state.message,
                    isLoading = false,
                    onLoginClick = registrationViewModel::onRetryRegistration
                )
                is RegistrationUiState.WaitingConfirmation -> TelegramRegistrationScreen(
                    code = state.code,
                    demoMessage = null,
                    onGetNewCodeClick = registrationViewModel::getAuthentificationCode
                )
                is RegistrationUiState.NeedsRegistration -> TelegramRegistrationScreen(
                    code = state.code,
                    demoMessage = stringResource(R.string.telegram_registration_needs_registration_text),
                    onGetNewCodeClick = registrationViewModel::getAuthentificationCode
                )
                is RegistrationUiState.Success -> SuccessRegistrationScreen(
                    user = state.user,
                    onOpenScheduleClick = registrationViewModel::onOpenScheduleClick
                )
                is RegistrationUiState.Error -> ErrorRegistrationScreen(
                    errorMessage = state.message,
                    onRetryClick = registrationViewModel::onRetryRegistration
                )
            }

            // Отладочный переключатель бэкенда: оверлеем в правом верхнем углу,
            // ниже логотипа, чтобы ничего не перекрывать. В авторизованной зоне
            // он живёт внизу профиля, поэтому здесь только на экранах регистрации.
            if (state !is RegistrationUiState.AuthorizedActions &&
                state !is RegistrationUiState.CheckingSession
            ) {
                ServerSwitcher(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 64.dp, end = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun AuthorizedMainRoute(
    appContext: Context,
    showAdminPanel: Boolean,
    onOpenAdmin: () -> Unit,
    onCloseAdmin: () -> Unit
) {
    val betaUpdateFactory = remember(appContext) {
        BetaUpdateViewModelFactory(appContext)
    }
    val profileFactory = remember(appContext) {
        ProfileViewModelFactory(appContext)
    }
    val betaUpdateViewModel: BetaUpdateViewModel = viewModel(factory = betaUpdateFactory)
    val profileViewModel: ProfileViewModel = viewModel(factory = profileFactory)
    val betaUpdateState by betaUpdateViewModel.uiState.collectAsState()
    val profileUiState by profileViewModel.uiState.collectAsState()
    val profileActions = remember(profileViewModel) { profileViewModel.asProfileActions() }

    LaunchedEffect(Unit) {
        betaUpdateViewModel.checkForUpdate()
    }

    if (showAdminPanel) {
        YsuAdminTheme {
            AdminPanelScreen(onOpenMain = onCloseAdmin)
        }
    } else {
        MainScreen(
            onOpenAdmin = onOpenAdmin,
            betaUpdateState = betaUpdateState,
            onDismissBetaUpdate = betaUpdateViewModel::dismissBottomSheet,
            onStartBetaUpdate = betaUpdateViewModel::startUpdate,
            profileUiState = profileUiState,
            profileActions = profileActions
        )
    }
}
