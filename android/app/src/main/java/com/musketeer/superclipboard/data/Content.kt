package com.musketeer.superclipboard.data

class Content {
    enum class ContentType(val t: Int) {
        Text(1),
        Image(2)
    }

    val type: ContentType
    val content: String
    val extra: String
    val createTime: Long
    val updateTime: Long

    constructor(type: ContentType, content: String, extra: String, createTime: Long, updateTime: Long) {
        this.type = type
        this.content = content
        this.extra = extra
        this.createTime = createTime
        this.updateTime = updateTime
    }
}