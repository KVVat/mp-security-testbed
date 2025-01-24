package niapcert.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LogText(text: String) {

    val logState = rememberScrollState(0)

    LaunchedEffect(text) {
        logState.animateScrollTo(logState.maxValue)
    }

    //Can change colors each lines?
    //https://stackoverflow.com/questions/72832802/how-to-show-multiple-color-text-in-same-text-view-with-jetpack-compose

    Text(
        text = text, color = Color.Green, fontFamily = FontFamily.Monospace,
        lineHeight = 20.sp, fontSize = 12.sp,
        modifier = Modifier.fillMaxWidth(fraction = 1.0f)
            .fillMaxHeight().background(Color.Black).padding(12.dp)
            .verticalScroll(logState)
    )
}
