package com.musketeer.superclipboard.utils

import android.content.Context
import android.util.Log

enum class UserType(val v: Int) {
    UserTypeUnknow(0),
    UserTypeAccount(1),
    UserTypeQQ(2)
}

object SharePreference {
    val Name = "super-clipboard"
    val UserTypeKey = "user-type"
    val UserIDKey = "user-id"

    fun getUserTypeByValue(v: Int): UserType? {
        return when(v) {
            UserType.UserTypeAccount.v -> {
                UserType.UserTypeAccount
            }
            UserType.UserTypeQQ.v -> {
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