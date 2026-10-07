# Requests, responses e interfaz NFL, fútbol y MLB

Captura del 2 de octubre de 2026, con el backend local conectado a API-Sports. Las respuestas están guardadas: puedes revisarlas y diseñar sin ejecutar requests ni gastar cuota. Los IDs, marcadores y métricas pertenecen a esta captura; no son un feed actualizado.

Importa **MyBetLive.postman_collection.json** en Postman o revisa **MyBetLive.http**. Los ejemplos de respuesta están incluidos en la colección y en **responses/**. Las variables de tokens y clave están vacías; no se incluyeron credenciales reales.

La colección incluye 16 requests y 13 respuestas capturadas. El login y el POST de creación tienen sus requests preparados; el seguimiento creado desde Android se recuperó con GET y su respuesta está guardada en app-bet.json. Los dos requests de Google cubren desafío y acceso/registro. Se guardó su respuesta GOOGLE_NOT_CONFIGURED; no se guardaron respuestas de login con tokens.

Fútbol tiene su propia colección **Futbol.postman_collection.json** y **Futbol.http**, con 14 requests y 10 respuestas guardadas del portal/API. Los ejemplos de fútbol están en **../verification/soccer-2026-10-02/**: catálogo, jugadores/fotos, cuatro seguimientos, ligas y cuota. Status del proveedor está sanitizado. La app usa UUID internos; el fixture crudo de ejemplo es 1520908. Obtén eventos/versiones actuales antes de repetir altas o cambios; H2 puede reiniciarse.

Desde `/admin/mybet/football` se administran las ligas disponibles en Android: búsqueda, alta y estado con versión. Deshabilitar conserva seguimientos e impide nuevos registros. Los mercados de fútbol son 1X2, hándicap, total partido/equipo y props de goles, asistencias, tiros y tiros a puerta, durante 90 minutos más compensación. Líneas enteras/medias; prórroga/penales y cuartos quedan fuera. [Reglas y prueba real](../verification/soccer-2026-10-02/resultado.md).

## Lo que el cliente puede seleccionar hoy

MLB tiene **MLB.http** y **MLB.postman_collection.json** (16 requests y 15 respuestas guardadas), con respuestas del proveedor y del backend en **../verification/mlb-2026-10-02/**. Ofrece ganador local/visitante, Run Line, total de carreras y carreras del equipo: juego completo con entradas extra, líneas enteras/medias. API-Baseball carece de jugadores/estadísticas individuales, así que no hay props ni fotos de jugadores MLB. Marcadores futuros null se conservan; finales abreviados/incompletos esperan datos. [Reglas y prueba real](../verification/mlb-2026-10-02/resultado.md).

| Grupo | Mercado | Selección | Dato adicional obligatorio |
| --- | --- | --- | --- |
| Resultado | Moneyline | Equipo local o visitante | Ninguno |
| Resultado | Spread | Equipo local o visitante | Hándicap, por ejemplo -3.5 |
| Totales | Total del partido | Más de / menos de | Línea de puntos |
| Totales | Total del equipo | Más de / menos de | Equipo y línea de puntos |
| Jugadores | Yardas de carrera | Más de / menos de | Jugador y línea |
| Jugadores | Yardas de recepción | Más de / menos de | Jugador y línea |
| Jugadores | Recepciones | Más de / menos de | Jugador y línea |
| Jugadores | Yardas de pase | Más de / menos de | Jugador y línea |
| Jugadores | Touchdowns de pase | Más de / menos de | Jugador y línea |

La app muestra primero deporte, partido, grupo y mercado; después presenta únicamente los campos necesarios. El resumen Tu selección reúne equipo/jugador, mercado y línea. Monto, momio decimal y casino son opcionales y se despliegan aparte. NFL, fútbol y MLB están habilitados en el perfil real.

`availableMarkets` en la respuesta de jugadores permite filtrar qué jugadores tienen datos para cada estadística. En esta captura hay 19 participantes con métricas soportadas, dos con estadísticas de pase y quince con recepciones. Se basa en datos disponibles del encuentro, no en una predicción de posiciones. Si el campo está ausente o vacío, se conserva compatibilidad con rosters/backend anteriores.

`imageUrl` entrega la foto opcional del jugador; `playerImageUrl` la conserva en la respuesta del seguimiento. Android la usa en el selector, el resumen y la tarjeta, con caché y avatar si falta o falla. La URL proviene de `player.image` en las estadísticas del proveedor, sin un request deportivo extra para obtener la foto.

Los nombres y métricas de los partidos LIVE/FINAL salen de sus estadísticas. El plan Free restringe rosters 2026 antes del inicio; el ejemplo de error real también está incluido. La app muestra esa limitación al cargar jugadores. Las líneas, el casino y el momio los introduce el usuario desde la apuesta que ya realizó; actualmente no se ofrecen cuotas automáticas de casas.

## Relación entre datos y pantallas

| Pantalla / control | Request del backend | Campos de la respuesta |
| --- | --- | --- |
| Deporte y mercados | GET /api/v1/tracking/sports | sport, enabled, markets |
| Fecha y partidos | GET /api/v1/tracking/events?sport=NFL&date=... | id, event, startsAt, teams, scores, status |
| Elegir jugador | GET /api/v1/tracking/events/{id}/players | id, name, teamId, availableMarkets, imageUrl |
| Guardar selección | POST /api/v1/tracking/bets | eventId, market, selection, line, team, playerId |
| Mis seguimientos | GET /api/v1/tracking/bets | playerName, playerImageUrl, status, currentValue, remaining, updatedAt, stale |
| Perfil > Mi plan | GET /api/v1/subscriptions/me | plan, access, activeTrackers, offers |

El API de la app usa UUID internos de eventos; el proveedor usa IDs como 21561. No son intercambiables. Las respuestas de API-Sports son crudas; el backend las normaliza para Android y aplica reglas de seguimiento.

## Ejemplo visual y de datos

DK Metcalf → Recepciones → Más de 4.5. El proveedor registró 5 recepciones y el backend devuelve WON. Un Over 5 da PUSH cuando termina en 5. Moneyline y Spread usan equipos, no Over/Under; Total del equipo conserva el equipo elegido en el resumen.

Ese recorrido se verificó en un HONOR YAL-L21 con Android 10: selección de jugador, línea, resumen, guardado y resultado final. Capturas y evidencia: [interfaz en teléfono](../verification/interface-2026-10-02/resultado.md).

El resultado de un partido finalizado se muestra como Resultado / Línea, sin instrucciones de progreso pendiente. La prueba usa registros locales de desarrollo y no coloca apuestas ni activa compras.

La fecha capturada está dentro de la ventana -7/+14 días del catálogo. Para repetir después, cambia date, eventId y playerId por datos actuales. Para ejecutar contra el backend local: scripts/Iniciar-NflReal.ps1 desde seed-backend; URL predeterminada http://127.0.0.1:8081.
