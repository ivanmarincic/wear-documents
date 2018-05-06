package io.github.twoloops.weardocuments.dialogs

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import io.github.twoloops.core.SingletonHolder
import io.github.twoloops.weardocuments.R


class AlertInfoDialog(private val context: Context) {

    fun alertError(message: String) {
        if(context is Activity && context.isFinishing)
            return
        val dialogBuilder = android.support.v7.app.AlertDialog.Builder(context)
        dialogBuilder.setTitle(context.resources.getString(R.string.alert_dialog_title_error))
        dialogBuilder.setMessage(message)
        dialogBuilder.setPositiveButton(context.resources.getString(R.string.alert_dialog_ok), { _, _ ->
        })
        Handler(Looper.getMainLooper()).post({
            dialogBuilder.create().show()
        })
    }

    fun alertDeleteAnyway(listener: () -> Unit) {
        if(context is Activity && context.isFinishing)
            return
        val dialogBuilder = android.support.v7.app.AlertDialog.Builder(context)
        dialogBuilder.setTitle(context.resources.getString(R.string.alert_dialog_title_error))
        dialogBuilder.setMessage(context.resources.getString(R.string.alert_dialog_error_deleting_file))
        dialogBuilder.setPositiveButton(context.resources.getString(R.string.alert_dialog_ok), { _, _ ->
            listener()
        })
        dialogBuilder.setNegativeButton(context.resources.getString(R.string.alert_dialog_cancel), { dialog, _ ->
            dialog.cancel()
        })
        Handler(Looper.getMainLooper()).post({
            dialogBuilder.create().show()

        })
    }

    companion object : SingletonHolder<AlertInfoDialog, Context>(::AlertInfoDialog)
}