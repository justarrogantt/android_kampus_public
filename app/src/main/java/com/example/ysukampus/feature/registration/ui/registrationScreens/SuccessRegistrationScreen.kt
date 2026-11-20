package com.example.ysukampus.feature.registration.ui.registrationScreens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ysukampus.R
import com.example.ysukampus.data.network.dto.UserDto
import com.example.ysukampus.feature.registration.ui.components.PrimaryActionButton
import com.example.ysukampus.feature.registration.ui.components.ShowLogo
import com.example.ysukampus.feature.registration.ui.components.StateIcon
import com.example.ysukampus.feature.registration.ui.components.StateText
import com.example.ysukampus.feature.registration.ui.components.StudentCard
import com.example.ysukampus.ui.theme.YsukampusTheme

@Composable
fun SuccessRegistrationScreen(
    user: UserDto,
    onOpenScheduleClick: () -> Unit
){
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(colorResource(R.color.background_color))
    ) {
        ShowLogo(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        )
        Spacer(modifier = Modifier.weight(1f))
        StateIcon(
            painter = painterResource(R.drawable.baseline_check_36),
            shape = CircleShape,
            backgroundColorRes = R.color.success_registration_icon_background_color,
            borderColorRes = R.color.success_registration_icon_border_color,
            iconColorRes = R.color.success_registration_icon_color,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(32.dp))
        StateText(
            title = stringResource(R.string.success_registration_title_text),
            label = stringResource(R.string.success_registration_label_text),
            titleStyle = MaterialTheme.typography.titleLarge,
            labelStyle = MaterialTheme.typography.labelMedium
        )
        Spacer(modifier = Modifier.weight(1f))
        StudentCard(
            name = user.firstName,
            usernametg = "@${user.username}",
            role = stringResource(R.string.success_registration_card_role),
            group = stringResource(R.string.success_registration_card_group),
            modifier = Modifier
                .padding(
                    horizontal = 32.dp
                )
        )
        Spacer(modifier = Modifier.height(16.dp))
        PrimaryActionButton(
            text = stringResource(R.string.success_registration_button_text),
            gradientStartColor = R.color.second_text_color,
            gradientEndColor = R.color.fade_end,
            trailingIcon = painterResource(R.drawable.ic_arrow_right),
            onClick = onOpenScheduleClick,
            modifier = Modifier
                .padding(
                    horizontal = 32.dp
                )
        )
        Spacer(modifier = Modifier.height(128.dp))
    }
}

@Preview(showBackground = false)
@Composable
fun PreviewSuccessRegistrationScreen(){
    YsukampusTheme(dynamicColor = false) {
        SuccessRegistrationScreen(
            user = UserDto(
                id = 1,
                username = "user",
                firstName = "Имя"
            ),
            onOpenScheduleClick = {}
        )
    }
}
