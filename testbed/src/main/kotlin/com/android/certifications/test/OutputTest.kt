package com.android.certifications.test

import androidx.compose.ui.draw.CacheDrawModifierNode
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import logging

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
        }
        logging("ClassName"+this.javaClass.canonicalName);
    }
}