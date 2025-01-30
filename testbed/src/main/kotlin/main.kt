import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.certifications.junit.JUnitTestRunner
import com.android.certifications.junit.xmlreport.AntXmlRunListener
import com.android.certifications.test.rule.AdbDeviceRule
import com.android.certifications.test.utils.ADSRPTest
import com.android.certifications.test.utils.SFR
import com.android.certifications.test.utils.ShellRequestThread
import com.android.certifications.test.utils.UIServerManager
import com.android.certifications.test.utils.output_path
import niapcert.ui.LabeledCheckBox
import niapcert.ui.LogConsole
import niapcert.ui.LogText
import niapcert.ui.StandardOneButtonDialog
import com.darkrockstudios.libraries.mpfilepicker.DirectoryPicker
import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.exception.RequestRejectedException
import com.malinskiy.adam.interactor.StartAdbInteractor
import com.malinskiy.adam.request.device.ListDevicesRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import niapcert.ui.SettingDialog
import niapcert.ui.SidePanel
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.SimpleFormatter
import java.util.prefs.Preferences

data class AdbProps(val osVersion:String="", val model:String="",val serial:String="", val displayId:String="")

//val logger = Logger.getLogger("TestBed");
lateinit var console: LogConsole;

var antRunner:AntXmlRunListener? = null

fun logging(line: String){
    console.write(line)
    antRunner?.appendSystemOut(line)
}

// manage setting dialogue visibility from outside composable
val flowLogger = MutableStateFlow("")
//val flowVisibleDialog = MutableStateFlow(false)

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

data class PanelUiState(
    val testCases: List<ADSRPTest> = listOf(
        ADSRPTest("FDP_ACF_EXT"),
        ADSRPTest("FPR_PSE1"),
        ADSRPTest("FDP_ACC1"),
        ADSRPTest("KernelAcvpTest"),
        ADSRPTest("FCS_CKH_EXT1"),
        ADSRPTest("FTP_ITC_EXT1"),
        ADSRPTest("MuttonTest"),
    ),
    var isRunning: Boolean = false,
    var visibleDialog:Boolean = false  ,
    val logger: TestBedLogger = TestBedLogger(Logger.getLogger("TestBed")),
    var consoleText: String ="",

)

class AppViewModel(): ViewModel(){
    val _uiState = MutableStateFlow(PanelUiState())
    val uiState = _uiState.asStateFlow()

    val preferences = Preferences.userRoot()
    val settings = PreferencesSettings(preferences)
    //var _settings = MutableStateFlow(settings)
    //var settings = _settings.asStateFlow()

    fun toggleVisibleDialog(b:Boolean) = viewModelScope.launch {
        _uiState.update {
            it.copy(visibleDialog = b)
        }
    }
    fun toggleIsRunning(b:Boolean) = viewModelScope.launch {
        _uiState.update {
            it.copy(isRunning = b)
        }
    }
    fun validateSettings():Boolean{
        val resPath  = settings.getString("PATH_RESOURCE","")
        val outPath  = settings.getString("PATH_OUTPUT","")
        logging("Validating settings. Resource:"+File(resPath).isDirectory+" Output:"+File(outPath).isDirectory)
        return File(resPath).isDirectory && File(outPath).isDirectory
    }
}

@Composable
@Preview
fun App() {

    val viewModel:AppViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val consoleText by flowLogger.collectAsState("")
    val uiModel = remember { RootStore() }
    //settings:Settings
    //val logger = TestBedLogger(Logger.getLogger("TestBed"))

    //val sc by remember { mutableStateOf(settings) }
    var adbProps by remember { mutableStateOf(AdbProps()) }

    val coroutineScope = rememberCoroutineScope()

    var isServerRunning by remember { mutableStateOf(false)  }
    var isTestRunning by remember {mutableStateOf(false)}
    //Behaviour
    //var resourcePath by remember { mutableStateOf(sc.getString("PATH_RESOURCE",""))  }
    //var outputPath by remember { mutableStateOf(sc.getString("PATH_OUTPUT","")) }
    //var useEmbedResource by remember { mutableStateOf(sc.getBoolean("USE_EMBED_RES",true)) }

    val serverShell by remember { mutableStateOf(ShellRequestThread())  }

    //val isSettingOpen by flowVisibleDialog.collectAsState(false)

    console = remember { LogConsole (flowLogger,coroutineScope) }

    var adbIsValid by remember { mutableStateOf(false) }
    lateinit var adb:AdbDeviceRule
    suspend fun observeAdb():Boolean{
        try {
            //adb = AdbDeviceRule()
            val client = adb.adb
            adb.startAlone()
            while(true){
                withContext(Dispatchers.IO) {
                    Thread.sleep(1000)
                }
                if (isTestRunning) continue
                val initialised = adb.isDeviceInitialised()
                try {
                    if (initialised) {
                        if (!adbIsValid) {
                            logging("Device Connected > ${adb.deviceSerial}/${adb.displayId}")
                            adbProps =
                                AdbProps(
                                    adb.osversion,
                                    adb.productmodel,
                                    adb.deviceSerial,
                                    adb.displayId
                                )
                        }
                        adbIsValid = true
                        //Hopefully finish gently but it raises exception..
                        client.execute(ShellCommandRequest("echo"))
                    }
                } catch (anyException: Exception) {
                    if (anyException is RequestRejectedException) {
                        adb.startAlone()
                        continue
                    } else {
                        throw anyException
                    }
                }
            }

        } catch (exception:Exception){
            if(adbIsValid) {
                logging("Device Disconnected > (" + exception.localizedMessage+") #${exception.javaClass.name}")
                adbProps = AdbProps()
                adbIsValid = false
            }
        }

        return true
    }

    fun genTestProps(it:ADSRPTest):Properties {
        val testProps = Properties()
        var sfr = it.clazz.getAnnotation(SFR::class.java)
        if(sfr == null) sfr = SFR()

        testProps.setProperty("SFR.name",sfr.title)
        testProps.setProperty("SFR.shortname",sfr.shortname)
        testProps.setProperty("SFR.description",sfr.description)
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
            //flowVisibleDialog.emit(true)
            viewModel.toggleVisibleDialog(true)
        }
        adb = AdbDeviceRule()
        launch {
            withContext(Dispatchers.IO) {
                while(true){
                    observeAdb()
                }
            }
        }
    }

    MaterialTheme {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.background(Color(0xFFEEEEEE))) {
                Column(modifier = Modifier.fillMaxWidth(fraction = 0.3f)){
                    SidePanel(uiState){ it->
                        if(!adbIsValid){
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
                        Row(modifier = Modifier.padding(4.dp)){
                        Button(modifier = Modifier.requiredSize(50.dp)
                            , onClick = {
                                //isSettingOpen.em = true
                                viewModel.toggleVisibleDialog(true)

                            }, content = {
                                Text("⚙", fontSize =18.sp)
                            }
                        )
                        Spacer(Modifier.size(6.dp))
                        Button(modifier = Modifier.requiredHeight(50.dp), enabled = adbIsValid, colors =
                            if(isServerRunning) ButtonDefaults.buttonColors(backgroundColor = Color.Green)
                                else ButtonDefaults.buttonColors(backgroundColor = Color.White)
                            , onClick = {
                                coroutineScope.launch {
                                    withContext(Dispatchers.IO) {
                                        //automata start processes
                                        adb = AdbDeviceRule()
                                        adb.startAlone()
                                        if(serverShell.isInitialized()){
                                            if(serverShell.isRunning){
                                                serverShell.interrupt()
                                                isServerRunning=false;
                                                return@withContext
                                            }
                                        }
                                        isServerRunning=true
                                        UIServerManager.runAutomataServer(serverShell,adb)
                                    }
                                }
                            }, content = {
                                Text(text = if(isServerRunning) "Stop UI Server" else "Start UI Server",
                                    fontSize =10.sp)
                            }
                        )
                        Spacer(Modifier.size(6.dp))
                        Button(modifier = Modifier.requiredHeight(50.dp), enabled = isServerRunning, colors =
                            if(isServerRunning) ButtonDefaults.buttonColors(backgroundColor = Color.Green)
                                else ButtonDefaults.buttonColors(backgroundColor = Color.White)
                            , onClick = {
                                uiModel.updateUiData()
                                logging(uiModel.state.dumpText)
                            },content = {
                                Text(text = "UI Test",
                                    fontSize =10.sp)
                            }
                        )
                        }
                    }
                }
                //
                SelectionContainer {
                    LogText(consoleText)
                    Text(if(adbIsValid) "🟢" else "🛑", modifier = Modifier.background(Color.Transparent)
                        , color = Color.Black
                    )
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
                    "Settings",
                    onClick = { runBlocking {} }
                )
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
