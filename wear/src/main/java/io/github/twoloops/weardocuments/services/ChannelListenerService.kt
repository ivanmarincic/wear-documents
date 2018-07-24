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

package io.github.twoloops.weardocuments.services

import android.os.AsyncTask
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
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
        if (DataService.getInstance(applicationContext).removeFromJson(id)) {
            DocumentDeleteTask(WeakReference(this)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null)
        }
    }

    private fun getDocument(channel: ChannelClient.Channel) {
        if (channel.path == "/document") {
            channelClient.getInputStream(channel).addOnCompleteListener {
                DataService.getInstance(applicationContext).addToJson(ObjectInputStream(it.result))
            }
        }
    }
}