package com.musketeer.superclipboard.components

import android.app.AlertDialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
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
import com.musketeer.superclipboard.utils.UserType
import com.squareup.picasso.Picasso
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.MessageDigest

interface LoginCallback {
    fun onLogined()
}

class LoginDialog(val ctx: Context, val callback: LoginCallback): View.OnClickListener {
    private val dialog: AlertDialog
    private val rootView: View
    private val loginPage: View
    private val registerPage: View
    private val loginAccountLayout: TextInputLayout
    private val loginAccount: TextInputEditText
    private val loginPasswordLayout: TextInputLayout
    private val loginPassword: TextInputEditText
    private val registerAccountLayout: TextInputLayout
    private val registerAccount: TextInputEditText
    private val registerPasswordLayout: TextInputLayout
    private val registerPassword: TextInputEditText
    private val registerPasswordRepeatLayout: TextInputLayout
    private val registerPasswordRepeat: TextInputEditText
    private val registerCaptchaLayout: TextInputLayout
    private val registerCaptcha: TextInputEditText
    private val captchaImage: ImageView
    private var captchaID: String = ""

    init {
        val builder = AlertDialog.Builder(ctx)
        rootView = LayoutInflater.from(ctx).inflate(R.layout.dialog_login, null)
        rootView.findViewById<View>(R.id.go_login).setOnClickListener(this)
        rootView.findViewById<View>(R.id.go_register).setOnClickListener(this)
        loginPage = rootView.findViewById(R.id.login_page)
        registerPage = rootView.findViewById(R.id.register_page)

        // login
        loginAccountLayout = rootView.findViewById(R.id.login_account_layout)
        loginAccount = rootView.findViewById(R.id.login_account)
        loginAccount.addTextChangedListener(ClearErrorTextWatcher(loginAccount, loginAccountLayout))
        loginPasswordLayout = rootView.findViewById(R.id.login_password_layout)
        loginPassword = rootView.findViewById(R.id.login_password)
        loginPassword.addTextChangedListener(ClearErrorTextWatcher(loginPassword, loginPasswordLayout))

        // register
        registerAccountLayout = rootView.findViewById(R.id.register_account_layout)
        registerAccount = rootView.findViewById(R.id.register_account)
        registerAccount.addTextChangedListener(ClearErrorTextWatcher(registerAccount, registerAccountLayout))
        registerPasswordLayout = rootView.findViewById(R.id.register_password_layout)
        registerPassword = rootView.findViewById(R.id.register_password)
        registerPassword.addTextChangedListener(ClearErrorTextWatcher(registerPassword, registerPasswordLayout))
        registerPasswordRepeatLayout = rootView.findViewById(R.id.register_password_repeat_layout)
        registerPasswordRepeat = rootView.findViewById(R.id.register_password_repeat)
        registerPasswordRepeat.addTextChangedListener(ClearErrorTextWatcher(registerPasswordRepeat, registerPasswordRepeatLayout))
        registerCaptchaLayout = rootView.findViewById(R.id.register_captcha_layout)
        registerCaptcha = rootView.findViewById(R.id.register_captcha)
        registerCaptcha.addTextChangedListener(ClearErrorTextWatcher(registerCaptcha, registerCaptchaLayout))
        captchaImage = rootView.findViewById(R.id.captcha_image)
        captchaImage.setOnClickListener(this)

        rootView.findViewById<View>(R.id.login).setOnClickListener(this)
        rootView.findViewById<View>(R.id.register).setOnClickListener(this)
        builder.setView(rootView)
        dialog = builder.create()
        dialog.show()
        getCaptcha()
    }

    private fun getCaptcha() {
        UdpClient.Instance!!.threadPool.submit(object: Runnable{
            override fun run() {
                try {
                    val request: Request = Request.Builder()
                        .url("${HttpClient.Host}/openapi/captcha/new")
                        .get()
                        .build()
                    val respbody = HttpClient.client.newCall(request).execute().body
                    if (respbody != null) {
                        val jo = JSONObject(String(respbody.bytes()))
                        if (jo.getInt("errno") == 0) {
                            captchaID = jo.getJSONObject("data").getString("captcha_id")
                            ClipboardMainWindow.Instance!!.handler.post(object : Runnable {
                                override fun run() {
                                    Picasso.get().load("${HttpClient.Host}/openapi/captcha/${captchaID}.png")
                                        .into(captchaImage);
                                }
                            })
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
    }

    private fun login() {
        var hasError = false
        val loginAccountText = loginAccount.text.toString().trim()
        val loginPasswordText = loginPassword.text.toString().trim()
        if (loginAccountText.isEmpty()) {
            loginAccountLayout.isErrorEnabled = true
            loginAccountLayout.error = ctx.getString(R.string.required)
            hasError = true
        }
        if (loginPasswordText.isEmpty()) {
            loginPasswordLayout.isErrorEnabled = true
            loginPasswordLayout.error = ctx.getString(R.string.required)
            hasError = true
        }
        if (hasError) {
            return
        }
        UdpClient.Instance!!.threadPool.submit(object: Runnable{
            override fun run() {
                try {
                    val nowTs = System.currentTimeMillis() / 1000
                    val req = JSONObject()
                    req.put("app_id", HttpClient.AppID)
                    req.put("account", loginAccountText)
                    req.put("time", nowTs)
                    req.put("token", HashUtils.sha256("${loginAccountText}-${HashUtils.sha256(loginPasswordText)}-$nowTs"))
                    val body: RequestBody = req.toString()
                        .toRequestBody("application/json; charset=utf-8".toMediaType())
                    val request: Request = Request.Builder()
                        .url("${HttpClient.Host}/openapi/account/login")
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
                                        Toast.makeText(ctx, ctx.getString(R.string.login_success), Toast.LENGTH_SHORT).show()
                                        SharePreference.setUserType(ctx, UserType.UserTypeAccount)
                                        SharePreference.setUserID(ctx, jo.getJSONObject("data").getString("account"))
                                        dialog.dismiss()
                                        callback.onLogined()
                                    }
                                    4000010 -> {
                                        loginAccountLayout.isErrorEnabled = true
                                        loginAccountLayout.error = ctx.getString(R.string.account_not_exists)
                                    }
                                    4000012 -> {
                                        loginPasswordLayout.isErrorEnabled = true
                                        loginPasswordLayout.error = ctx.getString(R.string.password_wrong)
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

    private fun register() {
        var hasError = false
        val registerAccountText = registerAccount.text.toString().trim()
        val registerPasswordText = registerPassword.text.toString().trim()
        val registerPasswordRepeatText = registerPasswordRepeat.text.toString().trim()
        val registerCaptchaText = registerCaptcha.text.toString().trim()
        if (registerAccountText.isEmpty()) {
            registerAccountLayout.isErrorEnabled = true
            registerAccountLayout.error = ctx.getString(R.string.required)
            hasError = true
        }
        if (registerPasswordText.isEmpty()) {
            registerPasswordLayout.isErrorEnabled = true
            registerPasswordLayout.error = ctx.getString(R.string.required)
            hasError = true
        }
        if (registerPasswordRepeatText.isEmpty()) {
            registerPasswordRepeatLayout.isErrorEnabled = true
            registerPasswordRepeatLayout.error = ctx.getString(R.string.required)
            hasError = true
        }
        if (registerPasswordText != registerPasswordRepeatText) {
            registerPasswordRepeatLayout.isErrorEnabled = true
            registerPasswordRepeatLayout.error = ctx.getString(R.string.password_repeat_wrong)
            hasError = true
        }
        if (registerCaptchaText.isEmpty()) {
            registerCaptchaLayout.isErrorEnabled = true
            registerCaptchaLayout.error = ctx.getString(R.string.required)
            hasError = true
        }
        if (hasError) {
            return
        }
        UdpClient.Instance!!.threadPool.submit(object: Runnable{
            override fun run() {
                try {
                    val req = JSONObject()
                    req.put("app_id", HttpClient.AppID)
                    req.put("account", registerAccountText)
                    req.put("password", HashUtils.sha256(registerPasswordText))
                    req.put("captcha_id", captchaID)
                    req.put("captcha_solution", registerCaptchaText)
                    val body: RequestBody = req.toString()
                        .toRequestBody("application/json; charset=utf-8".toMediaType())
                    val request: Request = Request.Builder()
                        .url("${HttpClient.Host}/openapi/account/register")
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
                                        Toast.makeText(ctx, ctx.getString(R.string.register_success), Toast.LENGTH_SHORT).show()
                                        SharePreference.setUserType(ctx, UserType.UserTypeAccount)
                                        SharePreference.setUserID(ctx, jo.getJSONObject("data").getString("account"))
                                        dialog.dismiss()
                                        callback.onLogined()
                                    }
                                    4000030 -> {
                                        registerCaptchaLayout.isErrorEnabled = true
                                        registerCaptchaLayout.error = ctx.getString(R.string.captcha_code_error)
                                    }
                                    4000011 -> {
                                        registerAccountLayout.isErrorEnabled = true
                                        registerAccountLayout.error = ctx.getString(R.string.account_exists)
                                    }
                                    else -> {
                                        Toast.makeText(ctx, ctx.getString(R.string.network_error), Toast.LENGTH_SHORT).show()
                                    }
                                }
                                getCaptcha()
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
            R.id.go_login -> {
                loginPage.visibility = View.VISIBLE
                registerPage.visibility = View.GONE
            }
            R.id.go_register -> {
                loginPage.visibility = View.GONE
                registerPage.visibility = View.VISIBLE
            }
            R.id.login -> {
                login()
            }
            R.id.register -> {
                register()
            }
            R.id.captcha_image -> {
                getCaptcha()
            }
        }
    }
}