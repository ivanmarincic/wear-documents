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

package io.github.twoloops.weardocuments.dialogs

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.widget.AppCompatImageButton
import android.widget.Button
import android.widget.TextView
import io.github.twoloops.weardocuments.BuildConfig
import io.github.twoloops.weardocuments.R


class AboutDialog(context: Context) : Dialog(context) {

    private val closeButton by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<AppCompatImageButton>(R.id.about_dialog_toolbar_close_button)
    }
    private val viewSourceButton by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<Button>(R.id.about_dialog_view_source)
    }
    private val leaveATipButton by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<Button>(R.id.about_dialog_leave_a_tip)
    }
    private val reportIssueButton by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<Button>(R.id.about_dialog_report_issue)
    }
    private val appVersion by lazy(LazyThreadSafetyMode.NONE) {
        findViewById<TextView>(R.id.about_dialog_application_version)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.about_dialog)
        closeButton.setOnClickListener {
            dismiss()
        }
        appVersion.text = "${context.resources.getString(R.string.about_dialog_version)} ${BuildConfig.VERSION_CODE}"
        viewSourceButton.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ivanmarincic/wear-documents"))
            context.startActivity(browserIntent)
        }
        leaveATipButton.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=TANZQE2TFLCKA"))
            context.startActivity(browserIntent)
        }
        reportIssueButton.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ivanmarincic/wear-documents/issues/new"))
            context.startActivity(browserIntent)
        }
    }
}