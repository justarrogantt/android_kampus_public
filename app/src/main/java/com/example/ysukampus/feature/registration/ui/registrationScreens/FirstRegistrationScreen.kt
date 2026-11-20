package com.example.ysukampus.feature.registration.ui.registrationScreens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ysukampus.ui.theme.YsukampusTheme
import com.example.ysukampus.R
import com.example.ysukampus.feature.registration.ui.components.PrimaryActionButton
import com.example.ysukampus.feature.registration.ui.components.RoleCard
import com.example.ysukampus.feature.registration.ui.components.ShowLogo
import com.example.ysukampus.feature.registration.ui.components.ShowStepFooter
import com.example.ysukampus.feature.registration.ui.components.ShowStepHeader
import com.example.ysukampus.feature.registration.ui.model.RoleOption


@Composable
fun FirstRegistrationScreen() {

    var selectedRole by remember { mutableStateOf<RoleOption?>(null) }

//    var currentStep by remember { mutableStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                colorResource(R.color.background_color)
            )
    ) {
        ShowLogo()
        ShowStepHeader(
            step = stringResource(R.string.step_one),
            questionText = stringResource(R.string.first_step_question),
            questionDescription = stringResource(R.string.first_step_describe)
        )

        Column(modifier = Modifier.padding(
            horizontal = 16.dp,
            vertical = 32.dp
        )
        ) {
            RoleCard(
                title = stringResource(R.string.role_student),
                description = stringResource(R.string.role_student_description),
                iconRes = R.drawable.school_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
                selected = selectedRole == RoleOption.STUDENT,
                onClick = { selectedRole = RoleOption.STUDENT },
            )

            Spacer(modifier = Modifier.padding(top = 16.dp))

            RoleCard(
                title = stringResource(R.string.role_tutor),
                description = stringResource(R.string.role_tutor_description),
                iconRes = R.drawable.school_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
                selected = selectedRole == RoleOption.TEACHER,
                onClick = { selectedRole = RoleOption.TEACHER }
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(modifier = Modifier.padding(start = 8.dp)) {
                ShowStepFooter(
                    modifier = Modifier.offset(x = 16.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            PrimaryActionButton(
                text = stringResource(R.string.step_one_button_text),
                gradientStartColor = R.color.second_text_color,
                gradientEndColor = R.color.fade_end,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = false)
@Composable
fun PreviewRegistrationScreen(){
    YsukampusTheme(dynamicColor = false) {
        FirstRegistrationScreen()
    }

}
