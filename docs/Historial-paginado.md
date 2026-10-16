# Historial en bloques de 10

## Seguimientos

Android obtiene los activos mediante GET /api/v1/tracking/bets?activeOnly=true. Ya no usa la consulta completa para cada actualización de 15 segundos. Incluye los IDs que estaban activos para recibir también sus resultados al finalizar.

GET /api/v1/tracking/bets/history admite page (desde cero), size (predeterminado 10, máximo 10), sport opcional y timezone. Devuelve items, page, total y hasMore. El backend filtra el propietario del JWT, estados WON/LOST/PUSH/VOID y deporte antes de paginar en SQL. Ordena por updatedAt e id descendentes. V18 agrega el índice del historial; no modifica los registros.

El historial solicita diez resultados iniciales y diez más al acercarse al final. Al cambiar el deporte vuelve a la primera página; el filtro se ejecuta en el servidor. Al refrescar o eliminar se recarga la primera página. Los resultados se deduplican por ID; el total corresponde a la última consulta del servidor. Las páginas usan desplazamiento: si otro dispositivo modifica el historial mientras se recorre, una actualización vuelve a consultar desde el inicio.

Las cargas de historial y la actualización de activos comparten el coordinador de acceso a la caché. Una página no reemplaza los activos ni mezcla cuentas. Las respuestas de otra sesión se descartan; los registros borrados no se resucitan. Sin conexión se conserva el contenido guardado y una carga fallida ofrece Reintentar. Las notificaciones pueden consultar el detalle por ID aunque no esté entre las páginas cargadas.

El endpoint antiguo que devuelve todos los seguimientos permanece por compatibilidad, pero la app no lo utiliza para cargar o refrescar la lista.

## Bank

Apuestas y movimientos usan diez elementos por página en Android y como tamaño predeterminado del backend. Conservan los controles Anterior/Siguiente y filtros existentes.

## Despliegue

Publicar primero el backend con V18 y después la APK. La instalación local conserva los datos y mantiene http://127.0.0.1:8081 mediante el puente USB.
