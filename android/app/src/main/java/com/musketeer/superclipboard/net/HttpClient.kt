package com.musketeer.superclipboard.net

import okhttp3.OkHttpClient

object HttpClient {
//    val Domain = "192.168.0.103"
//    val Host = "http://${Domain}:8000"

    val Domain = "www.superclipboard.online"
    val Host = "https://${Domain}"

    val FeedbackUrl = "/openapi/codeutils"
    val AppID = 4
    val client = OkHttpClient()
}