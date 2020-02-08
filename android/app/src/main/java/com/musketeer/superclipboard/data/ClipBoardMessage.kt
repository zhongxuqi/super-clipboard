package com.musketeer.superclipboard.data

class ClipBoardMessage {
    enum class MessageType(val t: Int) {
        Unknow(0),
        Text(1),
        Image(2)
    }

    companion object {
        fun ToMessageType(t: Int): MessageType {
            when (t) {
                1 -> {
                    return MessageType.Text
                }
                2 -> {
                    return MessageType.Image
                }
            }
            return MessageType.Unknow
        }

        fun FromMessageType(t: MessageType): Int {
            when (t) {
                MessageType.Text -> {
                    return 1
                }
                MessageType.Image -> {
                    return 2
                }
            }
            return 0
        }
    }

    var id: Int
    var type: MessageType
    var content: String
    var extra: String
    var createTime: Long
    var updateTime: Long

    constructor(id: Int, type: MessageType, content: String, extra: String, createTime: Long, updateTime: Long) {
        this.id = id
        this.type = type
        this.content = content
        this.extra = extra
        this.createTime = createTime
        this.updateTime = updateTime
    }

    fun cloneMessage(): ClipBoardMessage {
        return ClipBoardMessage(this.id, this.type, this.content, this.extra, this.createTime, this.updateTime)
    }
}