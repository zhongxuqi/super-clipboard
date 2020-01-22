package com.musketeer.superclipboard

import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.graphics.PixelFormat
import android.os.Build
import android.util.DisplayMetrics
import android.view.*
import android.view.View.OnTouchListener
import android.widget.ListView
import android.widget.Toast
import com.musketeer.superclipboard.adapter.HistoryListAdapter
import com.musketeer.superclipboard.data.Content
import java.util.*


class ClipboardMainWindow constructor(private val mContext: Context) {
    companion object {
        private var mIsFloatViewShowing = false
        private var mWindowManager: WindowManager? = null
        private var mFloatView: View? = null
        private var mFloatViewLayoutParams: WindowManager.LayoutParams? =null
        private var mFloatViewTouchConsumedByMove = false
        private var mFloatViewLastX = 0
        private var mFloatViewLastY = 0
        private var mFloatViewFirstX = 0
        private var mFloatViewFirstY = 0

        private var mContentListView: ListView? = null
        private var mContentListAdapter: HistoryListAdapter? = null
        private var mContentList: LinkedList<Content> = LinkedList()
    }

    fun dismissFloatView() {
        if (mIsFloatViewShowing) {
            mIsFloatViewShowing = false
            if (mWindowManager != null) {
                mWindowManager!!.removeViewImmediate(mFloatView)
            }
        }
    }

    fun showFloatView() {
        if (!mIsFloatViewShowing) {
            mIsFloatViewShowing = true
            if (mWindowManager != null) {
                mWindowManager!!.addView(mFloatView, mFloatViewLayoutParams)
            }
        }
    }

    private val mFloatViewOnClickListener: View.OnClickListener = object : View.OnClickListener {
        override
        fun onClick(v: View?) {
            Toast.makeText(
                mContext,
                "Float view is clicked!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val mFloatViewOnTouchListener = OnTouchListener { v, event ->
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

    init {
        mWindowManager = mContext.getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        mWindowManager!!.defaultDisplay.getMetrics(metrics)
        val inflater = LayoutInflater.from(mContext)
        mFloatView = inflater.inflate(R.layout.clipboard_main_layout, null)
        mFloatView!!.setOnClickListener(mFloatViewOnClickListener)
        mFloatView!!.setOnTouchListener(mFloatViewOnTouchListener)
        mFloatViewLayoutParams = WindowManager.LayoutParams()
        mFloatViewLayoutParams!!.format = PixelFormat.TRANSLUCENT
        mFloatViewLayoutParams!!.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        mFloatViewLayoutParams!!.type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        mFloatViewLayoutParams!!.gravity = Gravity.CENTER
        mFloatViewLayoutParams!!.width = (metrics.widthPixels * 0.6).toInt()
        mFloatViewLayoutParams!!.height = (metrics.heightPixels * 0.6).toInt()

        for (i in 0..10) {
            var contentType = Content.ContentType.Text
            if (i%2 != 0) {
                contentType = Content.ContentType.Image
            }
            mContentList.add(Content(contentType, "text $i", "", 0, 0))
        }
        mContentListView = mFloatView!!.findViewById(R.id.history_list) as ListView
        mContentListAdapter = HistoryListAdapter(mContext, R.id.history_list_item_content, mContentList)
        mContentListView!!.adapter = mContentListAdapter
    }
}