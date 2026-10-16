package com.mybetlive.data
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

internal fun Session.sameOwner(current: Session?): Boolean = current != null && current.baseUrl == baseUrl &&
    current.username == username && current.generation == generation

/** Token refresh and response ownership are independent of Android storage and SDKs. */
internal class AuthorizedCalls(private val session: () -> Session?,private val save: (Session) -> Unit,
    private val api: (String) -> TrackingApi,private val expire:(Session)->Unit = {}) {
    private val refreshLock=Mutex()
    private fun ensureOwner(started:Session){if(!started.sameOwner(session()))throw CancellationException("La cuenta cambió durante la consulta")}
    suspend fun <T> execute(action:suspend (TrackingApi)->T):T {
        return executeApi(api,action)
    }
    suspend fun <A,T> executeApi(factory:(String)->A,action:suspend(A)->T):T {
        val started=session() ?: error("Inicia sesión")
        if(accessTokenNeedsRenewal(started.tokens.accessToken))renew(started)
        val sentAccess=session()?.tokens?.accessToken ?: started.tokens.accessToken
        try {val value=action(factory(started.baseUrl));ensureOwner(started);return value;}
        catch(error:Exception) {
            ensureOwner(started)
            if(error !is HttpException||error.code()!=401)throw error
            // Capture the credential that actually failed (it may already have been renewed above).
            val failedAccess=error.response()?.raw()?.request?.header("Authorization")?.removePrefix("Bearer ") ?: sentAccess
            renew(started,failedAccess)
            val retried=session() ?: throw error
            try {val value=action(factory(started.baseUrl));ensureOwner(started);return value}
            catch(retry:HttpException) {
                ensureOwner(started)
                if(retry.code()==401 && session()?.tokens?.accessToken==retried.tokens.accessToken)expired(started)
                throw retry
            }
        }
    }

    private suspend fun renew(started:Session,failedAccess:String=started.tokens.accessToken) {
        refreshLock.withLock {
            ensureOwner(started);val current=session() ?: error("Inicia sesión")
            if(current.tokens.accessToken!=failedAccess)return@withLock
            try {
                val tokens=api(current.baseUrl).refresh(Refresh(current.tokens.refreshToken))
                ensureOwner(started)
                if(tokens.accessToken.isNullOrBlank()||tokens.refreshToken.isNullOrBlank())throw java.io.IOException("Respuesta de sesión incompleta")
                save(current.copy(tokens=tokens))
            } catch(error:HttpException) {
                ensureOwner(started)
                if(error.code()==401 || error.code()==403)expired(started)
                throw error
            }
        }
    }
    private fun expired(started:Session):Nothing {
        ensureOwner(started);expire(started);throw com.mybetlive.application.SessionExpiredException()
    }
}

internal fun accessTokenNeedsRenewal(token:String,nowSeconds:Long=System.currentTimeMillis()/1000):Boolean=runCatching {
    val parts=token.split('.');if(parts.size!=3||parts[1].length>8192)return false
    val payload=String(java.util.Base64.getUrlDecoder().decode(parts[1]),Charsets.UTF_8)
    val expires=com.google.gson.JsonParser.parseString(payload).asJsonObject.get("exp")?.asLong ?: return false
    expires<=nowSeconds+30
}.getOrDefault(false)
