package io.github.twoloops.weardocuments.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import io.github.twoloops.weardocuments.R


class AboutDialog(context: Context) : Dialog(context) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.about_dialog)
    }
}