package grpc

import com.github.uiautomutton.BoundRect
import com.github.uiautomutton.Position
import com.github.uiautomutton.UiAccessibilityNode
import com.github.uiautomutton.UiamDeviceGrpcKt
import com.google.protobuf.empty
import io.grpc.ManagedChannel
import java.io.Closeable
import java.util.concurrent.TimeUnit

class UiAutomuttonClient(private val channel: ManagedChannel) : Closeable {
    private val stub: UiamDeviceGrpcKt.UiamDeviceCoroutineStub =
        UiamDeviceGrpcKt.UiamDeviceCoroutineStub(channel)

    suspend fun waitForIdle(){
        stub.waitForIdle(empty{})
    }
    suspend fun click(pos:Position):Boolean{
        return stub.click(pos).value
    }
    suspend fun dumpNode():UiAccessibilityNode
    {
        return stub.getRootNode(empty {  })
    }

    suspend fun getDisplaySize(): BoundRect
    {
        return stub.getDisplaySize(empty {  })
    }

    override fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}