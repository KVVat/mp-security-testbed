package com.android.certifications.test

import com.android.certifications.test.rule.AdbDeviceRule
import com.android.certifications.test.utils.AdamUtils
import com.android.certifications.test.utils.HostShellHelper
import com.android.certifications.test.utils.PCapHelper
import com.android.certifications.test.utils.SFR
import com.android.certifications.test.utils.TestAssertLogger
import com.android.certifications.test.utils.resource_path
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import kotlinx.coroutines.runBlocking
import logging
import org.dom4j.Document
import org.dom4j.Element
import org.dom4j.Node
import org.dom4j.io.SAXReader
import org.hamcrest.core.IsEqual
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ErrorCollector
import org.junit.rules.TestName
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.absolutePathString

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
    val adb: AdbDeviceRule = AdbDeviceRule()
    private val client: AndroidDebugBridgeClient = adb.adb;

    @Rule @JvmField
    var errs: ErrorCollector = ErrorCollector()
    @Rule @JvmField
    var name: TestName = TestName()
    //Asset Log
    var a: TestAssertLogger = TestAssertLogger(name)

    @Before
    fun setUp()
    {
        runBlocking {
            AdamUtils.InstallApk(file_pcapdroid,true,adb);
            AdamUtils.InstallApk(file_module,true,adb);
        }
    }
    @After
    fun teardown() {
        runBlocking {
            AdamUtils.RemoveApk(file_pcapdroid,adb)
            AdamUtils.RemoveApk(file_module,adb)
        }
    }
    fun isLockScreenEnbled():Boolean{
        var locked = false
        runBlocking {
            val response =
                client.execute(
                    ShellCommandRequest(
                        "dumpsys window | grep mDreamingLockscreen"
                    ), adb.deviceSerial
                )
            if(response != null &&
                response.output.contains("mDreamingLockscreen=true"))
                locked=true
            println(response)
        }
        return locked
    }

    var PKG_PCAPDROID ="com.emanuelef.remote_capture"
    var MDL_PCAPDROID ="pcapdroid.apk"
    private val MDL_TEST = "openurl-debug.apk"
    private val PKG_TEST = "com.example.openurl"

    private val file_module: File =
        File(Paths.get(resource_path(),"FTP_ITC_EXT1",MDL_TEST).toUri())
    private val file_pcapdroid: File =
        File(Paths.get(resource_path(),"FTP_ITC_EXT1",MDL_PCAPDROID).toUri())


    val REQUIRED_CIPHERS_IN_SFR = arrayOf(
        "TLS_RSA_WITH_AES_256_GCM_SHA384",
        "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384")


    @Test
    fun testNormalHost() {
         runBlocking{
            if(isLockScreenEnbled()){
                println("lock screen is enabled. please unlock")
                assert(false)
            }

            val hostName = "https://tls-v1-2.badssl.com:1012/"
            val resp:Pair<String, Path> =
                PCapHelper.tlsCapturePacket(adb,"normal",hostName)
            val httpret:String = resp.first
            errs.checkThat(httpret, IsEqual( "200"))
            logging(httpret)

            val pdml_path  = resp.second.absolutePathString()
            anaylzeCertainPdml(Paths.get(pdml_path+".xml"),hostName)
        }
    }


    //Helper fucntions for analyzing pdml files
    fun Node.attrib(attrib:String="showname"):String{
        val elem = this as Element
        return elem.attributeValue(attrib).toString()
    }
    fun Node.selectChild(key:String): Node?
    {
        //if(this == null) return null;
        return this.selectSingleNode(".//descendant::field[@name='${key}']")
    }
    fun Node.selectChildren(key:String):List<Node>?
    {
        //if(this == null) return null;
        return this.selectNodes(".//descendant::field[@name='${key}']")
    }
    fun Node.packetSerial():Int
    {
        return _value(this.selectSingleNode(
            ".//proto[@name='geninfo']/field[@name='num']")).toInt(16)
    }

    fun _showname(n: Node?):String{
        return n?.attrib() ?: "N/A"
    }
    fun _show(n: Node?):String{
        return n?.attrib("show") ?: "N/A"
    }
    fun _value(n: Node?):String {
        return n?.attrib("value") ?: "0"
    }
   private fun anaylzeCertainPdml(p:Path, targetHost:String)
    {
        //var p:Path = Paths.get("../results/capture/20230615140545-expired.pcap.xml")
        //var targetHost = "https://expired.badssl.com"
        val document: Document = SAXReader().read(File(p.toUri()))
        //Start DNS record check
        //determine entry point of the analyze with dnspackets
        //we should verify the tlspackets after dns query to the target host...
        val dnspkts = document.selectNodes("/pdml/packet/proto[@name='dns']")
        if(dnspkts.size == 0) {
            //there are no dns records...exit
            //errs.checkThat(a.Msg("Need at least one dns packet in cocument"), dnspkts.size,)
            println("need at least one dns packet to continue")
            Assert.assertTrue(false)
            return
        }

        var readAfter = 0
        for(pkt in dnspkts){
            val num = pkt.parent.packetSerial()
            val queryname = _show(pkt.selectChild("dns.qry.name"))
            if(targetHost.contains(queryname)){
                println("Target : $targetHost contains $queryname. we'll examine after this($num) packet.")
                readAfter = num
                break
            }
        }

        //Start TLS record check
        val nodes = document.selectNodes("/pdml/packet/proto[@name='tls']")
        if(nodes.size == 0) {
            //there are no tls records...exit
            println("need at least one tls packet to continue")
            Assert.assertTrue(false)
            return
        }

        var helloLookupDone = false
        var certLookupDone = false
        var certExpire = false
        var certProblemFound = false

        for(tlsp in nodes){
            val records = tlsp.selectChildren("tls.record")//multiple tls.records can be exist in a proto tag
            val serial = tlsp.parent.packetSerial()
            if(serial<readAfter){
                //println("Packet Number:$serial < $readAfter.")
                continue
            } else {
                println("Packet Number=$serial")
            }
            if(records !== null) {
                var i=1
                for (record in records) {
                    println(record.attrib()+"[$serial-$i]")
                    println("\t" + _showname(record.selectChild("tls.record.version")))
                    println("\t" + _showname(record.selectChild("tls.handshake.type")))
                    println("\t" + _showname(record.selectChild("tls.record.content_type")))
                    //tls.record.content_type
                    //println("\t\t>" + _value(record.selectChild("tls.handshake.type")))
                    val hsType = _value(record.selectChild("tls.handshake.type")).toInt(16)
                    val cnType = _value(record.selectChild("tls.record.content_type")).toInt(16)
                    if(hsType>0){
                        //test for client hello
                        if(hsType == 1 && !helloLookupDone){ //Client Hello
                            //test 1: client need to support some certain ciphersuite listed in SFR
                            val ciphers = record.selectChildren("tls.handshake.ciphersuite")
                            if(ciphers !== null){
                                println("\t\ttest 1:ciphers>")
                                val matches:MutableList<String> = mutableListOf()
                                for(c in ciphers) {
                                    println("\t\t\t>"+_showname(c))
                                    val cipherName = _showname(c)
                                    REQUIRED_CIPHERS_IN_SFR.forEach { it->
                                        if(cipherName.indexOf(it)>=0){
                                            matches.add(it)
                                        }
                                    }
                                }
                                //should support one of the ciphersuite listed in SFR:

                                if(matches.size>=1){
                                    println("supported ciphers in SFR requirement:"+matches.toString())
                                } else {
                                    //should assert
                                    // println("found no ciphers which is required to implement.")
                                    errs.checkThat(
                                        a.Msg("Found no ciphers which is required to implement.)"),
                                        true, IsEqual(false)
                                    )
                                }
                            } else {
                                //should assert
                                errs.checkThat(
                                    a.Msg("found no ciphers block in this tls packet"),
                                    true, IsEqual(false)
                                )
                            }
                            //test 2
                            val tlsversions = record.selectChildren("tls.handshake.extensions.supported_version")
                            if(tlsversions !== null){
                                //implement TLS v1.2 [7], TLS v1.3 [11] or higher version of TLS;
                                println("\t\ttest 2:versions>")//0x.0304,0303
                                //var matches:MutableList<String> = mutableListOf()
                                var supported = false
                                for(ver in tlsversions) {
                                    val found = _value(ver).toInt(16)
                                    //println(found)
                                    if(found == 0x0304 || found == 0x0303){
                                        supported = true
                                        break
                                    }
                                }
                                if(supported){
                                    println("The client supports tls v1.2 or later")
                                } else {
                                    errs.checkThat(
                                        a.Msg("Failure : The client does not support tls v1.2 or later)"),
                                        true, IsEqual(false)
                                    )
                                }
                            } else {
                                errs.checkThat(
                                    a.Msg("Failure :  found no tlsversion block in this tls packet)"),
                                    true, IsEqual(false)
                                )
                            }
                            helloLookupDone = true
                        }
                        else if(hsType == 11 && !certLookupDone){ //Certificate
                            //check : x509af.version should be larger than 0x02
                            // val x50
                            val n_ = record.selectChild("x509af.version")
                            if(n_ !== null){
                                val x509afver = _value(n_).toInt(16)
                                println("test3 : x509 auth framework version is 0x$x509afver")
                                if(x509afver>=2){
                                    println("the value indicates version 3 or above ...  okay")
                                } else {
                                    //assert
                                    errs.checkThat(
                                        a.Msg("Failure : x509af version is insuffcient"),
                                        true, IsEqual(false)
                                    )
                                }
                            } else {

                                errs.checkThat(
                                    a.Msg("Failure : found no x509af version block in this tls packet"),
                                    true, IsEqual(false))

                            }
                            ///////////////////////////////////////////////////
                            //Check validity of certificate : expiration date
                            //Packet Note:
                            //  The value was put in the packet like below.
                            //  It's the concatenated ascii codes which represents a date time value.
                            //  3135303431323233353935395a => 1504009000000Z => 2015-04-09-00:00:00Z
                            //  (The letters on hundreds/thousand place of the year are omitted.)
                            //x509af.notBefore > x509af.utcTime
                            //If the value is invalid we can catch an alert packet (content type=21)
                            val nb_ = record.selectChild("x509af.notBefore")
                            val na_ = record.selectChild("x509af.notAfter")
                            if(nb_ !== null && na_ !== null){
                                fun tls_utcdate(input:String): LocalDateTime {
                                    val sb = StringBuffer()
                                    input.chunked(2).forEach {
                                        sb.append(Char( it.toInt(16)))
                                    }
                                    val dtf = DateTimeFormatter.ofPattern("yyMMddHHmmssX")
                                    return LocalDateTime.parse(sb.toString(),dtf)
                                }
                                val nb = tls_utcdate(_value(nb_.selectChild("x509af.utcTime")))
                                val na = tls_utcdate(_value(na_.selectChild("x509af.utcTime")))
                                val now = LocalDateTime.now()
                                println("test4: Cert Expiration check date should not before:$nb notafter:$na")
                                if(now.isAfter(na) || now.isBefore(nb)){
                                    certExpire = true//
                                    certProblemFound = true//The value should be set to false until the end of the test
                                    println("Failure : this cert is expired ")
                                }
                            } else {
                                errs.checkThat(
                                    a.Msg("Failure : found no expiration date records"),
                                    true, IsEqual(false))
                            }
                            certLookupDone = true
                        }
                    }

                    if(cnType == 21 && certExpire == true){ //Alert
                        println("The cert is expired, but connection is gently canceled. okay")
                        certProblemFound = false
                    }

                    i++
                }
            }
        }
        if(certProblemFound){
            errs.checkThat(
                a.Msg("Failure : there were problem in cert record, but connection was not canceled."),
                true, IsEqual(false))
        }
    }
}