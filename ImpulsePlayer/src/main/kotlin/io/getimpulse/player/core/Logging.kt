package io.getimpulse.player.core

import android.util.Log

internal object Logging {

    private val ignoreClasses = listOf(
        "VMStack",
        "Thread",
        "Generated_for_debugger_class",
        "Logging",
    )

    fun d(message: String) {
        Thread.currentThread().stackTrace.map { it.className.split('.').last() + "." + it.methodName }
        val element = Thread.currentThread().stackTrace.first {
            val `class` = it.className.split('.').last()
            ignoreClasses.contains(`class`).not()
        }
        val tag = element.className.split('.').last() + "." + element.methodName
        Log.d(tag, message)
    }
}