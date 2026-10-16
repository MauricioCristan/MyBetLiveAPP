# Ligas de béisbol

Android muestra **Béisbol**, conservando el código interno `MLB` para no romper sesiones, seguimientos ni estadísticas existentes.

## Administración

1. En Configuración, seleccionar API-Baseball (`API_SPORTS_MLB`) para consultar otras ligas. BallDontLie conserva únicamente MLB.
2. En **Ligas → Crear liga**, elegir Béisbol. El catálogo permite filtrar por nombre o país sin nuevas consultas por cada filtro; se reutiliza durante 24 horas.
3. Seleccionar la liga del proveedor y su duración reglamentaria (7 o 9 entradas). Guardar y habilitar tanto la liga como el deporte.

El servidor valida el ID y guarda nombre y país canónicos al crear. No publica ligas desconocidas o deshabilitadas. Deshabilitar una liga impide crear nuevos seguimientos; los anteriores siguen actualizándose. El proveedor consulta juegos por fecha UTC y conserva identidad, país y logo de la competición. Android filtra las ligas presentes en la jornada como en fútbol.

## Resultados

Se mantienen Moneyline, Run line, Total y Total de equipo. Los totales incluyen entradas extra. La duración configurada sirve para reconocer finales completos; juegos abreviados, empatados o incompletos conservan la revisión existente. No se agregan props de jugadores ni mercados de primeras entradas.

## Despliegue y validación

Primero publicar backend con `V17__baseball_leagues.sql`; después admin y Android. La migración conserva MLB y sus valores actuales y agrega la configuración de entradas. No habilita otras ligas automáticamente.

Pruebas: catálogo y reglas del proveedor con fixtures; APIs administrativas y aislamiento por rol; filtros locales de Android; formulario Angular y rutas; migración y restricciones de entradas. Backend local actualizado con PostgreSQL; APK debug apunta a `http://127.0.0.1:8081` por USB. El envío a producción continúa pendiente de commit/push del usuario.

Referencia del proveedor: [API-Sports Baseball v1](https://api-sports.io/documentation/baseball/v1).
