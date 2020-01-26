package com.musketeer.superclipboard

import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.graphics.PixelFormat
import android.os.Build
import android.util.DisplayMetrics
import android.view.*
import android.view.View.OnTouchListener
import android.widget.ImageView
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.Toast
import com.musketeer.superclipboard.adapter.HistoryListAdapter
import com.musketeer.superclipboard.data.ClipBoardMessage
import com.musketeer.superclipboard.db.SqliteHelper
import java.util.*


class ClipboardMainWindow constructor(private val mContext: Context) {
    companion object {
        private var mContext: Context? = null
        private var screenWidth: Int = 0
        private var screenHeight: Int = 0

        private var mIsFloatViewShowing = false
        private var mWindowManager: WindowManager? = null
        private var mFloatView: View? = null
        private var mFloatViewLayoutParams: WindowManager.LayoutParams? =null
        private var mFloatViewTouchConsumedByMove = false
        private var mFloatViewLastX = 0
        private var mFloatViewLastY = 0
        private var mFloatViewFirstX = 0
        private var mFloatViewFirstY = 0
        private var personSettingsBtn: ImageView? = null

        private var mContentListView: ListView? = null
        private var mContentListAdapter: HistoryListAdapter? = null
        private var mContentList: LinkedList<ClipBoardMessage> = LinkedList()

        private var popWindow: PopupWindow? = null
        private var personSettingsView: View? = null

        fun refreshAdapter() {
            val msgList = SqliteHelper.helper!!.ListAll()
            mContentList.clear()
            mContentList.addAll(msgList)
            mContentListAdapter!!.notifyDataSetChanged()
        }
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
        ClipboardMainWindow.mContext = this.mContext.applicationContext
        mWindowManager = mContext.getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        mWindowManager!!.defaultDisplay.getMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels

        // init base view
        val inflater = LayoutInflater.from(mContext)
        mFloatView = inflater.inflate(R.layout.clipboard_main_layout, null)
        mFloatView!!.setOnClickListener(mFloatViewOnClickListener)
        mFloatView!!.setOnTouchListener(mFloatViewOnTouchListener)
        mFloatViewLayoutParams = WindowManager.LayoutParams()
        mFloatViewLayoutParams!!.format = PixelFormat.TRANSLUCENT
        mFloatViewLayoutParams!!.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        mFloatViewLayoutParams!!.type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        mFloatViewLayoutParams!!.gravity = Gravity.CENTER
        mFloatViewLayoutParams!!.width = (screenWidth * 0.6).toInt()
        mFloatViewLayoutParams!!.height = (screenHeight * 0.6).toInt()
        personSettingsBtn = mFloatView!!.findViewById<ImageView>(R.id.person_settings)

        // init history list
        val msgList = SqliteHelper.helper!!.ListAll()
        mContentList.addAll(msgList)
        mContentListView = mFloatView!!.findViewById(R.id.history_list) as ListView
        mContentListAdapter = HistoryListAdapter(mContext, R.id.history_list_item_content, mContentList)
        mContentListView!!.adapter = mContentListAdapter

        // init popup window
        personSettingsView = inflater.inflate(R.layout.person_settings, null)
        popWindow = PopupWindow(personSettingsView, (screenWidth * 0.4).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT, true)
        personSettingsBtn!!.setOnClickListener(object: View.OnClickListener{
            override fun onClick(v: View?) {
                popWindow!!.showAsDropDown(personSettingsBtn)
            }
        })
    }
}