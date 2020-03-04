package com.musketeer.superclipboard.net

import okhttp3.OkHttpClient

class HttpClient {
    companion object {
        val Host = "https://www.easypass.tech"
        val FeedbackUrl = "/openapi/codeutils"
        val AppID = 4
        val client = OkHttpClient()
    }
}