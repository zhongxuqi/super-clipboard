package com.musketeer.superclipboard

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.musketeer.superclipboard.components.ChangePasswordDialog
import com.musketeer.superclipboard.components.FeedbackDialog
import com.musketeer.superclipboard.components.LoginCallback
import com.musketeer.superclipboard.components.LoginDialog
import com.musketeer.superclipboard.net.UdpClient
import com.musketeer.superclipboard.utils.SharePreference
import com.musketeer.superclipboard.utils.UserType
import com.tencent.connect.common.Constants
import com.tencent.tauth.IUiListener
import com.tencent.tauth.Tencent
import com.tencent.tauth.UiError
import org.json.JSONObject


class MainActivity : AppCompatActivity(), View.OnClickListener, View.OnLongClickListener, IUiListener, LoginCallback {
    private val REQUEST_CODE_DRAW_OVERLAY_PERMISSION = 5
    var clipboardMainWindow: ClipboardMainWindow? = null

    val syncSwitcher: SwitchMaterial by lazy {
        findViewById<SwitchMaterial>(R.id.sync_switcher)
    }

    val mTencent: Tencent by lazy {
        Tencent.createInstance("101857020", this.applicationContext)
    }

    val userTypeImage: ImageView by lazy {
        findViewById<ImageView>(R.id.user_type)
    }
    val userNameText: TextView by lazy {
        findViewById<TextView>(R.id.user_name)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.btn_open_floatview).setOnClickListener(this)
        findViewById<View>(R.id.btn_feedback).setOnClickListener(this)
        findViewById<View>(R.id.btn_login).setOnClickListener(this)
        findViewById<View>(R.id.btn_login).setOnLongClickListener(this)

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
        refreshUserType()
    }

    fun refreshUserType() {
        val userType = SharePreference.getUserType(this)
        val userID = SharePreference.getUserID(this)
        when (userType) {
            UserType.UserTypeAccount -> {
                userNameText.text = "${getString(R.string.account)} $userID\n${getString(R.string.logined)}"
            }
            else -> {

            }
        }
    }

    fun showFloatView() {
        if (checkDrawOverlayPermission()) {
            clipboardMainWindow!!.showFloatView()
        }
    }

    fun showLoginDialog() {
        LoginDialog(this, this)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        if (requestCode == Constants.REQUEST_LOGIN) {
            Tencent.onActivityResultData(requestCode, resultCode, data, this)
        }
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
            R.id.btn_feedback -> {
                FeedbackDialog.showDialog(this)
            }
            R.id.btn_login -> {
                showLoginDialog()
            }
        }
    }

    override fun onLongClick(v: View?): Boolean {
        when (v!!.id) {
            R.id.btn_login -> {
                val userType = SharePreference.getUserType(this)
                if (userType == null || userType == UserType.UserTypeUnknow) {
                    return false
                }
                ChangePasswordDialog(this)
                return true
            }
        }
        return false
    }

    // account
    override fun onLogined() {
        refreshUserType()
    }

    // QQ
    override fun onComplete(p0: Any?) {
        SharePreference.setUserType(this@MainActivity, UserType.UserTypeQQ)
        val jo = JSONObject(p0.toString())
        SharePreference.setUserID(this@MainActivity, jo.getString("openid"))
        refreshUserType()
    }

    override fun onCancel() {
        Log.d("===>>>", "onCancel")
    }

    override fun onError(p0: UiError?) {
        Log.d("===>>>", "onError ${p0.toString()}")
    }
}
