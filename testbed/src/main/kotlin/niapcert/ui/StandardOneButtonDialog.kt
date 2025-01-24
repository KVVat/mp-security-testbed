package niapcert.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StandardOneButtonDialog(title:String="Title",
                            button1Text:String="Apply",
                            onCloseRequest:()->Unit,
                            content: @Composable () -> Unit){
    Card {
        Column(modifier = Modifier.background(Color.White)) {
            Row(Modifier.background(Color.White).padding(10.dp)) {
                Text(
                    title, color = Color.Black,
                    fontSize = 20.sp, fontWeight = FontWeight.Bold
                )
            }
            Row(modifier = Modifier.fillMaxWidth().padding(start=10.dp, end = 10.dp, bottom = 10.dp)) {
                content()
            }
            Row(modifier = Modifier.align(Alignment.End).padding(10.dp)) {
                Button(onClick = {
                    onCloseRequest()
                }) {
                    Text(button1Text)
                }
            }
        }
    }
}