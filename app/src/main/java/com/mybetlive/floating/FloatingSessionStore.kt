package com.mybetlive.floating

import android.content.Context
import org.json.JSONArray

/** Restore only a user-enabled overlay in the same login generation; never stores credentials. */
internal class FloatingSessionStore(context:Context){
    private val prefs=context.getSharedPreferences("floating-session",Context.MODE_PRIVATE)
    data class Saved(val ids:List<String>,val selected:String?)
    fun read(owner:String):Saved?{
        if(prefs.getString("owner",null)!=owner)return null
        val ids=runCatching{val rows=JSONArray(prefs.getString("ids","[]"));(0 until rows.length()).map{rows.getString(it)}}.getOrDefault(emptyList())
        return ids.takeIf{it.isNotEmpty()}?.let{Saved(it,prefs.getString("selected",null))}
    }
    fun save(owner:String,ids:List<String>,selected:String?){prefs.edit().putString("owner",owner).putString("ids",JSONArray(ids).toString()).putString("selected",selected).commit()}
    fun clear(){prefs.edit().clear().commit()}
}
