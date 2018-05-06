package io.github.twoloops.weardocuments.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.view.View
import android.view.ViewGroup
import android.widget.*
import io.github.twoloops.core.Document
import io.github.twoloops.core.SerializableBitmap
import io.github.twoloops.weardocuments.R
import io.github.twoloops.weardocuments.helpers.Utils
import io.github.twoloops.weardocuments.tasks.DocumentConverter
import java.lang.ref.WeakReference


class DocumentPreviewDialog(context: Context, var item: Document) : Dialog(context) {

    private val saveButton by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<Button>(R.id.document_preview_dialog_save_button)
    }
    private val cancelButton by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<Button>(R.id.document_preview_dialog_cancel_button)
    }
    private val editButton by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<Button>(R.id.document_preview_dialog_edit_button)
    }
    private val deleteButton by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<Button>(R.id.document_preview_dialog_delete_button)
    }
    private val renameButton by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<Button>(R.id.document_preview_dialog_rename_button)
    }
    private val titleView by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<TextView>(R.id.document_preview_dialog_toolbar_title)
    }
    private val iconView by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<ImageView>(R.id.document_preview_dialog_toolbar_icon)
    }
    private val dataView by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<ListView>(R.id.document_preview_dialog_data)
    }
    private val progressBar by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<ProgressBar>(R.id.document_preview_dialog_progress_bar)
    }
    private val handler by lazy(LazyThreadSafetyMode.NONE) {
        Handler(Looper.getMainLooper())
    }
    private lateinit var documentConverter: DocumentConverter
    private var pdfRenderer: PdfRenderer? = null

    var deleteListener: ((document: Document) -> Unit)? = null
    var saveListener: ((document: Document) -> Unit)? = null
    var editListener: ((document: Document) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.document_preview_dialog)
        initializeToolbar()
        initializeButtons()
        initializeList()
        setOnDismissListener {
        }
    }

    private fun initializeToolbar() {
        titleView.text = item.name
        iconView.setImageDrawable(ContextCompat.getDrawable(context, Utils.getIconForType(item.type)))
    }

    private fun initializeButtons() {
        saveButton.setOnClickListener {
            dismiss()
            saveListener?.invoke(item)
        }
        cancelButton.setOnClickListener {
            dismiss()
        }
        deleteButton.setOnClickListener {
            dismiss()
            deleteListener?.invoke(item)
        }
        renameButton.setOnClickListener {
            openEditTextDialog()
        }
        editButton.setOnClickListener {
            dismiss()
            editListener?.invoke(item)
        }
    }


    private fun openEditTextDialog() {
        val dialogBuilder = AlertDialog.Builder(context)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.document_rename_dialog, null)
        dialogBuilder.setView(dialogView)
        val editText = dialogView.findViewById<EditText>(R.id.document_rename_text)
        editText.setText(item.name, TextView.BufferType.EDITABLE)
        dialogBuilder.setTitle(context.resources.getString(R.string.document_rename_dialog_title))
        dialogBuilder.setPositiveButton(context.resources.getString(R.string.document_rename_dialog_done), { _, _ ->
            item.name = editText.text.toString()
            titleView.text = item.name
        })
        dialogBuilder.setNegativeButton(context.resources.getString(R.string.document_rename_dialog_cancel), { _, _ ->
        })
        val b = dialogBuilder.create()
        b.show()
    }

    private fun initializeList() {
        val adapter = Adapter()
        var adapterSet = false
        item.dataChunkSize = 10
        documentConverter = DocumentConverter(item, WeakReference(context))
        documentConverter.listener = {
            handler.post({
                progressBar.visibility = View.INVISIBLE
                item.data.addAll(it)
                if (!adapterSet) {
                    adapterSet = true
                    dataView.adapter = adapter
                }
                adapter.notifyDataSetChanged()
            })
        }
        documentConverter.start()
    }

    inner class Adapter : BaseAdapter() {

        var lastLoaded = 0

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view: ImageView?
            if (convertView == null) {
                view = ImageView(context)
                view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            } else {
                view = convertView as ImageView
            }
            view.setImageBitmap(getItem(position).currentImage)
            if (position % item.dataChunkSize == item.dataChunkSize / 2 && position > lastLoaded) {
                lastLoaded = position
                documentConverter.nextChunk()
            }
            return view
        }

        override fun getItem(position: Int): SerializableBitmap {
            return item.data[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getCount(): Int {
            return item.data.count()
        }

    }

}