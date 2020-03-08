package com.musketeer.superclipboard.utils

import android.text.Editable
import android.text.TextWatcher
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ClearErrorTextWatcher(val editText: TextInputEditText, val editTextLayout: TextInputLayout): TextWatcher {
    init {
        editText.addTextChangedListener(this)
    }

    override fun afterTextChanged(s: Editable?) {

    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        if (editTextLayout.isErrorEnabled) {
            editTextLayout.isErrorEnabled = false
        }
    }
}