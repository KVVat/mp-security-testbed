package niapcert.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.russhwolf.settings.PreferencesSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logging
import java.io.File
import java.util.prefs.Preferences

class AppViewModel(): ViewModel(){
    val _uiState = MutableStateFlow(PanelUiState())
    val uiState = _uiState.asStateFlow()

    val _consoleText = MutableStateFlow("")
    val consoleText = _consoleText.asStateFlow()

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

    fun toggleUiServerIsRunning(b:Boolean) = viewModelScope.launch {
        _uiState.update {
            it.copy(isUiServerRunning = b)
        }
    }

    fun toggleAdbIsValid(b:Boolean) = viewModelScope.launch {
        _uiState.update {
            it.copy(adbIsValid = b)
        }
    }

    fun validateSettings():Boolean{
        val resPath  = settings.getString("PATH_RESOURCE","")
        val outPath  = settings.getString("PATH_OUTPUT","")
        logging("Validating settings. Resource:"+ File(resPath).isDirectory+" Output:"+ File(outPath).isDirectory)
        return File(resPath).isDirectory && File(outPath).isDirectory
    }
}