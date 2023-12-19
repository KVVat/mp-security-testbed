package com.android.certifications.test

import androidx.compose.ui.draw.CacheDrawModifierNode
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import logging
import java.io.File
import java.nio.file.Paths

class OutputTest {

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

            //val databasePath =
            val resourcesDir = File(System.getProperty("compose.application.resources.dir"))
            //working directory
            val workingpath = Paths.get("").toAbsolutePath().toString()
            logging("Working dir = $workingpath");


            //Call Class.getResource? to read the files in jar file
            logging(resourcesDir.resolve("aaa.txt").readText())

            logging(System.getProperty("compose.application.resources.dir"));
            val properites = System.getProperties()
            properites.entries.forEach{
                logging("${it.key}=${it.value}")
            }
            //File(System.getProperty("compose.application.resources.dir")).listFiles()?.forEach {
            //    logging(it.absolutePath)
            //}

            //logging("logFilePath="+logFile.absolutePath)

        }
        logging("ClassName"+this.javaClass.canonicalName);
    }
}