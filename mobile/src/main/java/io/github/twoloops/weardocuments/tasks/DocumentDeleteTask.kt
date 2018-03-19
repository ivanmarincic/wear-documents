package io.github.twoloops.weardocuments.tasks

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.AsyncTask
import android.os.Build
import android.os.Looper
import android.support.v4.app.NotificationCompat
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import io.github.twoloops.core.Document
import io.github.twoloops.weardocuments.R
import io.github.twoloops.weardocuments.dialogs.AlertInfoDialog
import io.github.twoloops.weardocuments.dialogs.NodePickerDialog
import io.github.twoloops.weardocuments.helpers.Utils
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit


class DocumentDeleteTask(private val context: WeakReference<Context>, private val listener: (isSuccessful: Boolean) -> Unit) : AsyncTask<Document, Unit, Unit>(), MessageClient.OnMessageReceivedListener {

    private val messageClient: MessageClient by lazy(LazyThreadSafetyMode.NONE) {
        val options = Wearable.WearableOptions.Builder().setLooper(Looper.myLooper()).build()
        Wearable.getMessageClient(context.get()!!, options)
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
            NotificationChannel("wear_documents_delete_progress", "Delete progress", NotificationManager.IMPORTANCE_LOW)
        } else {
            null
        }
    }
    private val notificationBuilder by lazy(LazyThreadSafetyMode.NONE) {
        NotificationCompat.Builder(context.get()!!, "wear_documents_upload_progress")
                .setSmallIcon(R.drawable.ic_delete)
                .setOngoing(true)
                .setContentTitle(context.get()!!.getString(R.string.notification_title_deleting))
                .setProgress(100, 0, true)
    }
    private var deleteFailed = false
    private var isSuccessful = 0

    override fun onPreExecute() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(notificationChannel)
        }
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    override fun doInBackground(vararg params: Document?) {
        try {
            val document = params[0]!!
            document.data = DataConverter(WeakReference(context.get()!!)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, document).get()
            document.dataCount = document.data.count()
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
                    deleteFailed = true
                }
                else -> sendDataWithNode(nodeList[0], document)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            deleteFailed = true
            notificationManager.cancel(notificationId)
        }
    }

    override fun onPostExecute(result: Unit?) {
        if (!deleteFailed) {
            notificationBuilder.setOngoing(false)
                    .setProgress(0, 0, false)
                    .setContentTitle(context.get()!!.getString(R.string.notification_title_deleting_finished))
                    .setContentText(null)
            notificationBuilder.mActions.clear()
            notificationManager.notify(notificationId, notificationBuilder.build())
            Looper.getMainLooper().run {
                Toast.makeText(context.get()!!, context.get()!!.getString(R.string.message_client_task_file_deleted), LENGTH_SHORT).show()
            }
        } else {
            notificationBuilder.setOngoing(false)
                    .setProgress(0, 0, false)
                    .setContentTitle(context.get()!!.getString(R.string.notification_title_deleting_failed))
                    .setContentText(null)
            notificationBuilder.mActions.clear()
            notificationManager.notify(notificationId, notificationBuilder.build())
        }
    }

    override fun onCancelled() {
        try {
            notificationBuilder.setOngoing(false)
                    .setProgress(0, 0, false)
                    .setContentTitle(context.get()!!.getString(R.string.notification_title_deleting_failed))
                    .setContentText(null)
            notificationBuilder.mActions.clear()
            notificationManager.notify(notificationId, notificationBuilder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onMessageReceived(p0: MessageEvent) {
        if (p0.path == "/document" && String(p0.data) == "Deleted") {
            isSuccessful = 1
        }
    }

    private fun deleteFailed(e: Exception? = null) {
        deleteFailed = true
        e?.printStackTrace()
        cancel(true)
        AlertInfoDialog.getInstance(context.get()!!).alertError(context.get()!!.getString(R.string.alert_dialog_error_device_connection))

    }

    private fun sendDataWithNode(node: Node, data: Document) {
        try {
            val timeBeforeExecution = System.currentTimeMillis()
            val task = messageClient.sendMessage(node.id, "/document", data.id.toByteArray())
            task.addOnFailureListener {
                isSuccessful = -1
            }
            messageClient.addListener(this)
            while (isSuccessful < 1) {
                Thread.sleep(100)
                if (isSuccessful == -1 || (System.currentTimeMillis() - timeBeforeExecution) >= TimeUnit.SECONDS.toMillis(30)) {
                    break
                }
            }
            if (isSuccessful == -1 || isSuccessful == 0) {
                deleteFailed()
            }
            listener(isSuccessful == 1)
        } catch (e: Exception) {
            deleteFailed(e)
        }
    }
}