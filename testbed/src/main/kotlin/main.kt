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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
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
import com.android.certifications.junit.JUnitTestRunner
import com.android.certifications.junit.xmlreport.AntXmlRunListener
import com.android.certifications.test.rule.AdbDeviceRule
import com.android.certifications.test.utils.ADSRPTest
import com.android.certifications.test.utils.SFR
import com.android.certifications.test.utils.ShellRequestThread
import com.android.certifications.test.utils.UIServerManager
import com.android.certifications.test.utils.output_path
import com.android.certifications.ui.LabeledCheckBox
import com.android.certifications.ui.LogConsole
import com.android.certifications.ui.LogText
import com.android.certifications.ui.StandardOneButtonDialog
import com.darkrockstudios.libraries.mpfilepicker.DirectoryPicker
import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.exception.RequestRejectedException
import com.malinskiy.adam.interactor.StartAdbInteractor
import com.malinskiy.adam.request.device.ListDevicesRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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


val testCases = listOf(
    ADSRPTest("FDP_ACF_EXT"),
    ADSRPTest("FPR_PSE1"),
    ADSRPTest("FDP_ACC1"),
    ADSRPTest("KernelAcvpTest"),
    ADSRPTest("FCS_CKH_EXT1"),
    ADSRPTest("FTP_ITC_EXT1"),
    ADSRPTest("MuttonTest"),
)


data class AdbProps(val osVersion:String, val model:String,val serial:String, val displayId:String)


//val logger = Logger.getLogger("TestBed");
lateinit var console: LogConsole;

val flowLogger = MutableStateFlow("")
var antRunner:AntXmlRunListener? = null
fun logging(line: String){
    console.write(line)
    antRunner?.appendSystemOut(line)
}

// manage setting dialogue visibility from outside composable
val flowVisibleDialog = MutableStateFlow(false)

@OptIn(ExperimentalTextApi::class,ExperimentalMaterialApi::class)
@Composable
@Preview
fun App(settings: Settings) {

    val logger = Logger.getLogger("TestBed")

    val sc by remember { mutableStateOf(settings) }
    var ap by remember { mutableStateOf(AdbProps("","","","")) }

    val coroutineScope = rememberCoroutineScope()
    val loggerText by flowLogger.collectAsState("")
    var isTestRunning by remember { mutableStateOf(false) }
    var isServerRunning by remember { mutableStateOf(false)  }

    //Behaviour
    var resourcePath by remember { mutableStateOf(sc.getString("PATH_RESOURCE",""))  }
    var outputPath by remember { mutableStateOf(sc.getString("PATH_OUTPUT","")) }
    var useEmbedResource by remember { mutableStateOf(sc.getBoolean("USE_EMBED_RES",true)) }

    var fileHandler:FileHandler? = null
    val serverShell by remember { mutableStateOf(ShellRequestThread())  }
    val model = remember { RootStore() }

    fun validateSettings():Boolean{
        logging("Validating settings. Resource:"+File(resourcePath).isDirectory+" Output:"+File(outputPath).isDirectory)

        return File(resourcePath).isDirectory && File(outputPath).isDirectory
    }

    fun updateLogger(){
        if(fileHandler !== null) logger.removeHandler(fileHandler)
        var newfilepath:String
            = Paths.get(outputPath,"testbed.log").toAbsolutePath().toString();
        logging(newfilepath);
        fileHandler =
            FileHandler(newfilepath, false)

        fileHandler!!.formatter = SimpleFormatter()
        //java.util.logging.SimpleFormatter.format="%4$s: %5$s [%1$tc]%n"
        logger.addHandler(fileHandler)
        logger.level = Level.FINE
    }
    val isSettingOpen by flowVisibleDialog.collectAsState(false)

    console = remember { LogConsole (flowLogger,coroutineScope,loggerText) }
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
                            ap =
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
                ap = AdbProps("", "", "", "")
                adbIsValid = false
            }
        }

        return true
    }

    //Launch Event
    LaunchedEffect(Unit) {
        updateLogger()
        logging("application launched.")
        if(!validateSettings()){
            //isSettingOpen = false
            flowVisibleDialog.emit(true)
        }
        adb = AdbDeviceRule()
        //text = String(resource("welcome.txt").readBytes())
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
                    LazyColumn( modifier = Modifier.fillMaxHeight(fraction = 0.9f).fillMaxWidth()) {
                        items(testCases) {
                            Card(enabled = !isTestRunning,
                                backgroundColor = if(isTestRunning) Color.LightGray else Color.White,
                                modifier = Modifier.padding(1.dp).height(120.dp).fillParentMaxWidth()
                                    .padding(4.dp),
                                onClick = {

                                    if(!adbIsValid){
                                        logging("*** Need to connect a device to run the test cases.")
                                        return@Card;
                                    }

                                    isTestRunning = true;
                                    console.clear()
                                    logging("[[${it.title}]]")

                                    var sfr = it.clazz.getAnnotation(SFR::class.java)
                                    if(sfr == null){
                                        sfr = SFR("title","description","shortname")
                                    }

                                    val testProps = Properties()
                                    testProps.setProperty("SFR.name",sfr.title)
                                    testProps.setProperty("SFR.description",sfr.description)
                                    if(!ap.osVersion.equals("")){
                                      testProps.setProperty("device", ap.model)
                                      testProps.setProperty("osversion", ap.osVersion)
                                      testProps.setProperty("system", ap.displayId)
                                      testProps.setProperty("signature", ap.serial)
                                    }
                                    //
                                    //adb.osversion,adb.productmodel,adb.deviceSerial,adb.displayId
                                    antRunner =  AntXmlRunListener(::logging, testProps) {
                                        isTestRunning = false;
                                    }
                                    val now = SimpleDateFormat("yyyyMMddHHmmss").format(Date())
                                    antRunner!!.setOutputStream(
                                        FileOutputStream(
                                        Paths.get(output_path(),"junit-report-${sfr.shortname}-$now.xml").toFile())
                                    )

                                    val runner = JUnitTestRunner(arrayOf(it.clazz),antRunner)
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
                    Column(modifier = Modifier.fillMaxSize().background(color = Color.Transparent).padding(10.dp)) {
                        Row(modifier = Modifier.padding(4.dp)){
                        Button(modifier = Modifier.requiredSize(50.dp)
                            , onClick = {
                                //isSettingOpen.em = true
                                coroutineScope.launch {
                                    flowVisibleDialog.emit(true);
                                }
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
                                model.updateUiData()
                                logging(model.state.dumpText)
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
                    LogText(loggerText)
                    Text(if(adbIsValid) "🟢" else "🛑", modifier = Modifier.background(Color.Transparent)
                        , color = Color.Black
                    )
                }
            }
        }
    }

    if(isSettingOpen) {
        // Path Setting Dialogue Implementation

        var dlgmessage by remember { mutableStateOf("") }
        fun onSettingDialogClose() {
            logging("Folder Setting is dismissed")
            dlgmessage=""
            if(validateSettings()) {
                //settingConfig.["PATH_OUTPUT"]=outputPath
                coroutineScope.launch {
                    flowVisibleDialog.emit(false)
                    //persist values to property file
                    settings.putString("PATH_RESOURCE",resourcePath)
                    settings.putString("PATH_OUTPUT",outputPath)
                    settings.putBoolean("USE_EMBED_RES",useEmbedResource)
                }
            } else {
                dlgmessage = "⚠️Need to set existent path."
            }
        }

        Dialog(
            onDismissRequest = {
                onSettingDialogClose()
            },
        ){
            var showDirPicker by remember { mutableStateOf(false) }
            var targetDir by remember { mutableStateOf(0) }

            DirectoryPicker(showDirPicker) { path ->
                showDirPicker = false
                // do something with path
                if(targetDir==0){
                    resourcePath=path!!
                } else if(targetDir==1){
                    outputPath=path!!
                }
            }

            StandardOneButtonDialog("Folder Settings","OK",
                onCloseRequest = {
                    onSettingDialogClose()
                }){
                Column {
                    Text("Resource Folder", modifier = Modifier.fillMaxWidth())
                    //
                    Row(verticalAlignment = Alignment.CenterVertically){
                        TextField(value=resourcePath, onValueChange = { resourcePath=it },
                            enabled=!useEmbedResource, modifier = Modifier.fillMaxWidth(0.85f))
                        Spacer(Modifier.size(6.dp))
                        Button(modifier = Modifier.requiredSize(60.dp)
                            , onClick = {
                                targetDir=0
                                showDirPicker=true
                            }, content = {
                                Text("\uD83D\uDCC1", fontSize =18.sp)
                            }, enabled = !useEmbedResource
                        )
                    }
                    //
                    LabeledCheckBox(checked = useEmbedResource, onCheckedChange = {
                        useEmbedResource=it
                        if(it){
                            resourcePath = System.getProperty("compose.application.resources.dir")
                        }
                    },label="Use Embedded Resources" )
                    //
                    Text("Output Folder", modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically){
                        TextField(value=outputPath, onValueChange = { outputPath=it },
                            enabled=true, modifier = Modifier.fillMaxWidth(0.85f))
                        Spacer(Modifier.size(6.dp))
                        Button(modifier = Modifier.requiredSize(60.dp)
                            , onClick = {
                                targetDir=1
                                showDirPicker=true
                            }, content = {
                                Text("\uD83D\uDCC1", fontSize =18.sp)
                            }
                        )
                    }
                    Text(dlgmessage, modifier = Modifier.fillMaxWidth())
                }
            }
            if(useEmbedResource == true){
                resourcePath = System.getProperty("compose.application.resources.dir")
            }
        }
    }
}







fun main() = application {

    val state = rememberWindowState(
        position = WindowPosition(Alignment.Center), size = DpSize(1024.dp, 600.dp)
    )
    val windowTitle by remember { mutableStateOf("ADSRP Test Application") }

    val preferences = Preferences.userRoot()
    val settings = PreferencesSettings(preferences)


    Window(title=windowTitle,onCloseRequest = ::exitApplication, undecorated = false, state = state) {
        App(settings)
        MenuBar {
            Menu("File", mnemonic = 'F') {
                Item(
                    "Settings",
                    onClick = { runBlocking {
                        //Verify the ADB server is running
                    } }

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
