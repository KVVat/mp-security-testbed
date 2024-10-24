package com.android.certifications.test.utils

val TP = "com.android.certifications.test"
data class ADSRPTest(
    val testClass:String
){
    val clazz = Class.forName("$TP.$testClass")

    var sfr =clazz.getAnnotation(SFR::class.java)
    var title="Title";
    var description="Dummy";

    init {
        if(sfr == null){
            sfr =SFR(testClass,"Dummy Description")
        }
        title = sfr.title.trimIndent()
        description = sfr.description.trimIndent()
    }
}