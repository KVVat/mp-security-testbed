package niapcert.ui

import RootStore
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.android.certifications.test.rule.AdbDeviceRule
import com.android.certifications.test.utils.ShellRequestThread
import com.android.certifications.test.utils.UIServerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logging
import niapcert.viewmodel.AppViewModel

@Composable
fun CommandPanel(viewModel: AppViewModel, uiModel:RootStore){


    lateinit var adb:AdbDeviceRule
    val serverShell by remember { mutableStateOf(ShellRequestThread())  }

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

        val isUiServerRunning = viewModel.uiState.value.isUiServerRunning

        Button(modifier = Modifier.requiredHeight(50.dp), enabled = viewModel.uiState.value.adbIsValid, colors =
        if(isUiServerRunning)
            ButtonDefaults.buttonColors(backgroundColor = Color.Green)
        else ButtonDefaults.buttonColors(backgroundColor = Color.White)
            , onClick = {
                viewModel.viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        //automata start processes
                        adb =AdbDeviceRule()
                        adb.startAlone()
                        if(serverShell.isInitialized()){
                            if(serverShell.isRunning){
                                serverShell.interrupt()
                                viewModel.toggleUiServerIsRunning(false)
                                return@withContext
                            }
                        }
                        viewModel.toggleUiServerIsRunning(true)
                        UIServerManager.runAutomataServer(serverShell,adb)

                        //uiModel.updateUiData()
                    }
                }
            }, content = {
                Text(text = if(isUiServerRunning) "Stop UI Server" else "Start UI Server",
                    fontSize =10.sp)
            }
        )
        Spacer(Modifier.size(6.dp))
        Button(modifier = Modifier.requiredHeight(50.dp), enabled = isUiServerRunning, colors =
        if(isUiServerRunning) ButtonDefaults.buttonColors(backgroundColor = Color.Green)
        else ButtonDefaults.buttonColors(backgroundColor = Color.White)
            , onClick = {
                uiModel.updateUiData()
                logging(uiModel.state.dumpText)
            },content = {
                Text(text = "UI Test", fontSize =10.sp)
            }
        )
    }
}