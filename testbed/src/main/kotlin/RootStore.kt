import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import com.github.uiautomutton.BoundRect
import com.github.uiautomutton.UiAccessibilityNode
import com.github.uiautomutton.boundRect
import com.github.uiautomutton.position
import grpc.UiAutomuttonClient
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.runBlocking

internal class RootStore {

    var state: RootState by mutableStateOf(initialState())
        private set

    private fun initialState(): RootState {
        val port = 9008
        val host = "localhost"
        val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
        val client = UiAutomuttonClient(channel)
        return RootState(port,channel,client,null, boundRect{top=0;left=0;right=800;bottom=800},dumpText="")
    }

    private var dpWidth:Float=0.0f
    private var dpHeight:Float=0.0f
    private var dss = Size(0f,0f)
    @OptIn(ExperimentalTextApi::class)
    fun drawUiAccessibilityNodes(ds: DrawScope, textMeasurer: TextMeasurer,
                                 node: UiAccessibilityNode?=state.uiRootNode){
        with(ds){
            if(node == null) {
                return
            }
            val bn = node.bounds
            dss = ds.size

            val l = (bn.left.toFloat()/dpWidth)*dss.width
            val t = (bn.top.toFloat()/dpHeight)*dss.height
            val b = (bn.bottom.toFloat()/dpHeight)*dss.height
            val r = (bn.right.toFloat()/dpWidth)*dss.width
            //println(">"+dpWidth+","+dpHeight)
            drawRect(color = Color.LightGray,
                topLeft = Offset(l,t), size = Size(r-l,b-t),
                style = Stroke(width = 1.dp.toPx())
            )
            drawText(textMeasurer,text=node.text, topLeft = Offset(l,t),size = Size(r-l,b-t))
            if(node.nodeListList.size>0){
                for(n in node.nodeListList){
                    drawUiAccessibilityNodes(ds, textMeasurer,n)
                }
            }
        }
    }

    suspend fun click(xx: Int, yy: Int) {

        val spx:Float = (xx*dpWidth)/dss.width
        val spy:Float = (yy*dpHeight)/dss.height
        println("Screen Point : $spx,$spy")
        //val dss = ds.size

        state.client.click(position { x=spx.toInt(); y=spy.toInt() })
        state.client.waitForIdle()

        updateUiData()
    }

    fun updateUiData()
    {
        runBlocking {
            try {
                state.uiRootNode = state.client.dumpNode()
                state.mDisplaySize = state.client.getDisplaySize()
                state.dumpText = state.uiRootNode.toString()
                //println(state.mDisplaySize)
                dpHeight = state.mDisplaySize.bottom.toFloat()
                dpWidth = state.mDisplaySize.right.toFloat()
            } catch (ex :Exception){
                println("Exception to dump:"+ex.printStackTrace())
                ex.printStackTrace()
                state.uiRootNode = null
                state.dumpText = "Error : There are no connection to the target device (${ex.message})"
            }
        }
    }



    data class RootState(
        val port: Int = 9008,
        val channel: ManagedChannel,
        val client:UiAutomuttonClient,
        var uiRootNode: UiAccessibilityNode?,
        var mDisplaySize: BoundRect,
        var dumpText:String
    )
}