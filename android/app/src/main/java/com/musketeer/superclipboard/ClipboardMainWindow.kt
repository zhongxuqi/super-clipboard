package com.musketeer.superclipboard

import android.content.ClipData
import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.util.DisplayMetrics
import android.view.*
import android.view.View.OnTouchListener
import android.widget.*
import androidx.constraintlayout.widget.Constraints
import com.google.android.material.switchmaterial.SwitchMaterial
import com.musketeer.superclipboard.adapter.HistoryListAdapter
import com.musketeer.superclipboard.data.ClipBoardMessage
import com.musketeer.superclipboard.db.SqliteHelper
import com.musketeer.superclipboard.net.UdpClient
import java.util.*


class ClipboardMainWindow constructor(val mContext: Context) {
    companion object {
        var Instance: ClipboardMainWindow? = null
    }

    val handler: Handler

    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    private var mIsFloatViewShowing = false
    private var mWindowManager: WindowManager? = null
    private var mFloatView: View? = null
    private var mFloatViewLayoutParams: WindowManager.LayoutParams? =null
    private var mFloatViewLastX = 0
    private var mFloatViewLastY = 0
    private var mFloatViewFirstX = 0
    private var mFloatViewFirstY = 0
    private var personSettingsBtn: ImageView? = null
    private var maxMainView: View? = null
    private var minMainView: View? = null
    val syncStateTextView: TextView

    private var mContentListView: ListView? = null
    private var mContentListAdapter: HistoryListAdapter? = null
    private var mContentList: LinkedList<ClipBoardMessage> = LinkedList()

    private var popWindow: PopupWindow? = null
    private var personSettingsView: View? = null

    private var actionMenuWindow: PopupWindow? = null
    private var actionMenuWindowView: View? = null
    private var actionMenuExpandView: TextView? = null
    private var clipboardMsg: ClipBoardMessage? = null

    fun addMessage(msg: ClipBoardMessage) {
        mContentList.addFirst(msg)
        mContentListAdapter!!.notifyDataSetChanged()
    }

    fun deleteMessage(id: Int) {
        for (msg in mContentList.iterator()) {
            if (msg.id.compareTo(id) == 0) {
                mContentList.remove(msg)
                break
            }
        }
        mContentListAdapter!!.notifyDataSetChanged()
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

    private val mFloatViewOnTouchListener = OnTouchListener { v, event ->
        var ret = false
        val prm = mFloatViewLayoutParams
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mFloatViewLastX = event.rawX.toInt()
                mFloatViewLastY = event.rawY.toInt()
                mFloatViewFirstX = mFloatViewLastX
                mFloatViewFirstY = mFloatViewLastY
                ret = true
            }
            MotionEvent.ACTION_UP -> {
                ret = true
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX.toInt() - mFloatViewLastX
                val deltaY = event.rawY.toInt() - mFloatViewLastY
                mFloatViewLastX = event.rawX.toInt()
                mFloatViewLastY = event.rawY.toInt()
                if (prm != null) {
                    if (prm.x < 0) {
                        prm.x = 0
                    } else if (prm.x >= screenWidth - mFloatView!!.width) {
                        prm.x = screenWidth - mFloatView!!.width
                    }
                    if (prm.x + deltaX in 0 until screenWidth - mFloatView!!.width) {
                        prm.x += deltaX
                    }
                    if (prm.y < 0) {
                        prm.y = 0
                    } else if (prm.y >= screenHeight - mFloatView!!.height) {
                        prm.y = screenHeight - mFloatView!!.height
                    }
                    if (prm.y + deltaY in 0 until screenHeight - mFloatView!!.height) {
                        prm.y += deltaY
                    }
                    if (mWindowManager != null) {
                        mWindowManager!!.updateViewLayout(mFloatView, prm)
                    }
                }
                ret = true
            }
            else -> {
            }
        }
        ret
    }

    init {
        ClipboardMainWindow.Instance = this

        handler = Handler()
        mWindowManager = mContext.getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        mWindowManager!!.defaultDisplay.getMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels

        // init base view
        val inflater = LayoutInflater.from(mContext)
        mFloatView = inflater.inflate(R.layout.clipboard_main_layout, null)
        mFloatView!!.setOnTouchListener(mFloatViewOnTouchListener)
        personSettingsBtn = mFloatView!!.findViewById(R.id.person_settings)

        maxMainView = mFloatView!!.findViewById(R.id.max_view)
        maxMainView!!.layoutParams = Constraints.LayoutParams((screenWidth * 0.6).toInt(), (screenHeight * 0.6).toInt())
        maxMainView!!.visibility = View.VISIBLE
        maxMainView!!.findViewById<ImageView>(R.id.btn_window_min).setOnClickListener {
            maxMainView!!.visibility = View.GONE
            minMainView!!.visibility = View.VISIBLE
        }
        maxMainView!!.findViewById<ImageView>(R.id.btn_window_close).setOnClickListener {
            dismissFloatView()
        }
        syncStateTextView = maxMainView!!.findViewById(R.id.sync_state_desc)
        maxMainView!!.findViewById<SwitchMaterial>(R.id.sync_switcher).setOnCheckedChangeListener(object: CompoundButton.OnCheckedChangeListener{
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                if (isChecked) {
                    UdpClient.Instance!!.start()
                    syncStateTextView.setTextColor(mContext.resources.getColor(R.color.blue))
                } else {
                    UdpClient.Instance!!.close()
                    syncStateTextView.setTextColor(mContext.resources.getColor(R.color.grey))
                    syncStateTextView.text = mContext.getText(R.string.all_platform_sync)
                }
            }
        })

        mFloatViewLayoutParams = WindowManager.LayoutParams()
        mFloatViewLayoutParams!!.format = PixelFormat.TRANSLUCENT
        mFloatViewLayoutParams!!.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        mFloatViewLayoutParams!!.type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        mFloatViewLayoutParams!!.width = WindowManager.LayoutParams.WRAP_CONTENT
        mFloatViewLayoutParams!!.height = WindowManager.LayoutParams.WRAP_CONTENT
        mFloatViewLayoutParams!!.gravity = Gravity.TOP or Gravity.START
        mFloatViewLayoutParams!!.x = screenWidth - maxMainView!!.layoutParams.width
        mFloatViewLayoutParams!!.y = 0

        minMainView = mFloatView!!.findViewById(R.id.min_view)
        minMainView!!.visibility = View.GONE
        minMainView!!.findViewById<ImageView>(R.id.btn_window_max).setOnClickListener {
            maxMainView!!.visibility = View.VISIBLE
            minMainView!!.visibility = View.GONE
        }

        // init history list
        val msgList = SqliteHelper.helper!!.ListAll()
        mContentList.addAll(msgList)
        mContentListView = mFloatView!!.findViewById(R.id.history_list) as ListView
        mContentListAdapter = HistoryListAdapter(mContext, R.id.history_list_item_content, mContentList)
        mContentListView!!.adapter = mContentListAdapter
        mContentListView!!.setOnItemClickListener(object: AdapterView.OnItemClickListener{
            override fun onItemClick(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val clipboardMsg = mContentList[position]
                MainService.instance!!.skipNum++
                MainService.instance!!.manager.setPrimaryClip(ClipData.newPlainText(clipboardMsg.content, clipboardMsg.content))
                Toast.makeText(mContext, R.string.copied, Toast.LENGTH_SHORT).show()
            }
        })
        mContentListView!!.setOnItemLongClickListener(object: AdapterView.OnItemLongClickListener{
            override fun onItemLongClick(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ): Boolean {
                clipboardMsg = mContentList[position]
                if (mContentListAdapter!!.hasExpandItem(clipboardMsg!!.id)) {
                    actionMenuExpandView!!.text = mContext.getText(R.string.fold)
                } else {
                    actionMenuExpandView!!.text = mContext.getText(R.string.expand)
                }
                actionMenuWindow!!.showAsDropDown(view)
                return true
            }
        })

        // init popup window
        personSettingsView = inflater.inflate(R.layout.person_settings, null)
        popWindow = PopupWindow(personSettingsView, (screenWidth * 0.4).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT, true)
        personSettingsBtn!!.setOnClickListener(object: View.OnClickListener{
            override fun onClick(v: View?) {
                popWindow!!.showAsDropDown(personSettingsBtn)
            }
        })

        // init action menu window
        actionMenuWindowView = inflater.inflate(R.layout.action_menu, null)
        actionMenuExpandView = actionMenuWindowView!!.findViewById(R.id.expand_text) as TextView
        actionMenuWindow = PopupWindow(actionMenuWindowView, (screenWidth * 0.4).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT, true)
        actionMenuWindowView!!.findViewById<View>(R.id.action_menu_detail).setOnClickListener(object: View.OnClickListener{
            override fun onClick(v: View?) {
                actionMenuWindow!!.dismiss()
                if (mContentListAdapter!!.hasExpandItem(clipboardMsg!!.id)) {
                    mContentListAdapter!!.removeExpandItem(clipboardMsg!!.id)
                } else {
                    mContentListAdapter!!.addExpandItem(clipboardMsg!!.id)
                }
            }
        })
        actionMenuWindowView!!.findViewById<View>(R.id.action_menu_delete).setOnClickListener(object: View.OnClickListener{
            override fun onClick(v: View?) {
                actionMenuWindow!!.dismiss()
                SqliteHelper.helper!!.Delete(clipboardMsg!!.id)
                deleteMessage(clipboardMsg!!.id)
            }
        })
    }
}