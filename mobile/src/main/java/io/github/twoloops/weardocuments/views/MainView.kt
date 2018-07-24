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

package io.github.twoloops.weardocuments.views

import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import io.github.twoloops.core.Document
import io.github.twoloops.core.File
import io.github.twoloops.weardocuments.R
import io.github.twoloops.weardocuments.adapters.DocumentListAdapter
import io.github.twoloops.weardocuments.contracts.MainContract
import io.github.twoloops.weardocuments.dialogs.AboutDialog
import io.github.twoloops.weardocuments.dialogs.AlertInfoDialog
import io.github.twoloops.weardocuments.dialogs.DocumentPreviewDialog
import io.github.twoloops.weardocuments.dialogs.FileBrowserDialog
import io.github.twoloops.weardocuments.presenters.MainPresenter


class MainView : AppCompatActivity(), MainContract.View {


    val READ_STORAGE_REQUEST_CODE = 2

    private lateinit var presenter: MainPresenter
    val addButton: FloatingActionButton by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<FloatingActionButton>(R.id.main_view_add_button)
    }
    val fileList: RecyclerView by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<RecyclerView>(R.id.main_view_list)
    }
    val addFilesText: TextView by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<TextView>(R.id.main_view_add_files_text)
    }
    val adapter: DocumentListAdapter by lazy(LazyThreadSafetyMode.NONE) {
        DocumentListAdapter()
    }
    lateinit var fileBrowserDialog: FileBrowserDialog
    var items: ArrayList<Document>? = null
    private var isNew = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_view)
        presenter = MainPresenter()
        presenter.start(this)
        presenter.loadData()
        presenter.initializeAddButton()
        presenter.initializeList()
        presenter.initializeDialogs()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuInflater = menuInflater;
        menuInflater.inflate(R.menu.main_view_toolbar, menu)
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == READ_STORAGE_REQUEST_CODE) {
            if (grantResults[0] == PERMISSION_GRANTED) {
                addButton.isEnabled = true
            } else {
                addButton.isEnabled = false
                Toast.makeText(this, getString(R.string.main_view_read_permission_denied), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.toolbar_menu_about_button -> {
            val aboutDialog = AboutDialog(this)
            aboutDialog.show()
            true
        }
//        R.id.toolbar_menu_settings_button -> {
//            val settingsDialog = SettingsDialog(this)
//            settingsDialog.show()
//            true
//        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    override fun createDocument(files: ArrayList<File>, oldDocument: Document?): Document {
        val document = oldDocument ?: Document()
        if (files.count() > 0) {
            val type = files[0].type
            document.files = files
            document.type = type
            if (type == File.FILE_TYPE_PDF) {
                document.name = files[0].name
            } else {
                document.name = "Image gallery"
            }
        }
        if (oldDocument == null) {
            isNew = true
        }
        return document
    }

    override fun addDocument(document: Document) {
        document.dataChunkSize = 0
        document.dataChunkStart = 0
        if (isNew) {
            items!!.add(document)
        } else {
            for (i in 0..(items!!.count() - 1)) {
                if (items!![i].id == document.id) {
                    items!![i] = document
                    break
                }
            }
        }
        checkDocumentCount()
        isNew = false
    }

    override fun checkDocumentCount() {
        if (items!!.count() > 0) {
            addFilesText.visibility = View.GONE
        } else {
            addFilesText.visibility = View.VISIBLE
        }
    }

    private fun deleteItem(item: Document) {
        runOnUiThread({
            items!!.remove(item)
            presenter.saveData()
            adapter.notifyDataSetChanged()
            checkDocumentCount()
        })
    }

    override fun getDocumentPreviewDialog(document: Document): DocumentPreviewDialog {
        val dialog = DocumentPreviewDialog(this, document)
        dialog.deleteListener = { documentToDelete ->
            AlertInfoDialog.getInstance(this).deleteOnThisDeviceOnly { thisOnly ->
                if (thisOnly) {
                    deleteItem(documentToDelete)
                } else {
                    presenter.deleteDocument(documentToDelete, { isSuccessful ->
                        if (isSuccessful) {
                            deleteItem(documentToDelete)
                        } else {
                            AlertInfoDialog.getInstance(this).alertDeleteAnyway {
                                deleteItem(documentToDelete)
                            }
                        }
                    })
                }
            }
        }
        dialog.saveListener = {
            addDocument(document)
            presenter.saveData()
            adapter.notifyDataSetChanged()
            presenter.sendDocument(it)
        }
        dialog.editListener = {
            getFileBrowserDialog(document).show()
        }
        return dialog
    }

    override fun getFileBrowserDialog(document: Document?): FileBrowserDialog {
        fileBrowserDialog.selectedFiles = document?.files
        fileBrowserDialog.listener = {
            getDocumentPreviewDialog(createDocument(it, document)).show()
        }
        return fileBrowserDialog
    }
}