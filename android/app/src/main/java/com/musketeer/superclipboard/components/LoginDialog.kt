package com.musketeer.superclipboard.components

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.WindowManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.musketeer.superclipboard.R

class LoginDialog {
    companion object {
        var dialog: BottomSheetDialog? = null

        fun showDialog(ctx: Context) {
            val dialog = BottomSheetDialog(ctx)
            val v = LayoutInflater.from(ctx).inflate(R.layout.dialog_login, null)
            dialog.setContentView(v)
            dialog.show()
        }
    }
}