package io.github.twoloops.weardocuments.views

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.wear.widget.WearableLinearLayoutManager
import android.support.wear.widget.WearableRecyclerView
import android.support.wear.widget.drawer.WearableDrawerView
import android.view.View
import android.widget.TextView
import io.github.twoloops.core.Document
import io.github.twoloops.weardocuments.R
import io.github.twoloops.weardocuments.adapters.DocumentBrowserAdapter
import io.github.twoloops.weardocuments.services.DataService


class DocumentBrowserView : Activity() {

    private val documentsList by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<WearableRecyclerView>(R.id.document_browser_list);
    }
    private val emptyListText by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<TextView>(R.id.document_browser_empty)
    }
    private val deleteToggle: View by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<View>(R.id.document_browser_toggle_delete)
    }
    private val drawer by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<WearableDrawerView>(R.id.document_browser_layout_drawer)
    }
    private var isDeleteMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.document_browser_layout)
        drawer.controller.peekDrawer()
        initializeList()
        deleteToggle.setOnClickListener {
            isDeleteMode = true
            drawer.controller.closeDrawer()
        }
    }

    private fun initializeList() {
        documentsList.isEdgeItemsCenteringEnabled = true
        documentsList.layoutManager = WearableLinearLayoutManager(this)
        val adapter = DocumentBrowserAdapter()
        adapter.itemList = DataService.getInstance(this).loadJson()
        adapter.listener = {
            if (isDeleteMode) {
                if (adapter.itemList?.length() ?: 0 > 0) {
                    val document = Document()
                    document.fromJSONObject(adapter.itemList!!.getJSONObject(it))
                    DataService.getInstance(this).removeFromJson(document.id)
                    DataService.getInstance(this).deleteImages(document)
                    adapter.itemList?.remove(it)
                    adapter.notifyDataSetChanged()
                    isDeleteMode = false
                    if (adapter.itemList?.length() ?: 0 == 0) {
                        emptyListText.visibility = View.VISIBLE
                    }
                }
            } else {
                val intent = Intent()
                intent.putExtra("index", it)
                setResult(RESULT_OK, intent)
                finish()
            }
        }
        if (adapter.itemList!!.length() == 0) {
            emptyListText.visibility = View.VISIBLE
        } else {
            emptyListText.visibility = View.INVISIBLE
        }
        documentsList.adapter = adapter
    }
}