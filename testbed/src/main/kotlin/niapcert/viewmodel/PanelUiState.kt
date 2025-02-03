package niapcert.viewmodel

import TestBedLogger
import com.android.certifications.test.utils.ADSRPTest
import java.util.logging.Logger

data class PanelUiState(
    val testCases: List<ADSRPTest> = listOf(
        ADSRPTest("FDP_ACF_EXT"),
        ADSRPTest("FPR_PSE1"),
        ADSRPTest("FDP_ACC1"),
        ADSRPTest("KernelAcvpTest"),
        ADSRPTest("FCS_CKH_EXT1"),
        ADSRPTest("FTP_ITC_EXT1"),
        ADSRPTest("MuttonTest"),
    ),
    var isRunning: Boolean = false,
    var visibleDialog:Boolean = false,
    var adbIsValid:Boolean = false,
    var isUiServerRunning:Boolean = false,
    val logger: TestBedLogger = TestBedLogger(Logger.getLogger("TestBed")),
    var consoleText: String ="",

    )