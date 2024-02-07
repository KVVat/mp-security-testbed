package com.android.certifications.test

import com.android.certifications.test.rule.AdbDeviceRule
import com.android.certifications.test.utils.AdamUtils
import com.android.certifications.test.utils.HostShellHelper
import com.android.certifications.test.utils.SFR
import com.android.certifications.test.utils.output_path
import com.android.certifications.test.utils.resource_path
import com.github.uiautomutton.UiamDeviceGrpcKt
import com.google.protobuf.empty
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import io.grpc.ManagedChannel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import logging
import org.junit.Rule
import java.io.Closeable
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.TimeUnit


class MuttonClient(private val channel: ManagedChannel) : Closeable {

    private val stub: UiamDeviceGrpcKt.UiamDeviceCoroutineStub =
        UiamDeviceGrpcKt.UiamDeviceCoroutineStub(channel)

    suspend fun dumpNode()
    {
        println("dump test in")
        val request = empty{  }
        println(stub.getDisplaySize( empty {  }))
        val nodeResponse = stub.getRootNode( empty {  })
        println("Received: ${nodeResponse}")
    }

    override fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}

@SFR("Mutton Test","The test for uiautomutton connect","automutton")
class MuttonTest {

    @Rule
    @JvmField
    val adb: AdbDeviceRule = AdbDeviceRule()

    private val file_server: File =
        File(Paths.get(resource_path(),"muttons","uiserver-release.apk").toUri())
    private val file_instrument: File =
        File(Paths.get(resource_path(),"muttons","uiserver-release-androidTest.apk").toUri())

    @Before
    fun setUp()
    {
        runBlocking {

        }

    }
    @After
    fun teardown() {
        runBlocking {

        }
    }

    @Test
    fun testOutput() {
        runBlocking{

            logging(resource_path())
            logging(output_path())



        }
        logging("ClassName"+this.javaClass.canonicalName);
    }
}