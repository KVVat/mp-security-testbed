import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.grpc.ManagedChannelBuilder

internal class RootStore {

    var state: RootState by mutableStateOf(initialState())
        private set

    private fun initialState(): RootState {
        val port = 9008
        //val host = "localhost"
        //val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
        //val client = UiAutomuttonClient(channel)
        return RootState(port);//,channel,client,null, boundRect{top=0;left=0;right=800;bottom=800},dumpText="")
    }

    data class RootState(
        val port: Int = 9008,
        //val channel:ManagedChannel,
        //val client:UiAutomuttonClient,
       // var uiRootNode: UiAccessibilityNode?,
        //var mDisplaySize: BoundRect,
        //var dumpText:String
    );
}