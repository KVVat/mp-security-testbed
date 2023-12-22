package com.android.certifications.test

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import logging

class FCS_CKH_EXT1_HIGH {

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