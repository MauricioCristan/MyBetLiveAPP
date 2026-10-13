package com.mybetlive.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import com.mybetlive.application.*
import androidx.activity.compose.BackHandler

internal enum class EmailFormMode { LOGIN, REGISTER, FORGOT }
@Composable internal fun EmailAccountForm(state:AppState,model:TrackingViewModel,url:String,mode:EmailFormMode,onModeChange:(EmailFormMode)->Unit) {
    var username by rememberSaveable{mutableStateOf("")};var email by rememberSaveable{mutableStateOf("")}
    var password by remember{mutableStateOf("")};var confirmation by remember{mutableStateOf("")}
    var validation by remember{mutableStateOf<String?>(null)}
    val focus=LocalFocusManager.current
    fun change(next:EmailFormMode){focus.clearFocus();password="";confirmation="";validation=null;onModeChange(next)}
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
    if(mode==EmailFormMode.REGISTER)LocalizedText("Confirma tu correo antes de comenzar. Tu cuenta inicia con el plan Free.",color=Muted,fontSize=13.sp)
    if(mode==EmailFormMode.FORGOT)LocalizedText("Te enviaremos un enlace para crear una nueva contraseña.",color=Muted,fontSize=14.sp,lineHeight=20.sp)
    state.error?.let{LocalizedText(it,color=Red)};validation?.let{LocalizedText(it,color=Red)}
    state.emailMessage?.let{LocalizedText(it,color=Green)}
    if(state.emailMessage==null||mode==EmailFormMode.LOGIN) {
        if(mode!=EmailFormMode.FORGOT)OutlinedTextField(username,{username=it},label={LocalizedText(if(mode==EmailFormMode.LOGIN)"Correo o usuario" else "Usuario")},singleLine=true,enabled=!state.busy,modifier=Modifier.fillMaxWidth(),shape=androidx.compose.foundation.shape.RoundedCornerShape(12.dp),keyboardOptions=KeyboardOptions(imeAction=ImeAction.Next),keyboardActions=KeyboardActions(onNext={focus.moveFocus(androidx.compose.ui.focus.FocusDirection.Down)}))
        if(mode!=EmailFormMode.LOGIN)OutlinedTextField(email,{email=it},label={LocalizedText("Correo electrónico")},singleLine=true,enabled=!state.busy,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email,imeAction=if(mode==EmailFormMode.FORGOT)ImeAction.Done else ImeAction.Next),keyboardActions=KeyboardActions(onNext={focus.moveFocus(androidx.compose.ui.focus.FocusDirection.Down)},onDone={focus.clearFocus()}),modifier=Modifier.fillMaxWidth(),shape=androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
        if(mode!=EmailFormMode.FORGOT) {
            PasswordField(password,{password=it},"Contraseña",!state.busy,next=mode==EmailFormMode.REGISTER)
            if(mode==EmailFormMode.REGISTER)PasswordField(confirmation,{confirmation=it},"Confirmar contraseña",!state.busy)
        }
        if(mode==EmailFormMode.LOGIN)TextButton(onClick={change(EmailFormMode.FORGOT)},enabled=!state.busy,modifier=Modifier.align(Alignment.End)) {
            LocalizedText("Olvidé mi contraseña",fontSize=13.sp)
        }
        Button(onClick={
            focus.clearFocus()
            validation=null
            when(mode) {
                EmailFormMode.LOGIN->model.login(url,username,password)
                EmailFormMode.REGISTER->{validation=emailRegistrationError(username,email,password,confirmation);if(validation==null)model.registerEmail(url,username,email,password)}
                EmailFormMode.FORGOT->{if(!email.trim().matches(Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")))validation="Ingresa un correo válido." else model.forgotPassword(url,email)}
            }
        },enabled=!state.busy&&url.isNotBlank()&&when(mode){EmailFormMode.LOGIN->username.isNotBlank()&&password.isNotBlank();EmailFormMode.REGISTER->username.isNotBlank()&&email.isNotBlank()&&password.isNotBlank()&&confirmation.isNotBlank();else->email.isNotBlank()},modifier=Modifier.fillMaxWidth().heightIn(min=54.dp),shape=androidx.compose.foundation.shape.RoundedCornerShape(12.dp)) {
            if(state.busy){CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp,color=Ink);Spacer(Modifier.width(10.dp))}
            LocalizedText(if(state.busy)"Procesando…" else when(mode){EmailFormMode.LOGIN->"Entrar";EmailFormMode.REGISTER->"Crear cuenta";else->"Enviar enlace de recuperación"})
        }
    }
    if(mode==EmailFormMode.REGISTER&&state.emailMessage!=null&&email.isNotBlank())TextButton(onClick={model.resendVerification(url,email)},enabled=!state.busy){LocalizedText("Reenviar confirmación de correo")}
    if(mode==EmailFormMode.LOGIN) {
        TextButton(onClick={change(EmailFormMode.REGISTER)},enabled=!state.busy,modifier=Modifier.align(Alignment.CenterHorizontally)){LocalizedText("Crear una cuenta con correo",fontSize=13.sp)}
    } else TextButton(onClick={change(EmailFormMode.LOGIN)},enabled=!state.busy,modifier=Modifier.align(Alignment.CenterHorizontally)){LocalizedText("Volver a iniciar sesión")}
    }
}
@Composable private fun PasswordField(value:String,change:(String)->Unit,label:String,enabled:Boolean,next:Boolean=false) {
    var visible by remember{mutableStateOf(false)}
    val focus=LocalFocusManager.current
    OutlinedTextField(value,change,label={LocalizedText(label)},singleLine=true,enabled=enabled,visualTransformation=if(visible)VisualTransformation.None else PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password,imeAction=if(next)ImeAction.Next else ImeAction.Done),keyboardActions=KeyboardActions(onNext={focus.moveFocus(androidx.compose.ui.focus.FocusDirection.Down)},onDone={focus.clearFocus()}),modifier=Modifier.fillMaxWidth(),shape=androidx.compose.foundation.shape.RoundedCornerShape(12.dp),trailingIcon={
        TextButton(onClick={visible=!visible},enabled=enabled){LocalizedText(if(visible)"Ocultar" else "Mostrar",fontSize=12.sp)}
    })
}
@Composable internal fun EmailLinkScreen(state:AppState,model:TrackingViewModel) {
    val link=state.emailLink?:return
    BackHandler(enabled=!state.busy){model.closeEmailLink()}
    var password by remember{mutableStateOf("")};var confirmation by remember{mutableStateOf("")};var error by remember{mutableStateOf<String?>(null)}
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        BrandLogo(height=34.dp)
        Spacer(Modifier.height(6.dp))
        LocalizedText(if(link.kind==EmailLinkKind.VERIFY)"Confirmar mi correo" else "Crear nueva contraseña",fontSize=28.sp,lineHeight=34.sp,fontWeight=FontWeight.Bold)
        state.emailMessage?.let{LocalizedText(it,color=Green)};state.error?.let{LocalizedText(it,color=Red)};error?.let{LocalizedText(it,color=Red)}
        if(state.emailMessage==null) {
            if(link.kind==EmailLinkKind.RESET){PasswordField(password,{password=it},"Nueva contraseña",!state.busy,next=true);PasswordField(confirmation,{confirmation=it},"Confirmar contraseña",!state.busy)}
            Button(onClick={if(link.kind==EmailLinkKind.VERIFY)model.verifyEmail() else {
                error=passwordChangeError(password,confirmation);if(error==null)model.resetPassword(password)
            }},enabled=!state.busy&&(link.kind==EmailLinkKind.VERIFY||password.isNotBlank()&&confirmation.isNotBlank()),modifier=Modifier.fillMaxWidth().heightIn(min=54.dp),shape=androidx.compose.foundation.shape.RoundedCornerShape(12.dp)) {
                LocalizedText(if(state.busy)"Procesando…" else if(link.kind==EmailLinkKind.VERIFY)"Confirmar correo" else "Guardar contraseña")
            }
        }
        TextButton(onClick=model::closeEmailLink,enabled=!state.busy){LocalizedText("Volver a MyBetLive")}
    }
}
