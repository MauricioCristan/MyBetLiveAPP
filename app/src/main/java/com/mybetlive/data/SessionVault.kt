package com.mybetlive.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SessionVault(context: Context) {
    private val bankReports=java.io.File(context.cacheDir,"bank-reports")
    private val prefs = context.getSharedPreferences("mybet-private", Context.MODE_PRIVATE)
    private val json = Gson()
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("mybet-session", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("mybet-session", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        return Base64.encodeToString(cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }
    private fun decrypt(value: String): String {
        val bytes = Base64.decode(value, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12))) }
        return cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8)
    }
    var session: Session?
        get() = prefs.getString("session", null)?.let { runCatching { json.fromJson(decrypt(it), Session::class.java) }.getOrNull() }
        set(value) { val previous=session;if(previous==null||!previous.sameOwner(value))clearBankCache();prefs.edit().apply { if (value == null) remove("session") else putString("session", encrypt(json.toJson(value))) }.commit() }
    var fcmToken: String?
        get() = prefs.getString("fcm", null)
        set(value) { prefs.edit().putString("fcm", value).apply() }
    val installationId: String
        get() = prefs.getString("installation", null) ?: UUID.randomUUID().toString().also { prefs.edit().putString("installation", it).commit() }
    var cachedBets: List<BetView>
        get() = prefs.getString("bets", null)?.let {
            runCatching { json.fromJson<List<BetView>>(decrypt(it), object : TypeToken<List<BetView>>() {}.type) }.getOrNull()
        } ?: emptyList()
        set(value) { prefs.edit().putString("bets", encrypt(json.toJson(value))).apply() }
    internal fun bankCache(owner:String):String?=prefs.getString("bank-cache",null)?.let{runCatching{decrypt(it)}.getOrNull()}?.takeIf{prefs.getString("bank-cache-owner",null)==owner}
    internal fun saveBankCache(owner:String,value:String){prefs.edit().putString("bank-cache-owner",owner).putString("bank-cache",encrypt(value)).apply()}
    private fun clearBankCache(){prefs.edit().remove("bank-cache-owner").remove("bank-cache").apply();bankReports.listFiles()?.filter{it.isFile&&it.name.startsWith("mybetlive-")}?.forEach{it.delete()}}
    fun clear() { clearBankCache();prefs.edit().remove("session").remove("bets").apply() }
}
