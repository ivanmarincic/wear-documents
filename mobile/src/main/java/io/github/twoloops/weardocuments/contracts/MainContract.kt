package io.github.twoloops.weardocuments.contracts

import io.github.twoloops.core.Document
import io.github.twoloops.core.File
import io.github.twoloops.weardocuments.dialogs.DocumentPreviewDialog
import io.github.twoloops.weardocuments.dialogs.FileBrowserDialog


class MainContract {

    interface View {
        fun createDocument(files: ArrayList<File>): Document
        fun addDocument(document: Document)
        fun checkDocumentCount()
        fun getDocumentPreviewDialog(document: Document): DocumentPreviewDialog
        fun getFileBrowserDialog(document: Document? = null): FileBrowserDialog
    }

    interface Presenter {
        fun start(view: View)
        fun initializeAddButton()
        fun initializeList()
        fun initializeToolbar()
        fun hasPermission(): Boolean
        fun sendDocument(document: Document)
        fun deleteDocument(document: Document, listener: ((isSuccessful: Boolean) -> Unit))
        fun getFile()
        fun loadData()
        fun saveData()
    }
}