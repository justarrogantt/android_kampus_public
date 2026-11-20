package com.example.ysukampus.feature.registration.ui.registrationScreens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.ysukampus.R
import com.example.ysukampus.feature.registration.ui.components.ShowLogo
import com.example.ysukampus.feature.registration.ui.components.ShowStepHeader
import com.example.ysukampus.ui.theme.YsukampusTheme

@Composable
fun StudentSecondRegistrationScreen(
    ){
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                colorResource(R.color.background_color)
            )
    ) {
        ShowLogo()
        ShowStepHeader(
            step = stringResource(R.string.step_two),
            questionText = stringResource(R.string.second_step_student_question),
            questionDescription = stringResource(R.string.second_step_student_describe)
        )
    }
}

@Preview(showBackground = false)
@Composable
fun previewSecondRegistrationScreen(){
    YsukampusTheme(dynamicColor = false
    ) {
        StudentSecondRegistrationScreen()
    }
}