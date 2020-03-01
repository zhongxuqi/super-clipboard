package com.musketeer.superclipboard.components

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import com.musketeer.superclipboard.R

class FeedbackDialog {
    companion object {
        var dialog: AlertDialog? = null
        var editText: EditText? = null

        fun showDialog(ctx: Context) {
            val builder = AlertDialog.Builder(ctx)
            val v = LayoutInflater.from(ctx).inflate(R.layout.dialog_feedback, null)
            editText = v.findViewById(R.id.feedback_text)
            v.findViewById<View>(R.id.feedback_cancel).setOnClickListener(object: View.OnClickListener{
                override fun onClick(v: View?) {
                    dialog?.dismiss()
                }
            })
            v.findViewById<View>(R.id.feedback_submit).setOnClickListener(object: View.OnClickListener{
                override fun onClick(v: View?) {
                    dialog?.dismiss()
                }
            })
            builder.setView(v)
            val dialog = builder.create()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                dialog.window!!.setType((WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY));
            } else {
                dialog.window!!.setType((WindowManager.LayoutParams.TYPE_SYSTEM_ALERT));
            }
            FeedbackDialog.dialog = dialog
            dialog.show()
        }
    }
}