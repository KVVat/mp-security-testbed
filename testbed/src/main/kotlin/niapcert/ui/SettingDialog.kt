package niapcert.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.darkrockstudios.libraries.mpfilepicker.DirectoryPicker
import logging
import niapcert.viewmodel.AppViewModel

@Composable
fun SettingDialog(viewModel: AppViewModel) {
// Path Setting Dialogue Implementation
    var dlgMessage by remember { mutableStateOf("") }
    val settings = viewModel.settings
    var resPath by remember { mutableStateOf(settings.getString("PATH_RESOURCE",""))  }
    var outPath by remember { mutableStateOf(settings.getString("PATH_OUTPUT",""))  }
    var useEmbedRes by remember { mutableStateOf(settings.getBoolean("USE_EMBED_RES",false)) }

    fun onSettingDialogClose() {
        logging("Folder Setting is dismissed")
        dlgMessage=""

        settings.putString("PATH_RESOURCE",resPath)
        settings.putString("PATH_OUTPUT",outPath)
        settings.putBoolean("USE_EMBED_RES",useEmbedRes)

        if(viewModel.validateSettings()){
            viewModel.toggleVisibleDialog(false)
        } else {
            //viewModel.toggleVisibleDialog(false)
            dlgMessage = "⚠️Need to set existent path."
        }
    }

    if(useEmbedRes == true){
        resPath = System.getProperty("compose.application.resources.dir")
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
                resPath=path!!
            } else if(targetDir==1){
                outPath=path!!
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
                    TextField(value=resPath, onValueChange = { resPath=it },
                        enabled=!useEmbedRes, modifier = Modifier.fillMaxWidth(0.85f))
                    Spacer(Modifier.size(6.dp))
                    Button(modifier = Modifier.requiredSize(60.dp)
                        , onClick = {
                            targetDir=0
                            showDirPicker=true
                        }, content = {
                            Text("\uD83D\uDCC1", fontSize =18.sp)
                        }, enabled = !useEmbedRes
                    )
                }
                //
                LabeledCheckBox(checked = useEmbedRes, onCheckedChange = {
                    useEmbedRes=it
                    if(it){
                        resPath = System.getProperty("compose.application.resources.dir")
                    }
                },label="Use Embedded Resources" )
                //
                Text("Output Folder", modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically){
                    TextField(value=outPath, onValueChange = { outPath=it },
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
                Text(dlgMessage, modifier = Modifier.fillMaxWidth())
            }
        }

    }
}