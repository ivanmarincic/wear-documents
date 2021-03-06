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

package io.github.twoloops.weardocuments.tasks

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.AsyncTask
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.support.v4.app.NotificationCompat
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import io.github.twoloops.core.Document
import io.github.twoloops.weardocuments.R
import io.github.twoloops.weardocuments.dialogs.AlertInfoDialog
import io.github.twoloops.weardocuments.dialogs.NodePickerDialog
import io.github.twoloops.weardocuments.helpers.Utils
import org.apache.commons.io.output.CountingOutputStream
import org.apache.commons.lang3.SerializationUtils
import java.io.ObjectOutputStream
import java.lang.ref.WeakReference
import kotlin.math.roundToInt


class DocumentUploadTask(private val context: WeakReference<Context>) : AsyncTask<Document, Float, Unit>() {

    private val channelClient: ChannelClient by lazy(LazyThreadSafetyMode.NONE) {
        val options = Wearable.WearableOptions.Builder().setLooper(Looper.myLooper()).build()
        Wearable.getChannelClient(context.get()!!, options)
    }
    private val capabilityClient: CapabilityClient by lazy(LazyThreadSafetyMode.NONE) {
        val options = Wearable.WearableOptions.Builder().setLooper(Looper.myLooper()).build()
        Wearable.getCapabilityClient(context.get()!!, options)
    }
    private val notificationId = synchronized(this) { Utils.getNotificationId() }
    private val notificationManager by lazy(LazyThreadSafetyMode.NONE) {
        context.get()!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    private val notificationChannel by lazy(LazyThreadSafetyMode.NONE) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel("wear_documents_upload_progress", "Upload progress", NotificationManager.IMPORTANCE_LOW)
        } else {
            null
        }
    }
    private val cancelBroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent!!.action == cancelIntentAction) {
                this@DocumentUploadTask.cancel(true)
            }
        }
    }
    private val notificationActionCancel by lazy(LazyThreadSafetyMode.NONE) {
        val intent = Intent()
        intent.action = cancelIntentAction
        val cancelPendingIntent = PendingIntent.getBroadcast(context.get()!!, notificationId + 100, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        NotificationCompat.Action.Builder(0, context.get()!!.getString(R.string.notification_button_uploading_cancel), cancelPendingIntent).build()
    }
    private val notificationBuilder by lazy(LazyThreadSafetyMode.NONE) {
        NotificationCompat.Builder(context.get()!!, "wear_documents_upload_progress")
                .setSmallIcon(R.drawable.ic_file_upload)
                .setOngoing(true)
                .setContentTitle(context.get()!!.getString(R.string.notification_title_uploading))
                .addAction(notificationActionCancel)
                .setProgress(100, 0, true)
    }
    private val handler by lazy(LazyThreadSafetyMode.NONE) {
        Handler(Looper.getMainLooper())
    }
    private var progressUpdateRunnable: Runnable? = null
    private var uploadFailed = false
    private var channel: ChannelClient.Channel? = null
    private var objectStream: ObjectOutputStream? = null

    override fun onPreExecute() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(notificationChannel)
        }
        notificationManager.notify(notificationId, notificationBuilder.build())
        val cancelIntentActionFilter = IntentFilter()
        cancelIntentActionFilter.addAction(cancelIntentAction)
        context.get()!!.registerReceiver(cancelBroadcastReceiver, cancelIntentActionFilter)
    }

    private val cancelIntentAction = "${context.get()!!.packageName}.cancel_uploading"

    override fun doInBackground(vararg params: Document?) {
        try {
            val document = params[0]!!
            val nodeList = ArrayList<Node>()
            nodeList.addAll(Tasks.await(capabilityClient.getCapability("get_wear_documents", CapabilityClient.FILTER_REACHABLE)).nodes)
            when {
                nodeList.count() > 1 -> {
                    val dialog = NodePickerDialog(context.get()!!, nodeList)
                    dialog.listener = {
                        sendDataWithNode(it, document)
                    }
                    dialog.show()
                }
                nodeList.count() == 0 -> {
                    AlertInfoDialog.getInstance(context.get()!!).alertError(context.get()!!.getString(R.string.alert_dialog_error_device_missing))
                    uploadFailed = true
                }
                else -> sendDataWithNode(nodeList[0], document)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            uploadFailed = true
            notificationManager.cancel(notificationId)
        }
    }

    override fun onPostExecute(result: Unit?) {
        if (!uploadFailed) {
            handler.removeCallbacks(progressUpdateRunnable)
            notificationBuilder.setOngoing(false)
                    .setProgress(0, 0, false)
                    .setContentTitle(context.get()!!.getString(R.string.notification_title_uploading_finished))
                    .setContentText(context.get()!!.getString(R.string.channel_client_task_file_sent))
            notificationBuilder.mActions.clear()
            notificationManager.notify(notificationId, notificationBuilder.build())
            Looper.getMainLooper().run {
                Toast.makeText(context.get()!!, context.get()!!.getString(R.string.channel_client_task_file_sent), LENGTH_SHORT).show()
            }
        } else {
            notificationBuilder.setOngoing(false)
                    .setProgress(0, 0, false)
                    .setContentTitle(context.get()!!.getString(R.string.notification_title_uploading_failed))
                    .setContentText(null)
            notificationBuilder.mActions.clear()
            notificationManager.notify(notificationId, notificationBuilder.build())
        }
    }

    override fun onProgressUpdate(vararg values: Float?) {
        val currentProgress = values[0]!!
        notificationBuilder.setProgress(100, (currentProgress * 100).roundToInt(), false)
        if (currentProgress >= 1f) {
            notificationManager.cancel(notificationId)
        } else {
            notificationManager.notify(notificationId, notificationBuilder.build())
        }
    }

    override fun onCancelled() {
        try {
            if (uploadFailed) {
                notificationBuilder.setOngoing(false)
                        .setProgress(0, 0, false)
                        .setContentTitle(context.get()!!.getString(R.string.notification_title_uploading_failed))
                        .setContentText(null)
                notificationBuilder.mActions.clear()
                notificationManager.notify(notificationId, notificationBuilder.build())
            } else {
                notificationManager.cancel(notificationId)
            }
            objectStream!!.close()
            channelClient.close(channel!!)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun uploadFailed(e: Exception) {
        uploadFailed = true
        e.printStackTrace()
        cancel(true)
        AlertInfoDialog.getInstance(context.get()!!).alertError(context.get()!!.getString(R.string.alert_dialog_error_device_connection))

    }

    private fun sendDataWithNode(node: Node, data: Document) {
        try {
            data.dataChunkSize = 10
            data.dataChunkStart = 0
            var isUploadingInProgress = true
            val documentConverter = DocumentConverter(data, context)
            documentConverter.listener = {
                try {
                    data.data = it
                    channel = Tasks.await(channelClient.openChannel(node.id, "/document"))
                    val outputStream = Tasks.await(channelClient.getOutputStream(channel!!))
                    if (outputStream != null) {
                        val countingOutputStream = CountingOutputStream(outputStream)
                        objectStream = ObjectOutputStream(countingOutputStream)
                        val objectByteArray = SerializationUtils.serialize(data)
                        val chunkProgress = (data.dataChunkStart) / data.dataCount.toFloat()
                        val chunkValue = (data.dataChunkSize) / data.dataCount.toFloat()
                        progressUpdateRunnable = object : Runnable {
                            override fun run() {
                                try {
                                    if (isCancelled) {
                                        return
                                    }
                                    publishProgress(chunkProgress + (chunkValue * countingOutputStream.byteCount.toFloat() / objectByteArray.size))
                                    handler.postDelayed(this, 500)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    handler.removeCallbacks(this)
                                    uploadFailed(e)
                                }
                            }
                        }
                        handler.post(progressUpdateRunnable)
                        objectStream!!.writeObject(data)
                        objectStream!!.flush()
                        objectStream!!.close()
                        channelClient.close(channel!!)
                        handler.removeCallbacks(progressUpdateRunnable)
                        isUploadingInProgress = documentConverter.nextChunk()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    uploadFailed(e)
                }
            }
            documentConverter.start()
            while (isUploadingInProgress) {
                if (isCancelled) {
                    DocumentDeleteTask(context, {

                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, data)
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            uploadFailed(e)
        }
    }
}