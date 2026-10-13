# Español e inglés

La app usa inglés cuando el idioma principal configurado del dispositivo es inglés (incluidos en-US y en-GB). Para español y los demás idiomas usa español. No cambia la zona horaria ni los datos de la cuenta.

Las pantallas, títulos, etiquetas de mercados, estados, líneas, mensajes dinámicos, accesibilidad y notificaciones se localizan al presentarse. Los identificadores técnicos, selecciones guardadas, nombres de equipos/jugadores y el contenido del proveedor se conservan. Los anuncios de terceros y Google Play gestionan su propio idioma.

Los recursos están en app/src/main/res/values/locale_catalog.xml y values-en/locale_catalog.xml. La capa AppStrings adapta los textos de presentación existentes para mantener compatibles los seguimientos y mensajes guardados. Los índices de ui_source y ui_translation deben corresponder; las variables usan {0}, {1}, etc., y las traducciones pueden cambiar su orden. La prueba de integridad comprueba que ambas listas y sus variables coincidan.

LocalizedText usa los recursos de la configuración actual de Compose, por lo que la recreación normal de Android al cambiar idioma vuelve a presentar los textos. MyBetLiveApplication proporciona los recursos a notificaciones, accesibilidad y transporte HTTP, incluso si llega una notificación con la app cerrada. Los títulos dinámicos se localizan por segmentos sin modificar el modelo de dominio.

Cada petición HTTP incluye Accept-Language: en o es, calculado al enviarla. El backend localiza ApiErrorResponse según ese encabezado y devuelve Content-Language y Vary: Accept-Language. Los códigos, estado HTTP, rutas y campos de validación siguen iguales. Sin encabezado, con idiomas no soportados o con un encabezado inválido, mantiene español para no cambiar la administración web existente.

Los mensajes ingleses del servidor están en seed-backend/src/main/resources/i18n/api_errors_en.properties. Un código nuevo sin traducción recibe un mensaje genérico seguro en inglés según el estado HTTP. Las validaciones de campo conservan su nombre y usan Invalid value en inglés. La autenticación rechazada por Spring Security también devuelve el formato localizado.

Verificación: recursos en en-US/en-GB/es-MX y alternativa fr-FR; variables y nombres propios; transmisión del encabezado sin cambiar Authorization; pantallas en ambos idiomas; notificaciones; errores de autenticación y validación. Las pruebas no necesitan consumir APIs deportivas.
