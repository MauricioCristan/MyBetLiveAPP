# Ligas de fútbol americano

El código interno NFL se conserva para las cuentas y seguimientos existentes. El proveedor API_SPORTS_NFL ahora permite registrar todas las ligas del catálogo de API-Sports. El 13/10/2026 el catálogo real devolvió NFL (1) y NCAA (2), ambas de USA. No se inventan competiciones que el proveedor no cubre.

## Uso

En Configuración seleccionar API_SPORTS_NFL. En Ligas → Crear, elegir Fútbol americano, seleccionar NFL o NCAA y habilitar la liga. El catálogo valida su ID y completa el nombre y país canónicos. Se conserva la liga NFL existente; no se habilita NCAA automáticamente. El selector y filtros funcionan como en fútbol y béisbol, con catálogo reutilizado durante 24 horas.

Android muestra Fútbol americano y el icono genérico del deporte. En Partidos se puede filtrar por las ligas habilitadas que tengan juegos en el día consultado, y crear el seguimiento desde un partido. El filtro es local.

BallDontLie mantiene únicamente NFL; sus seguimientos conservan el proveedor original. Al seleccionarlo el servidor desactiva la creación de otras ligas.

## Datos y actualización

Se consultan partidos por fecha UTC, sin limitar la llamada a league=1. Se conservan liga, país, temporada y logo. Las ligas desconocidas o deshabilitadas no se publican para crear seguimientos. Sus partidos siguen llegando para actualizar los seguimientos existentes.

Se conservan Moneyline, diferencia de puntos, total, total de equipo y los mercados de jugadores existentes. La API declaró cobertura de jugadores para NFL y NCAA en 2026. Los datos ausentes no se convierten en estadísticas inventadas; los seguimientos esperan datos cuando corresponda. FT y AOT indican final; los totales incluyen prórroga y no se vuelve a sumar su marcador.

La verificación de solo lectura consultó /leagues y /games?date=2026-10-13&timezone=UTC. El segundo endpoint devolvió dos registros. No se crearon ligas o seguimientos ficticios en cuentas de producción.
