package io.github.twoloops.weardocuments.tasks

import android.content.Context
import android.os.AsyncTask
import android.os.Looper
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit


class DocumentDeleteTask(private val context: WeakReference<Context>) : AsyncTask<Unit, Unit, Unit>() {

    private val messageClient: MessageClient by lazy(LazyThreadSafetyMode.NONE) {
        val options = Wearable.WearableOptions.Builder().setLooper(Looper.myLooper()).build()
        Wearable.getMessageClient(context.get()!!, options)
    }
    private val capabilityClient: CapabilityClient by lazy(LazyThreadSafetyMode.NONE) {
        val options = Wearable.WearableOptions.Builder().setLooper(Looper.myLooper()).build()
        Wearable.getCapabilityClient(context.get()!!, options)
    }
    var repeatCount = 1

    override fun doInBackground(vararg params: Unit?) {
        try {
            val nodeList = ArrayList<Node>()
            nodeList.addAll(Tasks.await(capabilityClient.getCapability("send_wear_documents", CapabilityClient.FILTER_REACHABLE)).nodes)
            if (nodeList.count() > 0) {
                sendDataWithNode(nodeList[0])
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun uploadFailed(e: Exception? = null) {
        e?.printStackTrace()
        cancel(true)
    }

    private fun sendDataWithNode(node: Node) {
        try {
            val timeBeforeExecution = System.currentTimeMillis()
            var isSuccessful = 0
            val task = messageClient.sendMessage(node.id, "/document", "Deleted".toByteArray())
            task.addOnSuccessListener {
                isSuccessful = 1
            }
            task.addOnFailureListener {
                isSuccessful = -1
            }
            while (isSuccessful < 1) {
                Thread.sleep(100)
                if (isSuccessful == -1 || (System.currentTimeMillis() - timeBeforeExecution) >= TimeUnit.SECONDS.toMillis(10)) {
                    break
                }
            }
            if (isSuccessful != 1 && repeatCount == 0) {
                uploadFailed()
            }
            if (repeatCount > 0 && isSuccessful != 1) {
                repeatCount--
                sendDataWithNode(node)
            }
        } catch (e: Exception) {
            uploadFailed(e)
        }
    }
}