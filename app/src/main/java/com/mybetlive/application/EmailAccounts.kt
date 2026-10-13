package com.mybetlive.application

import java.net.URI
import java.net.URLDecoder

interface EmailAccountOperations {
    suspend fun registerEmail(url:String,username:String,email:String,password:String)
    suspend fun forgotPassword(url:String,email:String)
    suspend fun resendVerification(url:String,email:String)
    suspend fun verifyEmail(url:String,token:String)
    suspend fun resetPassword(url:String,token:String,password:String)
}
enum class EmailLinkKind { VERIFY, RESET }
data class EmailAccountLink(val kind:EmailLinkKind,val token:String) {
    companion object {
        fun parse(value:String?):EmailAccountLink?=runCatching {
            val uri=URI(value?:return null)
            if(uri.userInfo!=null||uri.port!=-1||uri.fragment!=null)return null
            val kind=when {
                uri.scheme=="mybetlive" && uri.host=="auth" -> when(uri.path) {
                    "/verify"->EmailLinkKind.VERIFY;"/reset"->EmailLinkKind.RESET;else->return null
                }
                uri.scheme=="https" && uri.host=="mybetliveadmin.pages.dev" -> when(uri.path) {
                    "/verify-email"->EmailLinkKind.VERIFY;"/reset-password"->EmailLinkKind.RESET;else->return null
                }
                else->return null
            }
            val params=uri.rawQuery?.split('&')?:return null
            if(params.size!=1||!params[0].startsWith("token="))return null
            val token=URLDecoder.decode(params[0].substringAfter('='),"UTF-8")
            if(!token.matches(Regex("[a-f0-9]{64}")))return null
            EmailAccountLink(kind,token)
        }.getOrNull()
    }
}
fun emailRegistrationError(username:String,email:String,password:String,confirmation:String):String?=when {
    !username.matches(Regex("[A-Za-z0-9_.-]{3,60}"))->"Usa entre 3 y 60 letras, números, puntos, guiones o guiones bajos para el usuario."
    email.trim().length>120||!email.trim().matches(Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))->"Ingresa un correo válido."
    else->passwordChangeError(password,confirmation)
}
fun passwordChangeError(password:String,confirmation:String):String?=when {
    password.isBlank()||password.length<8->"Usa una contraseña de al menos 8 caracteres."
    password.toByteArray(Charsets.UTF_8).size>72->"La contraseña es demasiado larga. Usa menos caracteres."
    password!=confirmation->"Las contraseñas no coinciden."
    else->null
}
