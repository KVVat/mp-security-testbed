package com.android.certifications.test

import com.android.certifications.test.rule.AdbDeviceRule
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FPR_PSE1 {

    @Rule
    @JvmField
    val adbRule: AdbDeviceRule = AdbDeviceRule()
    private val client: AndroidDebugBridgeClient = adbRule.adb;

    private val TEST_PACKAGE = "com.example.directboot"
    private val TEST_MODULE  = "directboot-debug.apk"

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
    fun testDeviceEncryptedStorage() {
        runBlocking{
            println("${adbRule.deviceSerial}");
            val r = client.execute(
                ShellCommandRequest("ls -la"),
                adbRule.deviceSerial)
            println("${r.stdout}")
        }
        println("Hello Test World!"+this.javaClass.canonicalName);

    }
}