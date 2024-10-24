package com.android.certifications.test.utils

import com.android.certifications.test.rule.AdbDeviceRule
import com.malinskiy.adam.request.forwarding.ListPortForwardsRequest
import com.malinskiy.adam.request.forwarding.LocalTcpPortSpec
import com.malinskiy.adam.request.forwarding.PortForwardRequest
import com.malinskiy.adam.request.forwarding.PortForwardingMode
import com.malinskiy.adam.request.forwarding.PortForwardingRule
import com.malinskiy.adam.request.forwarding.RemoteTcpPortSpec
import com.malinskiy.adam.request.pkg.PmListRequest
import kotlinx.coroutines.delay
import logging
import java.io.File
import java.nio.file.Paths

class UIServerManager {
    companion object {
        suspend fun evalPortForward(adb: AdbDeviceRule):Boolean{
            val rules: List<PortForwardingRule> = adb.adb.execute(
                ListPortForwardsRequest(adb.deviceSerial)
            )
            for(r in rules){
                val param = r.localSpec.toSpec()+":"+r.remoteSpec.toSpec();
                if(param.equals("tcp:9008:tcp:9008"))
                    return true
            }
            return false
        }

        suspend fun evalPackages(verifies:List<String>,adb: AdbDeviceRule):Boolean{
            val packages: List<com.malinskiy.adam.request.pkg.Package> = adb.adb.execute(
                request = PmListRequest(
                    includePath = false
                ),
                adb.deviceSerial
            )
            var found =0;
            run loop@{
                packages.forEach {
                    verifies.forEach { v->
                        if(it.name.equals(v)){
                            found++
                        }
                    }
                    if(found == verifies.size)
                        return@loop//break
                }
            }
            if(found == verifies.size){
                logging("found all prerequisite packages")
                return true
            } else {
                logging("some of prerequisite packages not found")
                return false
            }

        }

        suspend fun runAutomataServer(serverShell:ShellRequestThread,adb: AdbDeviceRule):Boolean{

            val INSTRUMENT_PACKAGE =
                "com.github.uiautomutton.test/androidx.test.runner.AndroidJUnitRunner"
            val file_server: File =
                File(Paths.get(resource_path(),"muttons","uiserver-release.apk").toUri())
            val file_instrument: File =
                File(Paths.get(resource_path(),"muttons","uiserver-release-androidTest.apk").toUri())

            serverShell.setShellCommand("am instrument -w $INSTRUMENT_PACKAGE",adb)
            if(!serverShell.isInitialized()){
                logging("Server is not initialized")
            }
            //Install Packages Here
            val packagesFound =
                evalPackages(listOf("com.github.uiautomutton","com.github.uiautomutton.test"),adb)

            if(!packagesFound){
                AdamUtils.InstallApk(file_server,true,adb);
                delay(500)
                AdamUtils.InstallApk(file_instrument,true,adb);
                delay(500)
            }
            //Check PortForward
            if(!evalPortForward(adb)){
                adb.adb.execute(
                    PortForwardRequest(
                        remote = RemoteTcpPortSpec(9008),
                        local= LocalTcpPortSpec(9008),
                        mode = PortForwardingMode.DEFAULT,
                        serial = adb.deviceSerial
                    )
                )
                delay(500)

                if(!evalPortForward(adb))//If command failed
                    return false
            }
            serverShell.run()
            return true
        }

    }
}