package io.getimpulse.player.util

import android.util.Log

internal object Logging {

    private val ignoreClasses = listOf(
        "VMStack",
        "Thread",
        "Generated_for_debugger_class",
        "Logging",
    )

    fun d(message: String) {
        Log.d(getTag(), message)
    }

    fun w(message: String) {
        Log.w(getTag(), message)
    }

    fun e(message: String) {
        Log.e(getTag(), message)
    }

    private fun getTag(): String {
        Thread.currentThread().stackTrace.map { it.className.split('.').last() + "." + it.methodName }
        val element = Thread.currentThread().stackTrace.first {
            val `class` = it.className.split('.').last()
            ignoreClasses.contains(`class`).not()
        }
        return element.className.split('.').last() + "." + element.methodName
    }
}