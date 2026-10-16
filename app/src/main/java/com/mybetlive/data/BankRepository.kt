package com.mybetlive.data
import com.mybetlive.application.BankOperations
import com.google.gson.Gson
class BankRepository(private val account:TrackingRepository):BankOperations {
 private val json=Gson()
 override fun owner():String?=account.session?.let { session ->
  java.security.MessageDigest.getInstance("SHA-256").digest((session.baseUrl+"|"+session.username+"|"+session.generation).toByteArray()).joinToString(""){"%02x".format(it)}
 }
 override fun cached():BankCache {val key=owner()?:return BankCache();return account.vault.bankCache(key)?.let{runCatching{json.fromJson(it,BankCache::class.java)}.getOrNull()}?:BankCache()}
 override suspend fun <T> call(action:suspend(BankApi)->T):T=account.authorizedClient(BankApi::class.java,action)
 override fun remember(cache:BankCache){owner()?.let{account.vault.saveBankCache(it,json.toJson(cache))}}
}
