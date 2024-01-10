package com.android.certifications.test

import com.android.certifications.test.rule.AdbDeviceRule
import com.android.certifications.test.utils.SFR
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@SFR("FTP_ITC_EXT.1/TLS", """
FTP_ITC_EXT.1/TLS
The TSF shall provide a communication channel between itself and another trusted 
IT product that is logically distinct from other communication channels and provides 
assured identification of its end points and protection of the channel data from 
modification or disclosure.
If TLS is supported by the TOE, the TLS channel shall as a minimum:
 implement TLS v1.2 [7], TLS v1.3 [11] or higher version of TLS; 
 and support X.509v3 certificates for mutual(cross) authentication;
 and determine validity of the peer certificate by certificate path, 
 expiration date and revocation status according to IETF RFC 5280 [8]; and./client/build/install/client/bin/hello-world-client
notify the TSF and [selection: not establish the connection, 
request application authorization to establish the connection, no other action] 
if the peer certificate is deemed invalid; 
 and support one of the following ciphersuites: ...
  ""","FTP_ITC_EXT1")
class FTP_ITC_EXT1 {

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