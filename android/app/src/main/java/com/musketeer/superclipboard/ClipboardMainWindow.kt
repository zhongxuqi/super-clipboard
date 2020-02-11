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

    private val screenWidth: Int
    private val screenHeight: Int

    private var mIsFloatViewShowing = false
    private val mWindowManager: WindowManager
    private val mFloatView: View
    private val mFloatViewLayoutParams: WindowManager.LayoutParams
    private var mFloatViewLastX = 0
    private var mFloatViewLastY = 0
    private var mFloatViewFirstX = 0
    private var mFloatViewFirstY = 0
    private val personSettingsBtn: ImageView
    private val maxMainView: View
    private val minMainView: View
    val syncStateTextView: TextView

    private val mContentListView: ListView
    private val mContentListAdapter: HistoryListAdapter
    private val mContentList: LinkedList<ClipBoardMessage> = LinkedList()

    private val popWindow: PopupWindow
    private val personSettingsView: View

    private val actionMenuWindow: PopupWindow
    private val actionMenuWindowView: View
    private var clipboardMsg: ClipBoardMessage? = null

    fun addMessage(msg: ClipBoardMessage) {
        mContentList.addFirst(msg)
        mContentListAdapter.notifyDataSetChanged()
    }

    fun deleteMessage(id: Int) {
        for (msg in mContentList.iterator()) {
            if (msg.id.compareTo(id) == 0) {
                mContentList.remove(msg)
                break
            }
        }
        mContentListAdapter.notifyDataSetChanged()
    }

    fun dismissFloatView() {
        if (mIsFloatViewShowing) {
            mIsFloatViewShowing = false
            mWindowManager.removeViewImmediate(mFloatView)
        }
    }

    fun showFloatView() {
        if (!mIsFloatViewShowing) {
            mIsFloatViewShowing = true
            mWindowManager.addView(mFloatView, mFloatViewLayoutParams)
        }
    }

    init {
        Instance = this

        handler = Handler()
        mWindowManager = mContext.getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        mWindowManager.defaultDisplay.getMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels

        // init base view
        val inflater = LayoutInflater.from(mContext)
        mFloatView = inflater.inflate(R.layout.clipboard_main_layout, null)
        personSettingsBtn = mFloatView.findViewById(R.id.person_settings)

        maxMainView = mFloatView.findViewById(R.id.max_view)
        maxMainView.layoutParams = Constraints.LayoutParams((screenWidth * 0.6).toInt(), (screenHeight * 0.6).toInt())
        maxMainView.visibility = View.VISIBLE
        maxMainView.findViewById<ImageView>(R.id.btn_window_close).setOnClickListener {
            dismissFloatView()
        }
        syncStateTextView = maxMainView.findViewById(R.id.sync_state_desc)
        maxMainView.findViewById<SwitchMaterial>(R.id.sync_switcher).setOnCheckedChangeListener(object: CompoundButton.OnCheckedChangeListener{
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
        mFloatViewLayoutParams.format = PixelFormat.TRANSLUCENT
        mFloatViewLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        mFloatViewLayoutParams.type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        mFloatViewLayoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
        mFloatViewLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        mFloatViewLayoutParams.gravity = Gravity.TOP or Gravity.START
        mFloatViewLayoutParams.x = screenWidth - maxMainView.layoutParams.width
        mFloatViewLayoutParams.y = 0

        mFloatView.setOnTouchListener(object: View.OnTouchListener{
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event == null) return false
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
                        if (prm.x < 0) {
                            prm.x = 0
                        } else if (prm.x >= screenWidth - mFloatView.width) {
                            prm.x = screenWidth - mFloatView.width
                        }
                        if (prm.x + deltaX in 0 until screenWidth - mFloatView.width) {
                            prm.x += deltaX
                        }
                        if (prm.y < 0) {
                            prm.y = 0
                        } else if (prm.y >= screenHeight - mFloatView.height) {
                            prm.y = screenHeight - mFloatView.height
                        }
                        if (prm.y + deltaY in 0 until screenHeight - mFloatView.height) {
                            prm.y += deltaY
                        }
                        mWindowManager.updateViewLayout(mFloatView, prm)
                        ret = true
                    }
                    else -> {
                    }
                }
                return ret
            }
        })

        minMainView = mFloatView.findViewById(R.id.min_view)
        minMainView.visibility = View.GONE
        minMainView.findViewById<ImageView>(R.id.btn_window_max).setOnClickListener {
            maxMainView.visibility = View.VISIBLE
            minMainView.visibility = View.GONE
        }
        maxMainView.findViewById<ImageView>(R.id.btn_window_min).setOnClickListener {
            maxMainView.visibility = View.GONE
            minMainView.visibility = View.VISIBLE
        }

        // init history list
        val msgList = SqliteHelper.helper!!.ListAll()
        mContentList.addAll(msgList)
        mContentListView = mFloatView.findViewById(R.id.history_list) as ListView
        mContentListAdapter = HistoryListAdapter(mContext, R.id.history_list_item_content, mContentList)
        mContentListView.adapter = mContentListAdapter
        mContentListView.setOnItemClickListener(object: AdapterView.OnItemClickListener{
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

        // init popup window
        personSettingsView = inflater.inflate(R.layout.person_settings, null)
        popWindow = PopupWindow(personSettingsView, (screenWidth * 0.4).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT, true)
        personSettingsBtn.setOnClickListener(object: View.OnClickListener{
            override fun onClick(v: View?) {
                popWindow.showAsDropDown(personSettingsBtn)
            }
        })

        // init action menu window
        actionMenuWindowView = inflater.inflate(R.layout.action_menu, null)
        actionMenuWindow = PopupWindow(actionMenuWindowView, (screenWidth * 0.4).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT, true)
        actionMenuWindowView.findViewById<View>(R.id.action_menu_delete).setOnClickListener(object: View.OnClickListener{
            override fun onClick(v: View?) {
                actionMenuWindow.dismiss()
                SqliteHelper.helper!!.Delete(clipboardMsg!!.id)
                deleteMessage(clipboardMsg!!.id)
            }
        })


        mContentListView.setOnItemLongClickListener(object: AdapterView.OnItemLongClickListener{
            override fun onItemLongClick(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ): Boolean {
                clipboardMsg = mContentList[position]
                actionMenuWindow.showAsDropDown(view)
                return true
            }
        })

        // init udp listener
        UdpClient.Instance!!.listener = object: UdpClient.Listener{
            override fun onChangeDeviceNum(deviceNum: Int) {
                Instance?.handler?.post {
                    Instance?.syncStateTextView?.text = String.format(Instance?.mContext!!.getString(R.string.device_total, deviceNum))
                }
            }

            override fun onReceiveMsg(msg: ClipBoardMessage) {
                Instance?.handler?.post {
                    MainService.instance?.insertMessage(msg)
                }
            }
        }
    }
}