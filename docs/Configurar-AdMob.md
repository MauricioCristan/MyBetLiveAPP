# AdMob en MyBetLive

Implementado el 5 de octubre de 2026. El APK debug usa exclusivamente la app y el bloque nativo de prueba de Google. No genera ingresos. En release los anuncios permanecen apagados hasta configurar los IDs propios y desactivar el modo de prueba.

## Comportamiento

- Free: anuncios nativos identificados como publicidad en Partidos, Seguimientos (Activos y Anclados) e Historial. Por defecto uno cada 5 tarjetas, hasta 2 por lista; las listas cortas muestran uno al final. Activos y Anclados muestran un anuncio debajo del mensaje informativo incluso cuando están vacíos. Partidos e Historial vacíos no solicitan anuncios.
- Premium: sin anuncios, incluyendo accesos manuales y suscripciones canceladas que conservan acceso vigente. Cualquier suscripción vigente distinta de FREE elimina los anuncios.
- Sin sesión, sin respuesta de suscripción o sin conexión: no se solicitan anuncios. Si un anuncio falla, desaparece su espacio y la pantalla sigue funcionando.
- Detalles y notificaciones permanecen sin publicidad automática. No hay intersticiales automáticos. Al llegar al límite Free se ofrece un anuncio recompensado voluntario para obtener un seguimiento extra, con máximo uno extra activo. Consulta [recompensas](Plan-anuncios-recompensados.md).
- Los anuncios se destruyen al abandonar su tarjeta. No se refrescan por un temporizador.

## Administrador

En **Settings → MyBetLive · Publicidad** se guardan los siguientes controles en `setting`. Solo ADMIN puede modificarlos; se conserva auditoría y guardado atómico.

| Clave bajo `mybet.ads.` | Valor inicial | Rango / efecto |
|---|---|---|
| enabled | true | Interruptor para clientes Free |
| testMode | true | Debug usa pruebas; release no muestra anuncios en este modo |
| eventsEnabled | true | Partidos |
| historyEnabled | true | Historial |
| trackersEnabled | true | Seguimientos: Activos y Anclados |
| everyItems | 5 | Entero entre 3 y 50 tarjetas |
| maxAdsPerScreen | 2 | Entero entre 1 y 3 anuncios por lista |
| nativeUnitId | vacío | ID del bloque nativo de producción con `/` |
| rewardedEnabled | true | Ofrecer un seguimiento extra mediante anuncio voluntario |
| rewardedUnitId | vacío | ID independiente del bloque Rewarded; requiere SSV |

Se inicializan únicamente claves inexistentes. El backend recarga la configuración cada 5 segundos y después de guardar. La app recibe los ajustes en `/api/v1/subscriptions/me` durante su actualización habitual en primer plano. Guardar no obliga a solicitar anuncios. La frecuencia de tarjetas no representa un intervalo de tiempo ni altera consultas deportivas.

## Configuración de producción

Identificadores de MyBetLive proporcionados el 6 de octubre de 2026:

| Identificador | Valor |
|---|---|
| App ID | `ca-app-pub-7769623175751624~9357829812` |
| Bloque nativo · Listas Free | `ca-app-pub-7769623175751624/4045793388` |
| Bloque recompensado · Seguimiento extra | `ca-app-pub-7769623175751624/1529195275` |

El App ID está guardado como `MYBET_ADMOB_APP_ID` en `gradle.properties` para las compilaciones release. Los bloques quedan documentados para guardar en `mybet.ads.nativeUnitId` y `mybet.ads.rewardedUnitId` desde Settings; no se han actualizado esas filas ni desactivado `mybet.ads.testMode`. Las compilaciones debug conservan los anuncios de prueba. Falta configurar SSV y completar los requisitos de AdMob antes de activar producción.

El usuario eligió conservar **Grupos de anuncios** en el bloque recompensado. AdMob puede mostrar dos videos consecutivos en una impresión; se mantiene una sola recompensa `tracking_slot` de cantidad `1` por impresión completada. La duración y el formato efectivos los decide AdMob; la app no garantiza que cada impresión contenga dos videos.

1. Crear la cuenta AdMob y registrar la app Android `com.mybetlive`. Crear un bloque de **anuncio nativo**.
2. Obtener el **App ID** (`ca-app-pub-…~…`) y el **ID del bloque nativo** (`ca-app-pub-…/…`). Son identificadores públicos: no se necesita contraseña ni clave privada.
3. Compilar release pasando el App ID mediante la propiedad Gradle `MYBET_ADMOB_APP_ID`, por ejemplo `gradlew.bat assembleRelease -PMYBET_ADMOB_APP_ID=TU_APP_ID`. Debug siempre conserva los IDs de prueba. Una release sin App ID válido funciona sin anuncios.
4. Configurar en AdMob los mensajes aplicables de Privacidad y mensajes. La release utiliza UMP: actualiza el consentimiento, muestra el formulario cuando corresponde y solo inicializa publicidad si `canRequestAds()` lo permite. Perfil muestra **Privacidad de anuncios** cuando Google exige el acceso a esas opciones.
5. Completar revisión de app, vinculación con Google Play, política de privacidad, declaración de datos y verificación `app-ads.txt` según lo requiera AdMob. Revisar la elegibilidad de la app de seguimiento de apuestas; evitar anuncios de casinos/apuestas y categorías sensibles incompatibles con las políticas de la tienda.
6. Con la release adecuada instalada, guardar el bloque nativo propio y `testMode=false` en el administrador. Los IDs de app y bloque deben pertenecer al mismo editor. El backend rechaza activar producción sin un bloque propio.

El formulario UMP real y la entrega de anuncios de producción requieren tu cuenta y mensajes publicados; no se validaron con los IDs de demostración. Las pruebas de desarrollo usan exclusivamente anuncios de prueba. No pulsar anuncios reales durante QA.

Referencias: [integración Android](https://developers.google.com/admob/android/quick-start), [anuncios nativos](https://developers.google.com/admob/android/native/advanced), [anuncios de prueba](https://developers.google.com/admob/android/test-ads), [consentimiento UMP](https://developers.google.com/admob/android/privacy).
