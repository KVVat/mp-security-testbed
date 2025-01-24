package niapcert.ui

import AdbProps
import SidePanelUiState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.certifications.junit.JUnitTestRunner
import com.android.certifications.junit.xmlreport.AntXmlRunListener
import com.android.certifications.test.utils.ADSRPTest
import com.android.certifications.test.utils.SFR
import com.android.certifications.test.utils.output_path
import java.io.FileOutputStream
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SidePanel(uiState:SidePanelUiState, onCardItemClick:(target: ADSRPTest)->Unit){

    LazyColumn( modifier = Modifier.fillMaxHeight(fraction = 0.9f).fillMaxWidth()) {
        items(uiState.testCases) { it ->
            Card(enabled = !uiState.isRunning,
                backgroundColor = if(uiState.isRunning) Color.LightGray else Color.White,
                modifier = Modifier.padding(1.dp).height(120.dp).fillParentMaxWidth()
                    .padding(4.dp),

                onClick = {
                    onCardItemClick(it)
                },
            ) {
                Column {
                    Text(
                        text = it.title,fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(5.dp).fillMaxWidth()
                    )
                    Text(
                        text = it.description, overflow = TextOverflow.Ellipsis,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp, maxLines = 6,
                        modifier = Modifier.padding(5.dp).fillMaxWidth()
                    )
                }
            }
        }
    }
}