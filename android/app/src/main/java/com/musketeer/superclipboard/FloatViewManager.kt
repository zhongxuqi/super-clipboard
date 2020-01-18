package com.musketeer.superclipboard

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context.WINDOW_SERVICE
import android.graphics.PixelFormat
import android.os.Build
import android.view.*
import android.view.View.OnTouchListener
import android.widget.Button
import android.widget.TextView
import android.widget.Toast


class FloatViewManager @SuppressLint("InflateParams") constructor(private val mActivity: Activity) {
    private var mWindowManager: WindowManager? = null
    private var mFloatView: View? = null
    private var mFloatViewLayoutParams: WindowManager.LayoutParams? =null
    private var mFloatViewTouchConsumedByMove = false
    private var mFloatViewLastX = 0
    private var mFloatViewLastY = 0
    private var mFloatViewFirstX = 0
    private var mFloatViewFirstY = 0
    var imageButton: Button? = null
    fun dismissFloatView() {
        if (mIsFloatViewShowing) {
            mIsFloatViewShowing = false
            mActivity.runOnUiThread {
                if (mWindowManager != null) {
                    mWindowManager!!.removeViewImmediate(mFloatView)
                }
            }
        }
    }

    fun showFloatView() {
        if (!mIsFloatViewShowing) {
            mIsFloatViewShowing = true
            mActivity.runOnUiThread {
                if (!mActivity.isFinishing) {
                    mWindowManager =
                        mActivity.getSystemService(WINDOW_SERVICE) as WindowManager
                    if (mWindowManager != null) {
                        mWindowManager!!.addView(mFloatView, mFloatViewLayoutParams)
                    }
                }
            }
        }
    }

    private val mFloatViewOnClickListener: View.OnClickListener = object : View.OnClickListener {
        override
        fun onClick(v: View?) {
            mActivity.runOnUiThread {
                Toast.makeText(
                    mActivity,
                    "Float view is clicked!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private val mFloatViewOnTouchListener =
        OnTouchListener { v, event ->
            val prm = mFloatViewLayoutParams
            val totalDeltaX = mFloatViewLastX - mFloatViewFirstX
            val totalDeltaY = mFloatViewLastY - mFloatViewFirstY
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    mFloatViewLastX = event.rawX.toInt()
                    mFloatViewLastY = event.rawY.toInt()
                    mFloatViewFirstX = mFloatViewLastX
                    mFloatViewFirstY = mFloatViewLastY
                }
                MotionEvent.ACTION_UP -> {
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX.toInt() - mFloatViewLastX
                    val deltaY = event.rawY.toInt() - mFloatViewLastY
                    mFloatViewLastX = event.rawX.toInt()
                    mFloatViewLastY = event.rawY.toInt()
                    if (Math.abs(totalDeltaX) >= 5 || Math.abs(totalDeltaY) >= 5) {
                        if (event.pointerCount == 1) {
                            prm!!.x += deltaX
                            prm!!.y += deltaY
                            mFloatViewTouchConsumedByMove = true
                            if (mWindowManager != null) {
                                mWindowManager!!.updateViewLayout(mFloatView, prm)
                            }
                        } else {
                            mFloatViewTouchConsumedByMove = false
                        }
                    } else {
                        mFloatViewTouchConsumedByMove = false
                    }
                }
                else -> {
                }
            }
            mFloatViewTouchConsumedByMove
        }

    companion object {
        private var mIsFloatViewShowing = false
    }

    init {
        val inflater = LayoutInflater.from(mActivity)
        mFloatView = inflater.inflate(R.layout.float_view_layout, null)
        val textView: TextView = mFloatView!!.findViewById(R.id.float_text)
        textView.text = "I'm a float view!"
        imageButton = mFloatView!!.findViewById(R.id.float_btn)
        mFloatView!!.setOnClickListener(mFloatViewOnClickListener)
        mFloatView!!.setOnTouchListener(mFloatViewOnTouchListener)
        mFloatViewLayoutParams = WindowManager.LayoutParams()
        mFloatViewLayoutParams!!.format = PixelFormat.TRANSLUCENT
        mFloatViewLayoutParams!!.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        mFloatViewLayoutParams!!.type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_TOAST
        mFloatViewLayoutParams!!.gravity = Gravity.CENTER
        mFloatViewLayoutParams!!.width = WindowManager.LayoutParams.WRAP_CONTENT
        mFloatViewLayoutParams!!.height = WindowManager.LayoutParams.WRAP_CONTENT
    }
}