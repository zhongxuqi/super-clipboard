package com.musketeer.superclipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    var manager: ClipboardManager? = null
    private val REQUEST_CODE_DRAW_OVERLAY_PERMISSION = 5
    var mFloatViewManager: FloatViewManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mFloatViewManager = FloatViewManager(this)

        manager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        findViewById<Button>(R.id.btn).setOnClickListener(object:View.OnClickListener{
            override fun onClick(p0: View?) {
                this@MainActivity.manager!!.setPrimaryClip(ClipData.newPlainText("label test", "text test"))
                Log.w("clipboard", "put text")
                if (checkDrawOverlayPermission()) {
                    mFloatViewManager!!.showFloatView()
                }
            }
        })

        mFloatViewManager!!.imageButton!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {

                System.out.printf("===>>> %s\n", manager!!.primaryClip.toString())
                Log.w("clipboard", "get text: ${manager!!.primaryClip.toString()}")
            }
        })
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_DRAW_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                mFloatViewManager!!.showFloatView()
            }
        }
    }

    private fun checkDrawOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_CODE_DRAW_OVERLAY_PERMISSION)
            false
        } else {
            true
        }
    }
}
