# MyBetLive · Android

App nativa en Kotlin y Jetpack Compose para consultar partidos y seguir selecciones deportivas. El backend Spring Boot está en `seed-backend` y el portal administrativo en `seed-frontend`.

## Funciones

- Catálogo de NFL, fútbol, MLB y NBA, con equipos y logos.
- Moneyline/1X2, totales, hándicap y props según capacidades del deporte y permisos del plan.
- Marcador, progreso, estado provisional, resultado e historial; los datos ausentes no se representan como cero.
- Acceso con Google o usuario/contraseña, sesiones cifradas y renovación.
- Notificaciones de seguimiento con apertura del detalle y desanclado.
- Planes, suscripciones Google Play y restauración; requieren configuración del servidor y Play Console.
- Anuncios nativos para Free y recompensa voluntaria de un seguimiento extra, con máximo uno extra activo. Debug utiliza anuncios de prueba.

Free permite un seguimiento base; los beneficios y límites efectivos se obtienen del backend. Premium puede habilitar props, más seguimientos y una experiencia sin publicidad. No se incluyen precios inventados en la APK.

MLB ofrece mercados de equipos y carreras, sin props individuales con API-Baseball. Fútbol utiliza regulación más compensación, sin prórroga ni penales. Los resultados son informativos y no representan liquidaciones oficiales de un casino.

## Compilación

Abrir el proyecto en Android Studio. Usar JDK 21 y Android SDK 36; declarar la instalación del SDK en `local.properties`, que permanece fuera de Git.

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest lintDebug
```

## Backend local

Iniciar el perfil demo del backend con JDK 25:

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=demo'
```

La cuenta `demo / demo-change-me` existe únicamente en ese entorno temporal. En el emulador, usar `http://10.0.2.2:8080`. En un teléfono autorizado por USB:

```powershell
adb reverse tcp:8080 tcp:8080
```

Usar `http://127.0.0.1:8080` en la app. Para el perfil PostgreSQL local del backend en 8081, configurar `adb reverse tcp:8081 tcp:8081` y `http://127.0.0.1:8081`. HTTP se permite únicamente en debug; release requiere HTTPS.

El lanzador `scripts/Iniciar-MyBetLive.ps1` utiliza `ANDROID_SDK_ROOT` o `ANDROID_HOME`, y permite indicar `-SdkRoot`, `-BackendRoot` y `-BackendJava`. Requiere el AVD `MyBetLive_API_36` y una APK debug compilada. Los scripts de prueba de planes operan sobre un backend demo aislado, con cobros deshabilitados.

## Google, Firebase y anuncios

El cliente Web OAuth se configura en el backend. Android recibe el challenge y usa Credential Manager; no necesita un client secret ni Firebase Authentication. Véase [configuración Google](docs/Configurar-Google.md).

Para FCM, proporcionar el `app/google-services.json` del entorno. Gradle activa el plugin cuando ese archivo existe. La credencial Firebase Admin queda exclusivamente en el servidor. Véase [configuración Firebase](docs/Configurar-Firebase.md).

El App ID de AdMob en `gradle.properties` es un identificador público, no una credencial. Debug utiliza la app y los bloques de prueba de Google. La publicidad de producción y las recompensas SSV requieren completar la configuración del backend/AdMob. Véanse [AdMob](docs/Configurar-AdMob.md) y [recompensas](docs/Plan-anuncios-recompensados.md).

## Archivos del repositorio

Conservar fuentes, recursos de marca, pruebas, scripts reproducibles y Gradle Wrapper, incluido `gradle-wrapper.jar`. No incluir credenciales, `google-services.json`, llaves de firma, `.env`, `local.properties`, `.local`, cachés, compilaciones ni APK/AAB generados.

La bóveda Obsidian, respuestas crudas de APIs, informes personalizados y capturas de verificaciones se conservan localmente y están excluidos del repositorio. Los ejemplos HTTP/Postman utilizan variables que cada desarrollador completa en su entorno.

El paquete de imágenes promocionales se documenta en [Google Play](docs/diseno/google-play/LEEME.md); sus vistas son ilustrativas y deben contrastarse con la versión distribuida antes de publicar.

## Jornadas y zona horaria

Android envía el ID IANA del dispositivo, por ejemplo `America/Monterrey`, en `timezone`. `GET /api/v1/tracking/calendar` devuelve el día actual de esa zona calculado por el servidor. “Hoy” sigue ese calendario; una fecha histórica elegida explícitamente se conserva al cambiar de día o zona.

`GET /api/v1/tracking/events?sport=MLB&date=2026-10-07&timezone=America/Monterrey` devuelve los encuentros cuyo inicio cae entre las dos medianoches locales. El backend consulta y comparte las particiones UTC necesarias, con límites exclusivos y días de 23/25 horas cuando corresponda. El intervalo de seguimiento automático sigue agrupado por deporte/fecha UTC.

Los timestamps originales siguen siendo instantes UTC. Los campos opcionales `localStartsAt` y `localUpdatedAt` se convierten al responder, sin cambiar la base ni asociar un evento a la zona de un cliente. Android usa esos valores para mostrar horarios correctos incluso con reglas horarias antiguas del sistema. Sin `timezone`, el API conserva la jornada UTC para clientes anteriores.

Props, totales y hándicaps usan una rueda vertical con opciones `lineRules` del backend por deporte y mercado. Se puede elegir entre enteras y medios, deslizar con ajuste al centro o tocar una opción. Cambiar el formato requiere elegir de nuevo; mientras se desplaza o faltan metadatos, guardar permanece bloqueado. Los hándicaps admiten valores negativos. El servidor valida los mismos límites antes de consultar jugadores o consumir un espacio de seguimiento.

Los seguimientos liquidados pasan a Historial, ordenados por su última actualización. Activos y Anclados muestran el último resultado del deporte seleccionado con acceso al detalle. Un resultado final guardado conserva Ganada/Perdida/Push/Cancelada aunque el dispositivo esté sin conexión; los estados en vivo siguen marcándose como provisionales o desactualizados.
