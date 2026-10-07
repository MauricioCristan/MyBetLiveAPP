package com.mybetlive.data
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

internal fun Session.sameOwner(current: Session?): Boolean = current != null && current.baseUrl == baseUrl &&
    current.username == username && current.generation == generation

/** Token refresh and response ownership are independent of Android storage and SDKs. */
internal class AuthorizedCalls(private val session: () -> Session?,private val save: (Session) -> Unit,
    private val api: (String) -> TrackingApi) {
    private val refreshLock=Mutex()
    private fun ensureOwner(started:Session){if(!started.sameOwner(session()))throw CancellationException("La cuenta cambió durante la consulta")}
    suspend fun <T> execute(action:suspend (TrackingApi)->T):T {
        val started=session() ?: error("Inicia sesión")
        try {val value=action(api(started.baseUrl));ensureOwner(started);return value;}
        catch(error:Exception) {
            ensureOwner(started)
            if(error !is HttpException||error.code()!=401)throw error
            refreshLock.withLock {
                ensureOwner(started);val current=session() ?: throw error
                if(current.tokens.accessToken==started.tokens.accessToken){
                    val tokens=api(current.baseUrl).refresh(Refresh(current.tokens.refreshToken))
                    ensureOwner(started);save(current.copy(tokens=tokens))
                }
            }
            val value=action(api(started.baseUrl));ensureOwner(started);return value
        }
    }
}
