package com.musketeer.superclipboard.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.musketeer.superclipboard.R
import com.musketeer.superclipboard.data.ClipBoardMessage


class HistoryListAdapter: ArrayAdapter<ClipBoardMessage> {
    internal class ViewHolder {
        var iconView: ImageView? = null
        var contentView: TextView? = null
    }

    constructor(ctx: Context, resID: Int, contentList: List<ClipBoardMessage>): super(ctx, resID, contentList)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val contentObj: ClipBoardMessage = getItem(position)!!
        var view: View? = null
        val viewHolder: ViewHolder?
        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.history_list_item_layout, null)
            viewHolder = ViewHolder()
            viewHolder.iconView = view.findViewById(R.id.history_list_item_icon)
            viewHolder.contentView = view.findViewById(R.id.history_list_item_content)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view!!.tag as ViewHolder
        }

        when (contentObj.type) {
            ClipBoardMessage.MessageType.Text -> {
                viewHolder.iconView!!.setImageDrawable(context.getDrawable(R.drawable.ic_text_fields_black_24dp))
                viewHolder.iconView!!.setColorFilter(ContextCompat.getColor(context, R.color.green))
            }
            ClipBoardMessage.MessageType.Image -> {
                viewHolder.iconView!!.setImageDrawable(context.getDrawable(R.drawable.ic_image_black_24dp))
                viewHolder.iconView!!.setColorFilter(ContextCompat.getColor(context, R.color.blue))
            }
        }
        viewHolder.contentView!!.text = contentObj.content
        return view!!
    }
}