# Registro e inicio de sesión con Google

La app usa Credential Manager. El primer acceso con una identidad de Google verificada crea una cuenta de cliente con rol USER y acceso FREE; los siguientes accesos usan la misma cuenta mediante el identificador estable `sub`. El usuario no necesita definir una contraseña para este flujo.

La implementación está compilada y probada. El 3 de octubre de 2026 el usuario confirmó los clientes Android y Web del proyecto mybetlive; el Client ID Web quedó en la configuración local ignorada y activo en el backend 8081. Después se comprobó acceso real, cierre y segundo acceso con la misma cuenta en HONOR YAL-L21, conservando su historial. La cuenta ya existía: un primer registro con una identidad nueva todavía requiere prueba física. No se utiliza ni se copió el Client Secret descargado.

## Configuración en Google

1. Crea o selecciona el proyecto de MyBetLive en [Google Auth Platform](https://console.cloud.google.com/auth/overview).
2. Configura Branding y Audience. Para una app externa en pruebas, añade las cuentas que probarán el acceso según la configuración de audiencia del proyecto.
3. Crea un cliente OAuth de tipo **Android** con el paquete `com.mybetlive` y el SHA-1 de la firma que use la APK.
4. Crea un cliente OAuth de tipo **Web application** en el mismo proyecto. Su Client ID será la audiencia del backend y el `serverClientId` del selector Android. Este flujo de ID tokens no usa Client Secret ni requiere guardar un secreto en la app.

Firma debug verificada nuevamente en esta laptop el 3 de octubre de 2026 mediante signingReport y keytool:

```text
Paquete: com.mybetlive
SHA-1: 38:21:0A:3F:A6:F1:2A:FC:30:AB:64:27:B3:98:5E:95:DC:49:E4:06
SHA-256: A4:6C:C7:5C:A6:DA:47:7E:45:7C:EB:2F:9D:84:99:91:D9:37:88:3B:95:79:EA:16:67:9A:2B:25:E2:4B:1D:C1
```

Puedes recalcular las firmas con `gradlew.bat signingReport`. Para Google Play se debe configurar también la firma de Play App Signing; no reutilices la firma debug para la distribución final.

## Activación local

Añade a **<backend>/.local/api-sports.env**:

```dotenv
MYBET_GOOGLE_WEB_CLIENT_ID=CLIENT_ID_WEB.apps.googleusercontent.com
```

Reinicia el backend con **<backend>/scripts/Iniciar-MyBetLivePostgres.ps1** o Iniciar-DeportesReal.ps1. La ejecución actual usa PostgreSQL my_bet_live y llaves JWT persistentes: los datos sobreviven al reinicio. Iniciar-NflReal.ps1 conserva la alternativa H2 temporal, que requiere respaldo antes de detenerse. En otros despliegues, define la misma variable de entorno o `mybet.auth.google.web-client-id`. En producción, el backend debe estar disponible mediante HTTPS.

El lanzador Iniciar-DeportesReal.ps1 también carga esa configuración. La activación local del 3 de octubre comprobó que /auth/google/challenge devuelve el Client ID Web esperado y nonces distintos; /auth/google rechaza un token artificial inválido con 401 / GOOGLE_INVALID_TOKEN. El proceso estaba apagado y se restauraron 15 seguimientos (NFL 6, fútbol 4, MLB 5), cinco ligas de fútbol y FREE desde el respaldo anterior. La sesión demo anterior debe renovarse porque las llaves JWT son temporales.

Después se trasladó a PostgreSQL conservando las 19 tablas y sus IDs. Google sigue activo y el reinicio mantiene las nuevas sesiones; ya no se generan llaves temporales en la ejecución normal de 8081. Ver **docs/Postgres-local.md del backend**.

Para la prueba física: conectar el celular por USB con depuración autorizada, configurar el túnel adb reverse tcp:8081 tcp:8081 y usar http://127.0.0.1:8081 en Android. Pulsar Continuar con Google, elegir una cuenta de prueba y verificar el primer registro USER/FREE; salir y volver a entrar con la misma cuenta para comprobar reutilización. Los seguimientos demo pertenecen a su propietario original: una nueva cuenta Google comienza sin ellos. Evidencia de activación: **docs/verification/google-oauth-2026-10-03/resultado.md**.

No necesitas recompilar Android cuando cambie ese Client ID: el backend lo entrega al iniciar el acceso. Tampoco se necesita `google-services.json` para esta integración directa con Google. FCM y Google Play Billing conservan su configuración independiente.

## Flujo y comportamiento

1. «Continuar con Google» solicita `POST /api/v1/auth/google/challenge`, que devuelve el Client ID Web y un nonce aleatorio de un solo uso, válido cinco minutos.
2. Credential Manager abre el selector y solicita un ID token con ese nonce.
3. La app envía `idToken` y `nonce` a `POST /api/v1/auth/google`.
4. El backend verifica firma RS256 con claves de Google, emisor, audiencia, expiración, correo verificado y nonce. Devuelve `tokens`, `displayName` y `created`; emite las mismas sesiones JWT/refresh usadas por el acceso actual.
5. Al salir, Android limpia su sesión y el estado de Credential Manager para permitir elegir otra cuenta.

Un correo ya registrado mediante otra cuenta no se vincula automáticamente: devuelve `GOOGLE_EMAIL_IN_USE`. Vincular cuentas con autenticación de ambas partes queda pendiente. Las cuentas deshabilitadas no pueden acceder con Google ni renovar su sesión.

V5 agrega `google_identity`, sin modificar las contraseñas o roles de las cuentas anteriores. Los ID tokens, nonces y refresh tokens no se guardan en ejemplos ni logs. Los nonces pendientes viven en la instancia de backend; para varias instancias se necesita un almacén compartido o afinidad de sesión antes de habilitar ese despliegue.

## Validación disponible

La entrega original aprobó 52 pruebas de backend, incluidas firma falsificada, emisor/audiencia incorrectos, expiración, nonce, repetición, registro, reutilización de cuenta, permisos mínimos, correo existente, cuentas deshabilitadas y refresh. La entrega Android actual aprobó 40 pruebas y compilación/lint. La selección y autorización real, cierre y segundo acceso se comprobaron físicamente; evidencia en `docs/verification/implementation-2026-10-03/resultado.md`. La cancelación y los errores del proveedor Google aún requieren revisión física específica.

Referencias: [Credential Manager con Google](https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation), [verificación de ID tokens en el backend](https://developers.google.com/identity/sign-in/android/backend-auth), [marca de Google](https://developers.google.com/identity/branding-guidelines). El icono se obtuvo del recurso oficial de esa guía.

