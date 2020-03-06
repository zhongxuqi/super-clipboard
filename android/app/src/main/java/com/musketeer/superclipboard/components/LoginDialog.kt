package com.musketeer.superclipboard.components

import android.app.AlertDialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.musketeer.superclipboard.R
import com.musketeer.superclipboard.utils.ClearErrorTextWatcher

class LoginDialog(val ctx: Context): View.OnClickListener {
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

        rootView.findViewById<View>(R.id.login).setOnClickListener(this)
        rootView.findViewById<View>(R.id.register).setOnClickListener(this)
        builder.setView(rootView)
        val dialog = builder.create()
        dialog.show()
    }

    private fun login() {
        var hasError = false
        if (loginAccount.text!!.isEmpty()) {
            loginAccountLayout.isErrorEnabled = true
            loginAccountLayout.error = ctx.getString(R.string.required)
            hasError = true
        }
        if (loginPassword.text!!.isEmpty()) {
            loginPasswordLayout.isErrorEnabled = true
            loginPasswordLayout.error = ctx.getString(R.string.required)
            hasError = true
        }
        if (hasError) {
            return
        }
    }

    private fun register() {
        var hasError = false
        if (registerAccount.text!!.isEmpty()) {
            registerAccountLayout.isErrorEnabled = true
            registerAccountLayout.error = ctx.getString(R.string.required)
            hasError = true
        }
        if (registerPassword.text!!.isEmpty()) {
            registerPasswordLayout.isErrorEnabled = true
            registerPasswordLayout.error = ctx.getString(R.string.required)
            hasError = true
        }
        if (registerPasswordRepeat.text!!.isEmpty()) {
            registerPasswordRepeatLayout.isErrorEnabled = true
            registerPasswordRepeatLayout.error = ctx.getString(R.string.required)
            hasError = true
        }
        if (registerPassword.text.toString() != registerPasswordRepeat.text.toString()) {
            registerPasswordRepeatLayout.isErrorEnabled = true
            registerPasswordRepeatLayout.error = ctx.getString(R.string.password_repeat_wrong)
            hasError = true
        }
        if (registerCaptcha.text!!.isEmpty()) {
            registerCaptchaLayout.isErrorEnabled = true
            registerCaptchaLayout.error = ctx.getString(R.string.required)
            hasError = true
        }
        if (hasError) {
            return
        }
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
        }
    }
}