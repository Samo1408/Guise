package com.houvven.guise.xposed

import android.os.Bundle

internal object ProcessControl {
    const val EXTRA_COMMAND = "com.houvven.guise.extra.PROCESS_COMMAND"
    const val COMMAND_EXIT = "EXIT_PROCESS"

    fun isExitRequest(extras: Bundle?): Boolean =
        extras?.getString(EXTRA_COMMAND) == COMMAND_EXIT
}
