package com.example.ppapb_uas.database

import android.content.Context
import android.content.SharedPreferences

class PrefManager(context: Context) {
    companion object {
        private const val PREF_NAME = "AppPreferences"
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val editor: SharedPreferences.Editor = sharedPreferences.edit()

    fun setUser(userId:String){
        editor.putString("userId", userId).apply()
    }

    fun getUser():String?{
        return sharedPreferences.getString("userId", null)
    }

    fun clear(){
        editor.clear().apply()
    }
}