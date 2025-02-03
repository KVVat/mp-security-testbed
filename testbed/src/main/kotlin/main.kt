import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.certifications.junit.JUnitTestRunner
import com.android.certifications.junit.xmlreport.AntXmlRunListener
import com.android.certifications.test.rule.AdbDeviceRule
import com.android.certifications.test.utils.ADSRPTest
import com.android.certifications.test.utils.SFR
import com.android.certifications.test.utils.ShellRequestThread
import com.android.certifications.test.utils.UIServerManager
import com.android.certifications.test.utils.output_path
import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.interactor.StartAdbInteractor
import com.malinskiy.adam.request.device.ListDevicesRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import niapcert.adb.AdbObserver
import niapcert.ui.CommandPanel
import niapcert.ui.LogConsole
import niapcert.ui.LogText
import niapcert.ui.SettingDialog
import niapcert.ui.SidePanel
import niapcert.viewmodel.AppViewModel
import java.io.FileOutputStream
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

lateinit var console: LogConsole;
var antRunner:AntXmlRunListener? = null

class TestBedLogger(logger_:Logger){
    var fileHandler:FileHandler? = null
    val logger = logger_

    fun updateLogger(logoutPath:String){
        if(fileHandler !== null) logger.removeHandler(fileHandler)
        val newFilePath:String
                = Paths.get(logoutPath,"testbed.log").toAbsolutePath().toString();
        logging(newFilePath);
        fileHandler =
            FileHandler(newFilePath, false)

        fileHandler!!.formatter = SimpleFormatter()
        logger.addHandler(fileHandler)
        logger.level = Level.FINE
    }
}

fun logging(line: String){
    console.write(line)
    antRunner?.appendSystemOut(line)
}

@Composable
@Preview
fun App() {

    val viewModel:AppViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val consoleText by viewModel.consoleText.collectAsState()
    val rootStoreModel = remember { RootStore() }

    var adbObserver by remember { mutableStateOf(AdbObserver(viewModel))  }

    val coroutineScope = rememberCoroutineScope()

    var isServerRunning by remember { mutableStateOf(false)  }
    //val isTestRunning by remember { mutableStateOf(false) }
    //For realtime ui dumper

    console = remember { LogConsole (viewModel._consoleText,coroutineScope) }

    fun genTestProps(it:ADSRPTest):Properties {
        val testProps = Properties()
        var sfr = it.clazz.getAnnotation(SFR::class.java)
        if(sfr == null) sfr = SFR()

        testProps.setProperty("SFR.name",sfr.title)
        testProps.setProperty("SFR.shortname",sfr.shortname)
        testProps.setProperty("SFR.description",sfr.description)
        val adbProps = adbObserver.adbProps

        if(!adbProps.osVersion.equals("")){
            testProps.setProperty("device", adbProps.model)
            testProps.setProperty("osversion", adbProps.osVersion)
            testProps.setProperty("system", adbProps.displayId)
            testProps.setProperty("signature", adbProps.serial)
        }
        return testProps
    }

    //Launch Event
    LaunchedEffect(Unit) {
        val settings = viewModel.settings
        val outPath  = settings.getString("PATH_OUTPUT","")
        //logger.updateLogger(outputPath)
        uiState.logger.updateLogger(outPath)
        logging("application launched.")
        if(!viewModel.validateSettings()){
            viewModel.toggleVisibleDialog(true)
        }

        launch {
            withContext(Dispatchers.IO) {
                while(true){
                    adbObserver.observeAdb()
                }
            }
        }
    }
    fun MainPanelSizes():Array<Float>{
        if(uiState.isUiServerRunning){
            return arrayOf(0.5f,0.5f)
        } else {
            return arrayOf(1.0f,0.0f)
        }
    }
    MaterialTheme {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.background(Color(0xFFEEEEEE))) {
                Column(modifier = Modifier.fillMaxWidth(fraction = 0.3f)){
                    SidePanel(uiState){ it->
                        if(!uiState.adbIsValid){
                            logging("*** Need to connect a device to run the test cases.")
                            return@SidePanel;
                        }

                        viewModel.toggleIsRunning(true)
                        console.clear()

                        val timestamp = SimpleDateFormat("yyyyMMddHHmmss").format(Date())
                        val props = genTestProps(it)

                        antRunner =  AntXmlRunListener(::logging, props) {
                            viewModel.toggleIsRunning(false)
                        }
                        antRunner!!.setOutputStream(
                            FileOutputStream(
                                Paths.get(output_path(),"junit-report-${props.getProperty("SFR.shortname")}-$timestamp.xml").toFile())
                        )

                        val runner = JUnitTestRunner(arrayOf(it.clazz),antRunner)
                        runner.start()
                    }
                    //
                    Column(modifier = Modifier.fillMaxSize().background(color = Color.Transparent).padding(10.dp)) {
                        CommandPanel(viewModel,rootStoreModel)
                    }
                }

                //
                SelectionContainer(modifier = Modifier.fillMaxWidth()) {
                    Row{
                        Column(modifier =
                        Modifier.fillMaxWidth(fraction = MainPanelSizes()[0])) {
                            Box(Modifier.padding(4.dp)) {
                                LogText(consoleText)
                                Text(
                                    if (uiState.adbIsValid) "🟢" else "🛑",
                                    modifier = Modifier.background(Color.Transparent),
                                    color = Color.Black
                                )
                            }
                        }
                        //https://stackoverflow.com/questions/68591340/jetpack-compose-periodically-update-and-draw-in-another-thread-as-surfaceview
                        Column(modifier = Modifier.fillMaxWidth()){
                            Box(Modifier.padding(4.dp)){
                                Canvas(
                                    modifier = Modifier.fillMaxSize()
                                ){
                                    if(uiState.isUiServerRunning) {
                                        rootStoreModel.updateUiData()
                                        rootStoreModel.drawUiAccessibilityNodes(this)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    /* Settings Dialogue */
    if(uiState.visibleDialog) {
        SettingDialog(viewModel)
    }
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
                    "Exit",
                    onClick = {
                        exitApplication()
                    }
                )
            }
        }
    }
}
