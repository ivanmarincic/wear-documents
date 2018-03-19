package io.github.twoloops.weardocuments.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import com.google.android.gms.wearable.Node
import io.github.twoloops.weardocuments.R
import io.github.twoloops.weardocuments.adapters.NodeListAdapter


class NodePickerDialog(context: Context, private var nodeItems: ArrayList<Node>) : Dialog(context) {

    private val nodeListView by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<RecyclerView>(R.id.node_picker_dialog_list)
    }
    var listener: ((item: Node) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.node_picker_dialog)
        val adapter = NodeListAdapter()
        adapter.itemList = nodeItems
        adapter.listener = { item ->
            listener?.invoke(item)
        }
        nodeListView.adapter = adapter
        nodeListView.layoutManager = LinearLayoutManager(context)
    }

}