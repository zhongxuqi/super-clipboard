package com.musketeer.superclipboard.net

import okhttp3.OkHttpClient

class HttpClient {
    companion object {
//        val Host = "https://www.easypass.tech"
        val Host = "http://192.168.0.103:8000"
        val FeedbackUrl = "/openapi/codeutils"
        val AppID = 4
        val client = OkHttpClient()
    }
}