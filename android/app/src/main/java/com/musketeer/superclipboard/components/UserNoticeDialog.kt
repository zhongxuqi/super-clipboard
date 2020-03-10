package com.musketeer.superclipboard.components

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import com.musketeer.superclipboard.R

interface UserNoticeCallback {
    fun onAgree()
}

class UserNoticeDialog(val ctx: Context, val callback: UserNoticeCallback) {
    init {
        val builder = AlertDialog.Builder(ctx)
        val v = LayoutInflater.from(ctx).inflate(R.layout.dialog_user_notice, null)
        builder.setView(v)
        val dialog = builder.create()
        v.findViewById<View>(R.id.readed_btn).setOnClickListener(object: View.OnClickListener{
            override fun onClick(v: View?) {
                dialog.dismiss()
            }
        })
        dialog.setOnDismissListener(object: DialogInterface.OnDismissListener{
            override fun onDismiss(dialog: DialogInterface?) {
                callback.onAgree()
            }
        })
        dialog.show()
    }
}