# Inicio animado

El arranque desde el icono muestra las piezas blanca y verde del escudo uniéndose sobre el fondo #0C141B. Mantiene los paths originales del vector de marca. El escudo ocupa 112 dp y MyBetLive aparece debajo en Barlow Condensed Bold Italic a 28 sp, con MyBet blanco y Live verde. El nombre es una marca y no se traduce.

En Android 10/11, Compose une las piezas durante 500 ms, muestra el nombre durante 200 ms y desvanece al contenido durante 200 ms. La ventana inicial estática usa la misma posición separada de las piezas, evitando mostrar primero el escudo armado y luego desarmarlo. El tiempo previo de creación del proceso y carga de Compose depende de Android y no forma parte de esos 900 ms.

Android 12+ usa un AnimatedVectorDrawable de 500 ms en la pantalla nativa del sistema. Se conserva el tema Starting para que Android resuelva el vector animado. El listener de salida permite terminar el tiempo restante de la animación nativa, limitado a 500 ms. Compose continúa desde la fase del nombre, sin repetir la unión. El sistema controla cuándo presenta esa pantalla nativa; el estado final del escudo coincide con el de Compose.

MainActivity dibuja la introducción antes de crear el ViewModel, publicidad y facturación. Después de dos callbacks de cuadro, TrackingViewModelFactory prepara el repositorio y las notificaciones en Dispatchers.IO; al terminar se crea el contenido detrás de la animación. Si la preparación local aún no terminó, se conserva el escudo y el nombre antes de desvanecer, evitando una pantalla vacía. La publicidad y el permiso de notificaciones esperan al final. La animación no espera red. Solo se activa en una Activity nueva con MAIN/LAUNCHER, sin pantalla restaurada ni destino externo, y con ValueAnimator habilitado. Los nuevos intents cancelan la introducción inmediatamente; rotación, restauración y entradas por enlaces/notificaciones la omiten. No se cambian APIs, claves ni cuentas.

## Validación

- APK debug compilada e instalada conservando datos en HONOR YAL-L21, Android 10, y emulador Android 16/API 36.
- 138 pruebas unitarias Android sin fallos, incluyendo tres pruebas nuevas de condiciones de arranque. Las pruebas existentes de Dashboard y cuentas siguen pasando. La revisión visual no creó ni cambió cuentas reales.
- Grabación real del HONOR y del emulador: unión, nombre, transición al login y coincidencia de posición/tamaño del escudo nativo con Compose.
- En el emulador se verificaron regreso del segundo plano, rotación, enlace de correo con token ficticio sin confirmarlo, y arranque sin red con animaciones deshabilitadas. Se restauraron las opciones del dispositivo al terminar.
- La prueba real con el idioma inglés en API 36 detectó una expresión regular previa incompatible con ICU. Se escapó también la llave de cierre en los marcadores de AppStrings y se confirmó el arranque/traducciones después del arreglo.

La revisión posterior del arranque adelantado volvió a pasar las 138 pruebas y comprobó en API 36 que Android expone una duración nativa de 500 ms y que el enlace de confirmación abre su destino. La grabación actual del celular está en `.local/MyBetLive-inicio-adelantado.mp4` (archivo de revisión, ignorado por Git). El teléfono mantiene la URL de producción. No se hizo commit ni push.
