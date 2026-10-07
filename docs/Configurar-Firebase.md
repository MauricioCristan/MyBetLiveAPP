# Actualizaciones con la app cerrada

La notificación local muestra el último dato recibido. Para recibir cambios con Android en segundo plano se necesita Firebase Cloud Messaging (FCM). Las credenciales y los informes del entorno de pruebas se conservan localmente y no forman parte del repositorio.

1. En [Firebase Console](https://console.firebase.google.com/), añade Firebase al proyecto Google Cloud de MyBetLive existente.
2. Registra una app Android con el paquete `com.mybetlive`. Descarga `google-services.json` y guárdalo en `app/google-services.json`. Gradle activa automáticamente el plugin si encuentra ese archivo. Recompila e instala la APK.
3. En Configuración del proyecto → Cuentas de servicio, prepara la credencial de Firebase Admin. Para desarrollo local, guarda el JSON fuera de Git, por ejemplo `<backend>/.local/firebase-admin.json`. La credencial administrativa pertenece exclusivamente al servidor.
4. Copia `scripts/firebase.env.example` del backend a `.local/firebase.env`. Escribe el ID real del proyecto y la ruta del JSON; cambia `MYBET_FIREBASE_ENABLED` a `true` cuando estén listos.
5. Reinicia mediante `scripts/Iniciar-MyBetLivePostgres.ps1`. El lanzador carga la configuración opcional y comprueba las rutas sin imprimir secretos. Los cobros permanecen deshabilitados.
6. Abre la app, inicia sesión y permite las notificaciones. La app registra el token de esta instalación con su propietario.

Fuentes de configuración: [Android](https://firebase.google.com/docs/android/setup?hl=es) y [Firebase Admin / Application Default Credentials](https://firebase.google.com/docs/admin/setup).

## Prueba de aceptación

- Crear un seguimiento propio y enviar una actualización desde el backend con la app en segundo plano.
- Comprobar que reemplaza la misma notificación, conserva equipos/foto, muestra la hora y abre su detalle.
- Desanclar: no reaparece durante el juego y el seguimiento sigue guardado. El resultado final puede avisarse.
- Verificar dato atrasado, marcador ausente, final y permisos del plan.
- Cerrar sesión y comprobar que no se muestran seguimientos de la cuenta anterior.
- Repetir con ahorro de batería/red intermitente. Un fabricante puede retrasar la entrega; la notificación siempre debe identificar el último dato recibido.

El recorrido de transporte real ya se completó con estadísticas simuladas, sin modificar apuestas. Quedan pendientes un partido real en curso y la promoción nativa en Android 16. No se necesita Firebase Authentication para el acceso con Google existente.


