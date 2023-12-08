import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.rememberTextFieldScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.android.certifications.test.FCS_CKH_EXT1
import com.android.certifications.test.utils.SFR
import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.interactor.StartAdbInteractor
import com.malinskiy.adam.request.device.ListDevicesRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.runner.JUnitCore
import kotlin.math.log

data class TestCase(
    val testClass:String
){
    val clazz = Class.forName(testPackage + "." + testClass)

    var sfr =clazz.getAnnotation(SFR::class.java)
    var title="Title";
    var description="Dummy";

    init {
        if(sfr == null){
            sfr =SFR(testClass,"Dummy Description")
        }
        title = sfr.title.trimIndent()
        description = sfr.description.trimIndent()
    }
}

val testPackage = "com.android.certifications.test"
val testCases = listOf(
    TestCase("FDP_ACF_EXT"),
    TestCase("FPR_PSE1"),
    TestCase("FDP_ACC1"),
    TestCase("KernelAcvpTest"),
    TestCase("FCS_CKH_EXT1"),
    TestCase("OutputTest"),
    )
//Log Utils which supports to record data in textarea from outside of the compose.
@Stable
class Logger(val myLogger:MutableStateFlow<String>,
                  val coroutineScope: CoroutineScope,val loggerText:String,
                  val textStack:MutableList<String> = mutableListOf()
){
    fun clear(){
        coroutineScope.launch {
            textStack.clear();
            myLogger.emit("");
        }
    }
}
lateinit var logger:Logger;
val flowLogger = MutableStateFlow("")
fun logging(line:String){
   logger.coroutineScope.launch {
       logger.textStack.add(line);
       logger.myLogger.emit(logger.textStack.joinToString("\r\n"));

       println(line);
    }
}



@OptIn(ExperimentalTextApi::class,ExperimentalMaterialApi::class)
@Composable
@Preview
fun App() {

    val model = remember { RootStore() }

    val coroutineScope = rememberCoroutineScope()
    val loggerText by flowLogger.collectAsState("")

    logger = remember { Logger(flowLogger,coroutineScope,loggerText) }

    MaterialTheme {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.background(Color(0xFFEEEEEE))) {
                LazyColumn(modifier = Modifier.fillMaxWidth(fraction = 0.3f)) {
                    items(testCases) {
                        Card(
                            modifier = Modifier.padding(1.dp).height(120.dp).fillParentMaxWidth()
                                .padding(4.dp),
                            onClick = {
                                logger.clear()
                                logging("[[${it.title}]]")

                                val clazz = Class.forName(testPackage + "." + it.testClass)
                                val runner = JUnitTestRunner(arrayOf(clazz),
                                    UnitTestingTextListener(::logging))

                                runner.start()
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
                //
                SelectionContainer { LogText(loggerText) }

            }
        }
    }

}
@Composable
fun LogText(text: String) {

    val logState = rememberScrollState(0)

    LaunchedEffect(text) {
        logState.animateScrollTo(logState.maxValue)
    }

    Text(
        text = text, color = Color.Green, fontFamily = FontFamily.Monospace,
        lineHeight = 20.sp, fontSize = 12.sp,
        modifier = Modifier.fillMaxWidth(fraction = 1.0f)
            .fillMaxHeight().background(Color.Black).padding(12.dp)
            .verticalScroll(logState)
    )
}
fun main() = application {
    val state = rememberWindowState(
        position = WindowPosition(Alignment.Center), size = DpSize(1024.dp, 600.dp)
    )
    val windowTitle by remember { mutableStateOf("ADSRP Test Application") }
    Window(title=windowTitle,onCloseRequest = ::exitApplication, undecorated = false, state = state) {
        App()
        MenuBar {
            Menu("File", mnemonic = 'F') {
                Item(
                    "Adb Doctor",
                    onClick = { runBlocking {
                        //Verify the ADB server is running
                        val found = StartAdbInteractor().execute()
                        if (found) {
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
                            } catch (ex: Exception) {
                                println(ex.message)
                            }
                        } else {
                            println("not found")
                        }
                    } }
                )
            }
        }
    }
}
