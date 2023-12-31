package com.android.certifications.test.utils

import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

// These regex comes from https://cs.android.com/android/platform/superproject/+/master:development/tools/bugreport/src/com/android/bugreport/logcat/LogcatParser.java
private val BUFFER_BEGIN_RE = Pattern.compile("--------- beginning of (.*)")
private val LOG_LINE_RE = Pattern.compile(
    "((?:(\\d\\d\\d\\d)-)?(\\d\\d)-(\\d\\d)\\s+(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d\\d)\\s+(\\d+)\\s+(\\d+)\\s+(.)\\s+)(.*?):\\s(.*)",
    Pattern.MULTILINE
)
//private val sinceFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss.SSS")
//    .withZone(ZoneId.systemDefault())

data class LogcatResult(var tag:String,var text:String)

@Suppress("HasPlatformType", "MemberVisibilityCanBePrivate")
sealed class LogLine(val matcher: Matcher) {
    abstract val tag: String
    abstract val text:String
    abstract val date:Calendar
    class BufferLine(rawText: String) : LogLine(BUFFER_BEGIN_RE.matcher(rawText).also { it.find() }) {
        val bufferBegin = matcher.group(1)
        override val tag="[Blank]"
        override val text = bufferBegin
        override val date: Calendar = Calendar.getInstance();
        override fun toString() = "[BufferLine] $bufferBegin"
    }

    class Log(rawText: String, val timeZone: TimeZone) : LogLine(LOG_LINE_RE.matcher(rawText).also { it.find() }) {
        override val date = Calendar.getInstance(timeZone).apply {
            set(Calendar.MONTH, matcher.group(3)!!.toInt() - 1)
            set(Calendar.DAY_OF_MONTH, matcher.group(4)!!.toInt())
            set(Calendar.HOUR_OF_DAY, matcher.group(5)!!.toInt())
            set(Calendar.MINUTE, matcher.group(6)!!.toInt())
            set(Calendar.SECOND, matcher.group(7)!!.toInt())
            set(Calendar.MILLISECOND, matcher.group(8)!!.toInt())
        }

        val pid = matcher.group(9)
        val tid = matcher.group(10)
        val level = matcher.group(11)!![0]
        override val tag = matcher.group(12)
        override val text = matcher.group(13)

        //val instant get() = ZonedDateTime.ofInstant(date.toInstant(), timeZone.toZoneId())

        override fun toString() = "[LogLine] $tag: $text"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Log

            if (date != other.date) return false
            if (pid != other.pid) return false
            if (tid != other.tid) return false
            if (level != other.level) return false
            if (tag != other.tag) return false
            if (text != other.text) return false

            return true
        }

        override fun hashCode(): Int {
            var result = date.hashCode()
            result = 31 * result + (pid?.hashCode() ?: 0)
            result = 31 * result + (tid?.hashCode() ?: 0)
            result = 31 * result + level.hashCode()
            result = 31 * result + (tag?.hashCode() ?: 0)
            result = 31 * result + (text?.hashCode() ?: 0)
            return result
        }
    }

    companion object {
        fun of(rawText: String, timeZone: TimeZone): LogLine? = when {
            BUFFER_BEGIN_RE.matcher(rawText).matches() -> BufferLine(rawText)
            LOG_LINE_RE.matcher(rawText).matches() -> Log(rawText, timeZone)
            else -> null
        }
    }
}