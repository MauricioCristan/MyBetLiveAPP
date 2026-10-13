# Release de MyBetLive

La release usa el App ID propio de AdMob configurado en `MYBET_ADMOB_APP_ID` y el backend HTTPS de Railway. El APK debug mantiene los identificadores de demostración. Los bloques nativo y recompensado llegan desde la configuración de publicidad del backend; el modo de prueba debe desactivarse en el administrador para que la release los solicite. Premium continúa sin publicidad.

## Firma privada

La primera clave de subida se genera con `scripts/Prepare-ReleaseSigning.ps1`. Nunca reemplaza archivos existentes. Los archivos privados se guardan en `.local/signing/`, excluidos de Git:

- `mybetlive-upload.keystore`: clave privada RSA de 3072 bits, PKCS12.
- `release-signing.properties`: ruta, alias y contraseñas generadas, sin imprimirlas en la consola.
- `upload-certificate.der`: certificado público.
- `certificate-fingerprints.txt`: SHA-1 y SHA-256 públicos para registrar la firma.

Guardar una copia segura de la clave y las propiedades antes de publicar. En otra computadora se deben restaurar los mismos archivos. La compilación release falla si falta la firma o el App ID real; no cambia silenciosamente a IDs de prueba ni firma debug.

Compilar con Java 21:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot'
.\gradlew.bat assembleRelease bundleRelease
```

El APK firmado está en `app/build/outputs/apk/release/app-release.apk`. El AAB firmado para Google Play está en `app/build/outputs/bundle/release/app-release.aab`.

Se generó la versión `0.1.0`, código `1`, paquete `com.mybetlive`. Las copias para compartir están en `.local/releases/0.1.0/`, junto a las huellas del certificado público. Se verificó el APK con apksigner (firma v2 válida), el AAB con jarsigner y su estructura con bundletool. También pasó lintVitalRelease; la configuración debug sigue compilando con IDs de prueba. Se comprobó que los paquetes no contienen la clave privada, las propiedades de firma ni sus contraseñas. El generador rechazó una segunda ejecución y conservó intacta la clave existente; el validador de release rechazó el App ID de demostración de Google.

## Google y Firebase

Para instalar el APK directamente, registrar `com.mybetlive` y el SHA-1 del certificado de subida como cliente OAuth Android en el mismo proyecto que el cliente Web usado por el backend. Añadir SHA-1/SHA-256 en Firebase si corresponde y descargar la configuración actualizada. No cambiar el Client ID Web del backend ni copiar un Client Secret a Android.

Con Play App Signing, Google firma las APK distribuidas usando el certificado de firma de la aplicación, que puede ser distinto de esta clave de subida. Después de activar Play App Signing, obtener sus SHA-1/SHA-256 en Google Play Console → Integridad de la app y registrar también esa firma para Google/Firebase.

El APK release tiene una firma distinta de la APK debug instalada. No intentar reemplazarla mediante una desinstalación automática: eso elimina los datos locales. Para pruebas utilizar una instalación nueva, un dispositivo de pruebas o la distribución de prueba de Google Play.

## AdMob y activación

SSV debe quedar verificado y guardado en el bloque recompensado, con recompensa `1 tracking_slot`. La prueba de la URL comprueba conectividad/firma y no demuestra que una impresión real conceda crédito. Consultar `AdMob-SSV.md` del backend para el diagnóstico.

Configurar Privacidad y mensajes en AdMob; la release utiliza UMP y solicita anuncios únicamente cuando el consentimiento lo permite. Para revisar los bloques propios, registrar el celular como dispositivo de prueba desde AdMob. Evitar usar anuncios reales para QA. Los anuncios de prueba no son una validación de extremo a extremo del callback SSV real.

Después de vincular la app a la tienda y completar los requisitos de aprobación/verificación que solicite AdMob, activar publicidad y desactivar `mybet.ads.testMode` desde el administrador. Los fallos de carga o falta de inventario no deben bloquear el seguimiento de partidos.

Referencia: [Firma de aplicaciones y Play App Signing](https://developer.android.com/studio/publish/app-signing), [dispositivos de prueba de AdMob](https://developers.google.com/admob/android/test-ads).
