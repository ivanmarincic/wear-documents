package io.github.twoloops.weardocuments.services

import android.os.AsyncTask
import com.google.android.gms.wearable.*
import io.github.twoloops.core.Document
import io.github.twoloops.weardocuments.tasks.DocumentDeleteTask
import java.io.ObjectInputStream
import java.lang.ref.WeakReference


class ChannelListenerService : WearableListenerService() {

    private val channelClient: ChannelClient by lazy(LazyThreadSafetyMode.NONE) {
        Wearable.getChannelClient(this)
    }

    override fun onChannelOpened(p0: ChannelClient.Channel?) {
        if (p0 != null) {
            getDocument(p0)
        }
    }

    override fun onMessageReceived(p0: MessageEvent?) {
        if (p0 != null) {
            deleteDocument(String(p0.data))
        }
    }

    private fun deleteDocument(id: String) {
        if(DataService.getInstance(applicationContext).removeFromJson(id)){
            DocumentDeleteTask(WeakReference(this)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null)
        }
    }

    private fun getDocument(channel: ChannelClient.Channel) {
        if (channel.path == "/document") {
            channelClient.getInputStream(channel).addOnCompleteListener {
                val inputStream = ObjectInputStream(it.result)
                DataService.getInstance(applicationContext).addToJson(inputStream.readObject() as Document)
            }
        }
    }
}