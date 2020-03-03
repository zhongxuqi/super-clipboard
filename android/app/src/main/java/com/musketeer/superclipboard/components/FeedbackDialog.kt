package com.musketeer.superclipboard.components

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import com.musketeer.superclipboard.R
import com.musketeer.superclipboard.net.HttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject


class FeedbackDialog {
    companion object {
        var dialog: AlertDialog? = null
        var editText: EditText? = null

        fun showDialog(ctx: Context) {
            val builder = AlertDialog.Builder(ctx)
            val v = LayoutInflater.from(ctx).inflate(R.layout.dialog_feedback, null)
            editText = v.findViewById(R.id.feedback_text)
            v.findViewById<View>(R.id.feedback_cancel).setOnClickListener(object: View.OnClickListener{
                override fun onClick(v: View?) {
                    dialog?.dismiss()
                }
            })
            v.findViewById<View>(R.id.feedback_submit).setOnClickListener(object: View.OnClickListener{
                override fun onClick(v: View?) {
                    dialog?.dismiss()

                    if (editText!!.text.isEmpty()) return;
                    var jsonObject = JSONObject();
                    jsonObject.put("app_id", HttpClient.AppID)
                    jsonObject.put("type", 1)
                    jsonObject.put("context", "")
                    jsonObject.put("message", editText!!.text)
                    val body: RequestBody = jsonObject.toString()
                        .toRequestBody("application/json; charset=utf-8".toMediaType())
                    val request: Request = Request.Builder()
                        .url("${HttpClient.Host}${HttpClient.FeedbackUrl}")
                        .post(body)
                        .build()
                    HttpClient.client.newCall(request).execute()
                        .use { response -> {
                            Toast.makeText(ctx, R.string.think_feedback, Toast.LENGTH_SHORT).show()
                        } }
                }
            })
            builder.setView(v)
            val dialog = builder.create()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                dialog.window!!.setType((WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY));
            } else {
                dialog.window!!.setType((WindowManager.LayoutParams.TYPE_SYSTEM_ALERT));
            }
            FeedbackDialog.dialog = dialog
            dialog.show()
        }
    }
}