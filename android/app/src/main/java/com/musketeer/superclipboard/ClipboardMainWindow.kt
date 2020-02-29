package com.musketeer.superclipboard

import android.animation.ValueAnimator
import android.content.ClipData
import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
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
    private var mFloatViewMove = false
    private val personSettingsBtn: ImageView
    private val maxMainView: View
    val minMainView: View
    private val syncSwitcher: SwitchMaterial
    val syncStateTextView: TextView
    private val keywordInput: EditText
    private val keywordClear: View
    private val receiveMsgNotice: TextView
    private val receiveMsgValueAnimator: ValueAnimator

    private val mAllContentList: LinkedList<ClipBoardMessage> = LinkedList()
    private val mContentListView: ListView
    private val mContentListAdapter: HistoryListAdapter
    private val mContentList: LinkedList<ClipBoardMessage> = LinkedList()

    private val popWindow: PopupWindow
    private val personSettingsView: View

    private val actionMenuWindow: PopupWindow
    private val actionMenuWindowView: View
    private val actionMenuSyncView: View
    private val actionMenuExpandView: View
    private val actionMenuFoldView: View
    private var clipboardMsg: ClipBoardMessage? = null

    private val confirmCLoseWindow: PopupWindow

    fun minWindow() {
        val prm = mFloatViewLayoutParams
        if (prm.x + maxMainView.width / 2 > screenWidth / 2) {
            prm.x = screenWidth
        } else {
            prm.x = 0
        }
        prm.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        maxMainView.visibility = View.GONE
        minMainView.visibility = View.VISIBLE
        mWindowManager.updateViewLayout(mFloatView, prm)
    }

    fun getSameMessage(msg: ClipBoardMessage): ClipBoardMessage? {
        for (contentItem in mContentList) {
            if (contentItem.content == msg.content) return contentItem
        }
        return null
    }

    fun topMessage(msg: ClipBoardMessage) {
        for (item in mContentList.iterator()) {
            if (item.id.compareTo(msg.id) == 0) {
                mContentList.remove(msg)
                mContentList.addFirst(msg)
                mContentListAdapter.notifyDataSetChanged()
                return
            }
        }
    }

    fun addMessage(msg: ClipBoardMessage) {
        mAllContentList.addFirst(msg)
        val keyword = keywordInput.text.toString()
        if (keyword == "" || msg.content.indexOf(keyword) >= 0) {
            mContentList.addFirst(msg)
            mContentListAdapter.notifyDataSetChanged()
        }
    }

    fun refreshKeyword() {
        mContentList.clear()
        mContentList.addAll(mAllContentList.filter {
            val keyword = keywordInput.text.toString()
            keyword == "" || it.content.contains(keyword)
        })
        mContentListAdapter.notifyDataSetChanged()
    }

    fun deleteMessage(id: Int) {
        for (msg in mAllContentList.iterator()) {
            if (msg.id.compareTo(id) == 0) {
                mContentList.remove(msg)
                break
            }
        }
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
        mFloatViewLayoutParams.flags = WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        if (!mIsFloatViewShowing) {
            mIsFloatViewShowing = true
            mWindowManager.addView(mFloatView, mFloatViewLayoutParams)
            maxMainView.visibility = View.VISIBLE
            minMainView.visibility = View.GONE
        } else {
            maxMainView.visibility = View.VISIBLE
            minMainView.visibility = View.GONE
            mWindowManager.updateViewLayout(mFloatView, mFloatViewLayoutParams)

        }
    }

    init {
        Instance = this


        // init consts
        handler = Handler()
        mWindowManager = mContext.getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        mWindowManager.defaultDisplay.getMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        val actionDoneValueAnimator = ValueAnimator.ofArgb(
            mContext.resources.getColor(R.color.transparent),
            mContext.resources.getColor(R.color.light_green),
            mContext.resources.getColor(R.color.light_green),
            mContext.resources.getColor(R.color.light_green),
            mContext.resources.getColor(R.color.light_green),
            mContext.resources.getColor(R.color.light_green),
            mContext.resources.getColor(R.color.light_green),
            mContext.resources.getColor(R.color.transparent)
        )
        actionDoneValueAnimator.duration = 2000
        val receiveMsgMaxLenght = (screenWidth * 0.4).toInt()
        receiveMsgValueAnimator = ValueAnimator.ofInt(0, receiveMsgMaxLenght, receiveMsgMaxLenght, receiveMsgMaxLenght, receiveMsgMaxLenght, receiveMsgMaxLenght, receiveMsgMaxLenght, receiveMsgMaxLenght, receiveMsgMaxLenght, 0)
        receiveMsgValueAnimator.duration = 2000

        // init base view
        val inflater = LayoutInflater.from(mContext)
        mFloatView = inflater.inflate(R.layout.clipboard_main_layout, null)
        personSettingsBtn = mFloatView.findViewById(R.id.person_settings)

        maxMainView = mFloatView.findViewById(R.id.max_view)
        maxMainView.layoutParams = Constraints.LayoutParams((screenWidth * 0.6).toInt(), screenWidth)
        maxMainView.visibility = View.VISIBLE
        syncStateTextView = maxMainView.findViewById(R.id.sync_state_desc)
        keywordInput = maxMainView.findViewById(R.id.keyword_input)
        keywordClear = maxMainView.findViewById(R.id.keyword_clear)
        keywordClear.visibility = View.GONE
        keywordClear.setOnClickListener {
            keywordInput.setText("")
            refreshKeyword()
        }

        mFloatViewLayoutParams = WindowManager.LayoutParams()
        mFloatViewLayoutParams.format = PixelFormat.TRANSLUCENT
        mFloatViewLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        mFloatViewLayoutParams.type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        mFloatViewLayoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
        mFloatViewLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        mFloatViewLayoutParams.gravity = Gravity.TOP or Gravity.START
        mFloatViewLayoutParams.x = screenWidth - maxMainView.layoutParams.width
        mFloatViewLayoutParams.y = 0
        mFloatViewLayoutParams.flags = WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED

        val onTouchListener = object: View.OnTouchListener{
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
                        mFloatViewMove = false
                        ret = true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!mFloatViewMove) {
                            v?.performClick()
                        } else {
                            if (v != null) {
                                var valueAnimator: ValueAnimator? = null
                                if (v.width / 2 + prm.x > screenWidth / 2) {
                                    valueAnimator = ValueAnimator.ofInt(prm.x, screenWidth - v.width)
                                    valueAnimator.duration = (1000F * (screenWidth - v.width - prm.x) / screenWidth).toLong()
                                } else {
                                    valueAnimator = ValueAnimator.ofInt(prm.x, 0)
                                    valueAnimator.duration = (1000F * prm.x / screenWidth).toLong()
                                }
                                valueAnimator.addUpdateListener(object: ValueAnimator.AnimatorUpdateListener {
                                    override fun onAnimationUpdate(animation: ValueAnimator?) {
                                        if (animation != null) {
                                            mFloatViewLayoutParams.x = animation.animatedValue.toString().toInt()
                                            mWindowManager.updateViewLayout(mFloatView, mFloatViewLayoutParams)
                                        }
                                    }
                                })
                                valueAnimator.start()
                            }
                        }
                        ret = true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.rawX.toInt() - mFloatViewLastX
                        val deltaY = event.rawY.toInt() - mFloatViewLastY
                        mFloatViewLastX = event.rawX.toInt()
                        mFloatViewLastY = event.rawY.toInt()
                        if (kotlin.math.abs(mFloatViewLastX - mFloatViewFirstX) > 5 && kotlin.math.abs(mFloatViewLastY - mFloatViewFirstY) > 5) {
                            mFloatViewMove = true
                        }
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
        }
        mFloatView.setOnTouchListener(onTouchListener)

        minMainView = mFloatView.findViewById(R.id.min_view)
        minMainView.visibility = View.GONE
        receiveMsgNotice = minMainView.findViewById(R.id.receive_msg_notice)
        val minMainViewBtn = minMainView.findViewById<ImageView>(R.id.btn_window_max)
        minMainViewBtn.setOnClickListener {
            mFloatViewLayoutParams.flags = WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            maxMainView.visibility = View.VISIBLE
            minMainView.visibility = View.GONE
            mWindowManager.updateViewLayout(mFloatView, mFloatViewLayoutParams)
        }
        minMainViewBtn.setOnTouchListener(onTouchListener)
        val maxMainViewBtn = maxMainView.findViewById<ImageView>(R.id.btn_window_min)
        maxMainViewBtn.setOnClickListener {
            minWindow()
        }
        maxMainViewBtn.setOnTouchListener(onTouchListener)

        val confirmCLoseWindowView = inflater.inflate(R.layout.confirm_close, null)
        confirmCLoseWindow = PopupWindow(confirmCLoseWindowView, (screenWidth * 0.4).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT, true)
        confirmCLoseWindowView.findViewById<TextView>(R.id.confirm_close_cancel).setOnClickListener {
            confirmCLoseWindow.dismiss()
        }
        confirmCLoseWindowView.findViewById<TextView>(R.id.confirm_close_confirm).setOnClickListener {
            confirmCLoseWindow.dismiss()
            dismissFloatView()
        }
        val closeMainViewBtn = maxMainView.findViewById<ImageView>(R.id.btn_window_close)
        closeMainViewBtn.setOnClickListener {
            confirmCLoseWindow.showAsDropDown(it, 0, 0, Gravity.START)
        }
        closeMainViewBtn.setOnTouchListener(onTouchListener)

        // init history list
        val msgList = SqliteHelper.helper!!.ListAll()
        mAllContentList.addAll(msgList)
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
                actionDoneValueAnimator.end()
                actionDoneValueAnimator.removeAllUpdateListeners()
                actionDoneValueAnimator.addUpdateListener {
                    view?.setBackgroundColor(it.animatedValue.toString().toInt())
                }
                actionDoneValueAnimator.start()
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
        var itemView: View? = null
        actionMenuWindowView = inflater.inflate(R.layout.action_menu, null)
        actionMenuWindow = PopupWindow(actionMenuWindowView, (screenWidth * 0.4).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT, true)
        actionMenuSyncView = actionMenuWindowView.findViewById(R.id.action_menu_sync)
        actionMenuSyncView.setOnClickListener(object: View.OnClickListener{
            override fun onClick(v: View?) {
                actionMenuWindow.dismiss()
                if (UdpClient.Instance == null || !UdpClient.Instance!!.isRunning) return
                UdpClient.Instance?.sendClipboardMsg(clipboardMsg!!)
                actionDoneValueAnimator.end()
                actionDoneValueAnimator.removeAllUpdateListeners()
                val view = itemView
                if (view != null) {
                    actionDoneValueAnimator.addUpdateListener {
                        v?.setBackgroundColor(it.animatedValue.toString().toInt())
                    }
                    actionDoneValueAnimator.start()
                }
            }
        })
        if (!UdpClient.Instance!!.isRunning) {
            actionMenuSyncView.visibility = View.GONE
        }
        actionMenuWindowView.findViewById<View>(R.id.action_menu_delete).setOnClickListener(object: View.OnClickListener{
            override fun onClick(v: View?) {
                actionMenuWindow.dismiss()
                SqliteHelper.helper!!.Delete(clipboardMsg!!.id)
                deleteMessage(clipboardMsg!!.id)
            }
        })
        actionMenuExpandView = actionMenuWindowView.findViewById(R.id.action_menu_expand)
        actionMenuExpandView.setOnClickListener(object: View.OnClickListener{
            override fun onClick(v: View?) {
                mContentListAdapter.expandItem(clipboardMsg!!.id)
                actionMenuWindow.dismiss()
            }
        })
        actionMenuFoldView = actionMenuWindowView.findViewById(R.id.action_menu_fold)
        actionMenuFoldView.setOnClickListener(object: View.OnClickListener{
            override fun onClick(v: View?) {
                mContentListAdapter.foldItem(clipboardMsg!!.id)
                actionMenuWindow.dismiss()
            }
        })

        // init events
        syncSwitcher = maxMainView.findViewById(R.id.sync_switcher)
        syncSwitcher.isChecked = UdpClient.Instance!!.isRunning
        syncSwitcher.setOnCheckedChangeListener(object: CompoundButton.OnCheckedChangeListener{
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                if (isChecked) {
                    UdpClient.Instance!!.start()
                    syncStateTextView.setTextColor(mContext.resources.getColor(R.color.blue))
                    actionMenuSyncView.visibility = View.VISIBLE
                } else {
                    UdpClient.Instance!!.close()
                    syncStateTextView.setTextColor(mContext.resources.getColor(R.color.grey))
                    syncStateTextView.text = mContext.getText(R.string.content_sync)
                    actionMenuSyncView.visibility = View.GONE
                }
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
                actionMenuWindow.showAsDropDown(view, 0, 0, Gravity.END)
                itemView = view
                if (mContentListAdapter.isExpand(clipboardMsg!!.id)) {
                    actionMenuExpandView.visibility = View.GONE
                    actionMenuFoldView.visibility = View.VISIBLE
                } else {
                    actionMenuExpandView.visibility = View.VISIBLE
                    actionMenuFoldView.visibility = View.GONE
                }
                return true
            }
        })
        keywordInput.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                refreshKeyword()
                if (s.toString().isNotEmpty()) {
                    keywordClear.visibility = View.VISIBLE
                } else {
                    keywordClear.visibility = View.GONE
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
        })

        // init udp listener
        UdpClient.listener = object: UdpClient.Listener{
            override fun onChangeDeviceNum(deviceNum: Int) {
                Instance?.handler?.post {
                    Instance?.syncStateTextView?.text = String.format(Instance?.mContext!!.getString(R.string.device_total, deviceNum))
                }
            }

            override fun onReceiveMsg(msg: ClipBoardMessage) {
                Instance?.handler?.post(object: Runnable{
                    override fun run() {
                        MainService.instance?.insertMessage(msg)
                        showContent(msg.content)
                    }
                })
            }
        }
    }

    fun showContent(content: String) {
        if (minMainView.visibility != View.VISIBLE) return
        receiveMsgNotice.text = content
        receiveMsgValueAnimator.end()
        receiveMsgValueAnimator.removeAllUpdateListeners()
        receiveMsgValueAnimator.addUpdateListener {
            receiveMsgNotice.width = it.animatedValue.toString().toInt()
        }
        receiveMsgValueAnimator.start()
    }
}