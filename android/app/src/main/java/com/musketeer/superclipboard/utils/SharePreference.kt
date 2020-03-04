package com.musketeer.superclipboard.utils

import android.content.Context

enum class UserType(val v: Int) {
    UserTypeUnknow(0),
    UserTypeQQ(1)
}

object SharePreference {
    val Name = "super-clipboard"
    val UserTypeKey = "user-type"
    val UserIDKey = "user-id"

    fun getUserTypeByValue(v: Int): UserType? {
        return when(v) {
            1 -> {
                UserType.UserTypeQQ
            }
            else -> {
                UserType.UserTypeUnknow
            }
        }
    }

    fun setUserType(ctx: Context, userType: UserType) {
        val sp = ctx.getSharedPreferences(Name, Context.MODE_PRIVATE)
        val editor = sp.edit()
        editor.putInt(UserTypeKey, userType.v)
        editor.apply()
    }

    fun getUserType(ctx: Context): UserType? {
        val sp = ctx.getSharedPreferences(Name, Context.MODE_PRIVATE)
        return getUserTypeByValue(sp.getInt(UserTypeKey, 0))
    }

    fun setUserID(ctx: Context, userID: String) {
        val sp = ctx.getSharedPreferences(Name, Context.MODE_PRIVATE)
        val editor = sp.edit()
        editor.putString(UserIDKey, userID)
        editor.apply()
    }

    fun getUserID(ctx: Context): String {
        val sp = ctx.getSharedPreferences(Name, Context.MODE_PRIVATE)
        return sp.getString(UserIDKey, "")!!
    }
}