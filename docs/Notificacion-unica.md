# Notificación única de seguimientos

Android publica una sola tarjeta de seguimiento, con tag `tracking-summary` e id 1. Al expandirla, Anterior y Siguiente recorren circularmente los últimos snapshots; el contador indica la posición. Un seguimiento conserva solo Ver seguimiento/Ver resultado. El contenido abre el seguimiento seleccionado.

Las actualizaciones conservan la selección. Los avisos importantes de otros seguimientos incrementan Novedades sin cambiar el contenido elegido; al visitarlos se limpia su indicador. Los resultados se conservan en la misma tarjeta, que deja de ser persistente cuando no quedan seguimientos activos. Cambiar de seguimiento no repite sonidos ni consulta backend o APIs deportivas.

Los snapshots y la selección sobreviven a la recreación del proceso. Se rechazan actualizaciones más antiguas; imágenes tardías y acciones sobre tarjetas retiradas no pueden recrearlas. Un receiver no exportado recibe PendingIntents explícitos e inmutables. La generación de cuenta invalida acciones al cerrar sesión. El estado se limpia al salir o suspender el acceso a notificaciones. La pantalla bloqueada conserva una versión sin nombres, marcadores ni acciones.

El borrado de un seguimiento elimina su snapshot y selecciona el siguiente disponible. Los avisos individuales antiguos se retiran cuando se inicializa el gestor nuevo. activeIds sigue devolviendo todos los seguimientos activos representados, no solo el seleccionado, para mantener los indicadores de la app.

Validación: pruebas de una sola tarjeta con varios seguimientos, navegación circular, actualización sin saltos, enlace al seguimiento, persistencia, resultados y novedades, borrado, cierre/cambio de cuenta, permisos y canales, datos antiguos, imágenes tardías y plantillas de Android 16. Las pruebas de receiver ejecutan el Intent real del PendingIntent contra el componente registrado; las pruebas locales no simulan el panel SystemUI completo de cada fabricante.
