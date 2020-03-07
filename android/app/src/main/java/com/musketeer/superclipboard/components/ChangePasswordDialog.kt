package com.musketeer.superclipboard.components

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.musketeer.superclipboard.ClipboardMainWindow
import com.musketeer.superclipboard.R
import com.musketeer.superclipboard.net.HttpClient
import com.musketeer.superclipboard.net.UdpClient
import com.musketeer.superclipboard.utils.ClearErrorTextWatcher
import com.musketeer.superclipboard.utils.HashUtils
import com.musketeer.superclipboard.utils.SharePreference
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class ChangePasswordDialog(val ctx: Context): View.OnClickListener {
    private val dialog: AlertDialog
    private val rootView: View
    private val originPasswordLayout: TextInputLayout
    private val originPassword: TextInputEditText
    private val newPasswordLayout: TextInputLayout
    private val newPassword: TextInputEditText
    private val newPasswordRepeatLayout: TextInputLayout
    private val newPasswordRepeat: TextInputEditText

    init {
        val builder = AlertDialog.Builder(ctx)
        rootView = LayoutInflater.from(ctx).inflate(R.layout.dialog_change_password, null)

        originPasswordLayout = rootView.findViewById(R.id.origin_password_layout)
        originPassword = rootView.findViewById(R.id.origin_password)
        originPassword.addTextChangedListener(ClearErrorTextWatcher(originPassword, originPasswordLayout))
        newPasswordLayout = rootView.findViewById(R.id.new_password_layout)
        newPassword = rootView.findViewById(R.id.new_password)
        newPassword.addTextChangedListener(ClearErrorTextWatcher(newPassword, newPasswordLayout))
        newPasswordRepeatLayout = rootView.findViewById(R.id.new_password_repeat_layout)
        newPasswordRepeat = rootView.findViewById(R.id.new_password_repeat)
        newPasswordRepeat.addTextChangedListener(ClearErrorTextWatcher(newPasswordRepeat, newPasswordRepeatLayout))

        rootView.findViewById<View>(R.id.cancel_button).setOnClickListener(this)
        rootView.findViewById<View>(R.id.submit_button).setOnClickListener(this)
        builder.setView(rootView)
        dialog = builder.create()
        dialog.show()
    }

    private fun changePassword() {
        var hasError = false
        val originPasswordText = originPassword.text.toString().trim()
        val newPasswordText = newPassword.text.toString().trim()
        val newPasswordRepeatText = newPasswordRepeat.text.toString().trim()
        if (originPasswordText.isEmpty()) {
            originPasswordLayout.isErrorEnabled = true
            originPasswordLayout.error = ctx.getString(R.string.required)
            hasError = true
        }
        if (newPasswordText.isEmpty()) {
            newPasswordLayout.isErrorEnabled = true
            newPasswordLayout.error = ctx.getString(R.string.required)
            hasError = true
        }
        if (newPasswordRepeatText.isEmpty()) {
            newPasswordRepeatLayout.isErrorEnabled = true
            newPasswordRepeatLayout.error = ctx.getString(R.string.required)
            hasError = true
        }
        if (newPasswordText != newPasswordRepeatText) {
            newPasswordRepeatLayout.isErrorEnabled = true
            newPasswordRepeatLayout.error = ctx.getString(R.string.password_repeat_wrong)
            hasError = true
        }
        if (hasError) {
            return
        }
        val userID = SharePreference.getUserID(ctx)
        UdpClient.Instance!!.threadPool.submit(object: Runnable{
            override fun run() {
                try {
                    val nowTs = System.currentTimeMillis() / 1000
                    val req = JSONObject()
                    req.put("app_id", HttpClient.AppID)
                    req.put("account", userID)
                    req.put("time", nowTs)
                    req.put("token", HashUtils.sha256("${userID}-${HashUtils.sha256(originPasswordText)}-$nowTs"))
                    req.put("password", HashUtils.sha256(newPasswordText))
                    val body: RequestBody = req.toString()
                        .toRequestBody("application/json; charset=utf-8".toMediaType())
                    val request: Request = Request.Builder()
                        .url("${HttpClient.Host}/openapi/account/change_password")
                        .post(body)
                        .build()
                    val respbody = HttpClient.client.newCall(request).execute().body
                    if (respbody != null) {
                        val jo = JSONObject(String(respbody.bytes()))
                        ClipboardMainWindow.Instance!!.handler.post(object: Runnable{
                            override fun run() {
                                Log.d("===>>>", jo.toString())
                                when (jo.getInt("errno")) {
                                    0 -> {
                                        Toast.makeText(ctx, ctx.getString(R.string.change_password_success), Toast.LENGTH_SHORT).show()
                                        dialog.dismiss()
                                    }
                                    4000012 -> {
                                        originPasswordLayout.isErrorEnabled = true
                                        originPasswordLayout.error = ctx.getString(R.string.password_wrong)
                                    }
                                    else -> {
                                        Toast.makeText(ctx, ctx.getString(R.string.network_error), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        })
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.cancel_button -> {
                dialog.dismiss()
            }
            R.id.submit_button -> {
                changePassword()
            }
        }
    }
}