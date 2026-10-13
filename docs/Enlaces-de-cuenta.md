# Enlaces de confirmación y recuperación

Los correos mantienen las URLs HTTPS de producción:

- `https://mybetliveadmin.pages.dev/reset-password?token=...`
- `https://mybetliveadmin.pages.dev/verify-email?token=...`

Android registra únicamente esas dos rutas mediante App Links. La app valida el origen exacto, la ruta y un único token hexadecimal de 64 caracteres; no admite otros hosts, puertos, credenciales, fragmentos o parámetros adicionales. Conserva los enlaces `mybetlive://auth/reset` y `mybetlive://auth/verify` del botón web existente.

El enlace abre la pantalla de contraseña o confirmación sin repetir la animación inicial. Abrirlo no consume el token ni confirma una cuenta: se requiere una acción del usuario. El backend sigue validando expiración, destinatario y uso único.

## Activar en producción

1. Subir los cambios de MyBetLiveAdmin a Cloudflare Pages. Confirmar que `https://mybetliveadmin.pages.dev/.well-known/assetlinks.json` devuelve JSON, HTTP 200 y ninguna redirección.
2. Instalar la nueva APK release con la misma firma, conservando los datos. La firma actual está publicada en el JSON del admin; no se publica la firma debug.
3. Con conexión a internet, esperar a que Android verifique el dominio. En Android 12 o superior puede solicitarse con `adb shell pm verify-app-links --re-verify com.mybetlive`; revisar `adb shell pm get-app-links com.mybetlive`.
4. Probar desde un correo en el celular. Sin la app instalada o desde computadora, permanece disponible el formulario web. Algunos lectores de correo usan navegadores internos; la página conserva el botón para abrir la app.

Antes de distribuir mediante Google Play App Signing, agregar también a `assetlinks.json` la huella SHA-256 del **certificado de firma de la aplicación de Google Play**. Puede diferir del certificado de subida usado para la APK local. No sustituir la huella local si todavía se distribuyen APK con ella.

No se necesita modificar el backend ni solicitar nuevos correos: los enlaces existentes válidos tienen el mismo formato.

## Validación local

- 139 pruebas Android aprobadas, incluidos los enlaces HTTPS válidos y el rechazo de orígenes, rutas y tokens no admitidos.
- APK debug y release compiladas; firma release verificada y coincidente con `assetlinks.json`.
- Build de Cloudflare aprobado y comprobado que incluye el JSON en `.well-known` (conserva las advertencias de presupuesto de tamaño ya existentes).
- En producción todavía se obtiene HTML en la URL de `assetlinks.json`; falta publicar los cambios del admin.
- La revisión automática de permisos bloqueó el comando de instalación y apertura del enlace en el HONOR conectado, sin motivo específico. No se instaló la actualización ni se verificó el flujo físico en esta sesión.
