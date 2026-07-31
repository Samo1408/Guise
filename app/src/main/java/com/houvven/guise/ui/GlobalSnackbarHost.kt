package com.houvven.guise.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.houvven.guise.ContextAmbient
import com.houvven.guise.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object GlobalSnackbarHost {

    internal val state by derivedStateOf { SnackbarHostState() }
    internal val onError by derivedStateOf { mutableStateOf(false) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @JvmStatic
    fun show(
        message: String,
        actionLabel: String? = null,
        withDismissAction: Boolean = false,
        duration: SnackbarDuration = defaultDuration(actionLabel),
    ) = enqueue(false, false, message, actionLabel, withDismissAction, duration)

    @JvmStatic
    fun showOnError(
        message: String,
        actionLabel: String? = null,
        withDismissAction: Boolean = false,
        duration: SnackbarDuration = defaultDuration(actionLabel),
    ) = enqueue(true, false, message, actionLabel, withDismissAction, duration)

    @JvmStatic
    fun showIfNoShown(
        message: String,
        actionLabel: String? = null,
        withDismissAction: Boolean = false,
        duration: SnackbarDuration = defaultDuration(actionLabel),
    ) {
        if (state.currentSnackbarData == null) show(message, actionLabel, withDismissAction, duration)
    }

    @JvmStatic
    fun showErrorIfNoShown(
        message: String,
        actionLabel: String? = null,
        withDismissAction: Boolean = false,
        duration: SnackbarDuration = defaultDuration(actionLabel),
    ) {
        if (state.currentSnackbarData == null) showOnError(message, actionLabel, withDismissAction, duration)
    }

    @JvmStatic
    fun showByDismissPrevious(
        message: String,
        actionLabel: String? = null,
        withDismissAction: Boolean = false,
        duration: SnackbarDuration = defaultDuration(actionLabel),
    ) = enqueue(false, true, message, actionLabel, withDismissAction, duration)

    @JvmStatic
    fun showOnErrorByDismissPrevious(
        message: String,
        actionLabel: String? = null,
        withDismissAction: Boolean = false,
        duration: SnackbarDuration = defaultDuration(actionLabel),
    ) = enqueue(true, true, message, actionLabel, withDismissAction, duration)

    @JvmStatic
    fun showSuccess() = showByDismissPrevious(
        message = ContextAmbient.current.getString(R.string.operation_successful),
        withDismissAction = true,
        duration = SnackbarDuration.Short,
    )

    private fun enqueue(
        error: Boolean,
        dismissPrevious: Boolean,
        message: String,
        actionLabel: String?,
        withDismissAction: Boolean,
        duration: SnackbarDuration,
    ) {
        scope.launch {
            onError.value = error
            if (dismissPrevious) state.currentSnackbarData?.dismiss()
            state.showSnackbar(message, actionLabel, withDismissAction, duration)
        }
    }

    private fun defaultDuration(actionLabel: String?) =
        if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Indefinite
}
