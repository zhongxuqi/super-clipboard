package com.musketeer.superclipboard.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.musketeer.superclipboard.R
import com.musketeer.superclipboard.data.ClipBoardMessage


class HistoryListAdapter: ArrayAdapter<ClipBoardMessage> {
    internal class ViewHolder {
        var contentView: TextView? = null
    }

    val expandItemMap = HashSet<Int>()

    constructor(ctx: Context, resID: Int, contentList: List<ClipBoardMessage>): super(ctx, resID, contentList)

    fun isExpand(id: Int): Boolean {
        return expandItemMap.contains(id)
    }

    fun expandItem(id: Int) {
        expandItemMap.add(id)
        notifyDataSetChanged()
    }

    fun foldItem(id: Int) {
        expandItemMap.remove(id)
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val contentObj: ClipBoardMessage = getItem(position)!!
        val view: View?
        val viewHolder: ViewHolder?
        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.history_list_item_layout, null)
            viewHolder = ViewHolder()
            viewHolder.contentView = view.findViewById(R.id.history_list_item_content)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }

        viewHolder.contentView!!.text = contentObj.content
        if (expandItemMap.contains(contentObj.id)) {
            viewHolder.contentView!!.maxLines = 4
        } else {
            viewHolder.contentView!!.maxLines = 2
        }
        return view!!
    }
}