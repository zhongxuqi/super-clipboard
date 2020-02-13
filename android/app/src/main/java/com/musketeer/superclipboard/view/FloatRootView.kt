package com.musketeer.superclipboard.view

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.musketeer.superclipboard.ClipboardMainWindow


class FloatRootView: ConstraintLayout {
    constructor(context: Context?): super(context)

    constructor(context: Context?, attrs: AttributeSet?): super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr)

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK || event.getKeyCode() == KeyEvent.KEYCODE_SETTINGS) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                val ins = ClipboardMainWindow.Instance
                if (ins != null && ins.minMainView.visibility != View.VISIBLE) {
                    ClipboardMainWindow.Instance?.minWindow()
                }
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }
}