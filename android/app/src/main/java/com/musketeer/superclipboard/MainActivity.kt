package com.musketeer.superclipboard

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.musketeer.superclipboard.net.UdpClient


class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val REQUEST_CODE_DRAW_OVERLAY_PERMISSION = 5
    var clipboardMainWindow: ClipboardMainWindow? = null

    val syncSwitcher: SwitchMaterial by lazy {
        findViewById<SwitchMaterial>(R.id.sync_switcher)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.btn_open_floatview).setOnClickListener(this)
        if (ClipboardMainWindow.Instance != null) {
            clipboardMainWindow = ClipboardMainWindow.Instance
        } else {
            clipboardMainWindow = ClipboardMainWindow(this)
        }
        syncSwitcher.isChecked = UdpClient.Instance!!.isRunning
        syncSwitcher.setOnCheckedChangeListener(object: CompoundButton.OnCheckedChangeListener{
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                if (isChecked) {
                    UdpClient.Instance!!.start()
                } else {
                    UdpClient.Instance!!.close()
                }
            }
        })
    }

    fun showFloatView() {
        if (checkDrawOverlayPermission()) {
            clipboardMainWindow!!.showFloatView()
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_DRAW_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                clipboardMainWindow!!.showFloatView()
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

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.btn_open_floatview -> {
                showFloatView()
            }
        }
    }
}
