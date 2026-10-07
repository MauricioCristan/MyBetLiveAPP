package com.mybetlive.auth

import android.content.Context
import android.content.MutableContextWrapper
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.mybetlive.data.GoogleChallenge
import kotlinx.coroutines.CancellationException

suspend fun googleIdToken(context: Context, challenge: GoogleChallenge): String {
    val option = GetSignInWithGoogleOption.Builder(challenge.serverClientId).setNonce(challenge.nonce).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
    try {
        val credential = CredentialManager.create(context).getCredential(MutableContextWrapper(context), request).credential
        require(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Google no devolvió una credencial válida. Intenta nuevamente"
        }
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    } catch (error: GetCredentialCancellationException) {
        throw CancellationException("Acceso con Google cancelado", error)
    } catch (error: NoCredentialException) {
        throw IllegalStateException("No hay una cuenta de Google disponible. Revisa las cuentas del teléfono e intenta nuevamente")
    } catch (error: GetCredentialException) {
        throw IllegalStateException("No se pudo abrir el acceso con Google. Revisa tu conexión y los servicios de Google Play")
    }
}
