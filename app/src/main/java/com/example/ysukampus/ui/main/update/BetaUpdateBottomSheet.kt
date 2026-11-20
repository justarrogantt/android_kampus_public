package com.example.ysukampus.ui.main.update

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import com.example.ysukampus.R
import com.example.ysukampus.ui.main.BlurShellWhileVisible
import com.example.ysukampus.ui.main.KampusColors
import com.example.ysukampus.ui.theme.Onest
import com.example.ysukampus.ui.theme.Unbounded
import com.example.ysukampus.ui.theme.YsukampusTheme

@Composable
fun BetaUpdateBottomSheet(
    isVisible: Boolean,
    displayVersion: String?,
    buildVersion: String?,
    releaseNotes: String?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onUpdateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    BlurShellWhileVisible()

    var showReleaseNotes by remember(displayVersion, buildVersion, releaseNotes) { mutableStateOf(false) }
    val isInteractionEnabled = !isLoading
    val releaseNotesText = releaseNotes?.trim().takeUnless { it.isNullOrEmpty() }
        ?: stringResource(R.string.beta_update_notes_empty)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x8A0F172A))
            .clickable(enabled = isInteractionEnabled, onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .navigationBarsPadding(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = Color(0xFFF4F7FF),
            tonalElevation = 0.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 54.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD5E1F7))
                )
                Spacer(Modifier.height(26.dp))
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFFE3EEFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_update_sync),
                        contentDescription = stringResource(R.string.beta_update_icon_description),
                        tint = KampusColors.PrimaryBlue,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(Modifier.height(22.dp))
                Text(
                    text = stringResource(R.string.beta_update_title),
                    color = KampusColors.TextMain,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = Unbounded,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.beta_update_subtitle),
                    color = KampusColors.PrimaryBlue,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = Onest,
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp
                    )
                )
                if (!displayVersion.isNullOrBlank() || !buildVersion.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = buildVersionLabel(
                            displayVersion = displayVersion,
                            buildVersion = buildVersion
                        ),
                        color = KampusColors.TextMuted,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = Onest,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.beta_update_whats_new),
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(enabled = isInteractionEnabled) {
                            showReleaseNotes = !showReleaseNotes
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    color = KampusColors.PrimaryBlue,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = Onest,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                )
                AnimatedVisibility(visible = showReleaseNotes) {
                    Text(
                        text = releaseNotesText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        color = KampusColors.TextMuted,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = Onest,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 20.sp
                        )
                    )
                }
                Spacer(Modifier.height(30.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        enabled = isInteractionEnabled,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = KampusColors.PrimaryBlue,
                            disabledContainerColor = Color(0xFFE8EEF9),
                            disabledContentColor = KampusColors.TextMuted
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.beta_update_later),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = Unbounded,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                    }
                    Button(
                        onClick = onUpdateClick,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KampusColors.PrimaryBlue,
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFAEC7FA),
                            disabledContentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Text(
                            text = stringResource(
                                if (isLoading) R.string.beta_update_loading
                                else R.string.beta_update_action
                            ),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = Unbounded,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun buildVersionLabel(displayVersion: String?, buildVersion: String?): String {
    return when {
        !displayVersion.isNullOrBlank() && !buildVersion.isNullOrBlank() -> {
            stringResource(R.string.beta_update_version_with_build, displayVersion, buildVersion)
        }
        !displayVersion.isNullOrBlank() -> stringResource(R.string.beta_update_version_only, displayVersion)
        !buildVersion.isNullOrBlank() -> stringResource(R.string.beta_update_build_only, buildVersion)
        else -> ""
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun BetaUpdateBottomSheetPreview() {
    YsukampusTheme(dynamicColor = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(KampusColors.AppBackground)
        ) {
            BetaUpdateBottomSheet(
                isVisible = true,
                displayVersion = "1.2.0",
                buildVersion = "45",
                releaseNotes = "Исправили ошибки входа и обновили главный экран.",
                isLoading = false,
                onDismiss = {},
                onUpdateClick = {}
            )
        }
    }
}
