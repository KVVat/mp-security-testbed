import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.android.certifications.test.FCS_CKH_EXT1
import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.interactor.StartAdbInteractor
import com.malinskiy.adam.request.device.ListDevicesRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.runner.JUnitCore


data class TestCase(
    val testName:String,
    val testClass:String
)

val testPackage = "com.android.certifications.test"
val testCases = listOf(
    TestCase("FCS_ACF_EXT","FCS_ACF_EXT"),
    TestCase("FCS_CKH_EXT1","FCS_CKH_EXT1"),
    TestCase("FPR_PSE1","FPR_PSE1"),
    TestCase("FTP_ITC_EXT1","FTP_ITC_EXT1"),
)

@OptIn(ExperimentalMaterialApi::class)
@Composable
@Preview
fun App() {
    val model = remember { RootStore() }
    val items_ = (1..30).map { "Item $it" }
    MaterialTheme {

        Column(Modifier.fillMaxSize()) {
            Row(Modifier.background(Color(0xFFEEEEEE))) {

                LazyColumn {
                  items(testCases){
                      Card(modifier = Modifier.padding(1.dp).height(70.dp).fillMaxWidth(fraction=0.8f).padding(4.dp)
                              ,onClick = {

                              println("CardExample: Card Click ${it.testName}")

                              var clazz = Class.forName(testPackage+"."+it.testClass);

                              val result = JUnitCore.runClasses(clazz)

                             },
                      ) {
                          Column (modifier = Modifier.padding(1.dp)){
                              Text(text = it.testName,
                                  fontSize = 20.sp,
                                  modifier = Modifier.padding(start = 20.dp))
                              
                              LinearProgressIndicator(color = Color.Green)
                          }
                      }
                  }
                }
            }
        }
    }
}

fun LinearProgressIndicator(color: Color, modifier: Modifier, strokeCap: () -> Unit) {

}

fun main() = application {
    val state = rememberWindowState(
        position = WindowPosition(Alignment.Center), size = DpSize(800.dp,600.dp)
    )
    Window(onCloseRequest = ::exitApplication, undecorated = false, state = state) {
        App()
        MenuBar {
            Menu("File", mnemonic = 'F') {
                Item(
                    "Try Action A",
                    onClick = {
                        runBlocking {
                            println("Run JUnit Test")
                            val result = JUnitCore.runClasses(FCS_CKH_EXT1::class.java)

                        }
                    },
                )
                Item(
                    "Adb Doctor",
                    onClick = {
                        runBlocking {
                            println("Echo!")
                            runBlocking {
                                //Verify the ADB server is running
                                val found = StartAdbInteractor().execute()
                                if(found) {
                                    //Create AndroidDebugBridgeServer instance
                                    val adb = AndroidDebugBridgeClientFactory().apply {
                                        coroutineContext = Dispatchers.IO
                                    }.build()
                                    //Execute requests using suspendable execute() methods. First list available devices

                                    val devices = adb.execute(request = ListDevicesRequest())
                                    try {
                                        val serial = devices.first().serial
                                        //Execute an actual command specifying serial number of device
                                        val output = adb.execute(
                                            ShellCommandRequest("echo hello"),
                                            serial = serial
                                        )
                                        println(output.output)
                                    } catch (ex:Exception){
                                        println(ex.message)
                                    }
                                } else {
                                    println("notfound")
                                }
                            }

                        }

                    }
                )
            }
        }
    }


}