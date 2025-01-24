package niapcert.ui

import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Stable
class LogConsole(val myLogger: MutableStateFlow<String>,
                 val coroutineScope: CoroutineScope,
                 val textStack:MutableList<String> = mutableListOf()
){
    fun clear(){
        coroutineScope.launch {
            textStack.clear();
            myLogger.emit("");
        }
    }
    fun write(line:String){
        coroutineScope.launch {
            textStack.add(line)
            if(textStack.size>500)
                clear()
            myLogger.emit(textStack.joinToString("\r\n"))
        }
    }
}