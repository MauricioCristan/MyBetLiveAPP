package com.mybetlive.data

import com.mybetlive.application.FailureMessages
import retrofit2.HttpException
import androidx.credentials.exceptions.*
import com.google.gson.JsonParser

class ClientErrorMessages : FailureMessages {
    override fun serverMessage(error: Throwable): String? = if(error is HttpException) runCatching {
        JsonParser.parseString(error.response()?.errorBody()?.string()).asJsonObject.get("message")?.asString
    }.getOrNull() else null
    override fun message(error: Throwable): String {
        serverMessage(error)?.let { return it }
        return when(error) {
            is HttpException -> when(error.code()) {
                401 -> "La sesión no es válida. Revisa tus credenciales."
                400 -> "Revisa la selección y la línea del seguimiento."
                403 -> "Tu plan no permite esta operación. Consulta Mi plan."
                503 -> "Proveedor sin configurar o sin consultas disponibles. Revisa el backend."
                else -> "El servidor no pudo completar la operación (${error.code()})."
            }
            is java.io.IOException -> "No se pudo conectar al servidor. Revisa tu conexión y vuelve a intentar; tus seguimientos guardados se conservan."
            is GetCredentialCancellationException -> "Se canceló el inicio con Google. Puedes volver a intentarlo."
            is NoCredentialException -> "Google no encontró una cuenta disponible. Agrega una cuenta de Google en este dispositivo."
            is GetCredentialException -> "Google no pudo iniciar sesión. Revisa tu cuenta y vuelve a intentar."
            else -> error.message ?: "No se pudo completar la operación. Vuelve a intentar."
        }
    }
}
