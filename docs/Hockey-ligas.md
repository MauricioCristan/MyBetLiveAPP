# Ligas de hockey

El código interno NHL se mantiene para conservar los seguimientos. API_SPORTS_NHL (API-Hockey) ya no limita el catálogo a NHL.

En Configuración usar API_SPORTS_NHL. En Ligas → Crear, seleccionar Hockey sobre hielo y elegir una competición del catálogo por país o nombre. El servidor valida el ID y guarda los datos canónicos. La liga y el deporte deben estar habilitados; las ligas nuevas no se habilitan automáticamente.

En Android, Partidos → Hockey sobre hielo permite filtrar localmente las ligas habilitadas que tengan juegos en la fecha seleccionada. Desde cada partido se crea el seguimiento. Se usa un icono de hockey genérico, conservando los logos de cada competición.

## Actualización y mercados

La consulta por fecha UTC recibe todos los juegos. Se conservan liga, país, temporada y logo. Los juegos de ligas deshabilitadas o desconocidas no se publican para nuevos seguimientos, pero siguen disponibles para actualizar registros anteriores.

Se conservan los mercados de equipo, las reglas de tiempo reglamentario, períodos, prórroga y penales. FT, AOT y AP representan final; los marcadores finales no se vuelven a sumar con el desglose. Los períodos o marcadores ausentes siguen esperando datos. No se agregan props de jugadores: API-Hockey no proporciona estadísticas individuales verificables para esta integración. Bank mantiene confirmación de liquidación del casino.

## Verificación real

El 13/10/2026 /leagues devolvió 262 competiciones. Ejemplos: NHL 57 y AHL 58 (USA), KHL 35 (Russia), SHL 47 (Sweden), Liiga 16 (Finland), DEL 19 (Germany). /games?date=2026-10-13&timezone=UTC devolvió 57 juegos. No se crearon ligas ni seguimientos ficticios de producción.

Documentación oficial: https://api-sports.io/documentation/hockey/v1
