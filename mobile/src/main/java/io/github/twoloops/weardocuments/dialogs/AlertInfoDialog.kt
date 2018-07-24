/*
 * MIT License
 *
 * Copyright (c) 2018 Ivan Marinčić
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.twoloops.weardocuments.dialogs

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import io.github.twoloops.core.SingletonHolder
import io.github.twoloops.weardocuments.R


class AlertInfoDialog(private val context: Context) {

    fun alertError(message: String) {
        if (context is Activity && context.isFinishing)
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
        if (context is Activity && context.isFinishing)
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

    fun deleteOnThisDeviceOnly(listener: (thisDeviceOnly: Boolean) -> Unit) {
        if (context is Activity && context.isFinishing)
            return
        val dialogBuilder = android.support.v7.app.AlertDialog.Builder(context)
        dialogBuilder.setTitle(context.resources.getString(R.string.alert_dialog_title_delete_only))
        dialogBuilder.setMessage(context.resources.getString(R.string.alert_dialog_description_delete_only))
        dialogBuilder.setPositiveButton(context.resources.getString(R.string.alert_dialog_this_only), { _, _ ->
            listener(true)
        })
        dialogBuilder.setNegativeButton(context.resources.getString(R.string.alert_dialog_no_both), { dialog, _ ->
            listener(false)
            dialog.cancel()
        })
        Handler(Looper.getMainLooper()).post({
            dialogBuilder.create().show()

        })
    }

    companion object : SingletonHolder<AlertInfoDialog, Context>(::AlertInfoDialog)
}