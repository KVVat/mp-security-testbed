package com.android.certifications.test

import androidx.compose.ui.draw.CacheDrawModifierNode
import com.android.certifications.test.utils.SFR
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import logging
import java.io.File
import java.nio.file.Paths
import java.util.prefs.Preferences


@SFR("System Test","The test for debug and development purpose")
class OutputTest {


    lateinit var settings: Settings;// = TODO();
    @Before
    fun setUp()
    {
        runBlocking {
            settings = PreferencesSettings(Preferences.userRoot())
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

            val r = settings.getString("PATH_RESOURCE","PATH_RESOURCE")
            val w = settings.getString("PATH_OUTPUT","PATH_OUTPUT")
            logging(r)
            logging(w)

            //val databasePath =
            val resourcesDir = File(System.getProperty("compose.application.resources.dir"))
            //working directory
            val workingpath = Paths.get("").toAbsolutePath().toString()
            logging("Working dir = $workingpath");
            //
            logging(resourcesDir.resolve("aaa.txt").readText())
            //
            logging(System.getProperty("compose.application.resources.dir"));
            val properites = System.getProperties()
            properites.entries.forEach{
                logging("${it.key}=${it.value}")
            }

        }
        logging("ClassName"+this.javaClass.canonicalName);
    }
}