# Ligas de baloncesto

## Configuración

El deporte conserva el código interno NBA y se presenta como Baloncesto. Se agrega API_SPORTS_BASKETBALL, que utiliza https://v1.basketball.api-sports.io. API-NBA v2 y BallDontLie se mantienen disponibles y conservan los seguimientos existentes, con sus propios IDs de proveedor.

1. Configurar API_BASKETBALL_KEY en el servidor. Si falta, la integración usa la clave común mybet.api-sports.key (API_SPORTS_KEY).
2. Seleccionar API_SPORTS_BASKETBALL en Configuración, proveedor de Baloncesto. No se cambia automáticamente un proveedor ya configurado.
3. En Ligas, crear liga, seleccionar Baloncesto y elegir una competición del catálogo. Filtrar por país o nombre no hace nuevas llamadas; el catálogo se reutiliza durante 24 horas.
4. Guardar y habilitar liga y deporte. No se habilitan ligas automáticamente.
5. En Android, Partidos → Baloncesto muestra las competiciones habilitadas con partidos en la fecha consultada. El filtro de liga funciona localmente. Desde el partido se crea el seguimiento.

## Cobertura

API-Basketball ofrece Moneyline, diferencia de puntos, total y total de equipo. No se ofrecen props de jugadores en esta integración. API-NBA mantiene los mercados y estadísticas individuales para sus seguimientos originales. No se mezclan ni remapean sus IDs de partidos, equipos y ligas.

La liga NBA de API-Basketball tiene ID 12; el registro histórico NBA:1 corresponde a la integración API-NBA. Son identificadores distintos. FT y AOT son finales, Q1–Q4, HT, BT y OT son estados en vivo. Se usa el marcador total informado, que ya incluye prórroga; no se vuelve a sumarla. Los marcadores ausentes no se inventan.

La API entrega temporadas tanto 2026 como 2026-2027. Se conservan ID, nombre, país, temporada y logo de la competición. Deshabilitar una liga oculta su publicación, pero permite actualizar sus seguimientos anteriores.

## Verificación real

El 13/10/2026 se consultó el catálogo: 428 competiciones, entre ellas LNBP 63, ACB 117, Euroleague 120 y NBA 12. La consulta de juegos del día 2026-10-13 devolvió 71 registros. El plan Free rechazó la fecha histórica 2024-10-13 y permitió el rango 2026-10-13 a 2026-10-15. No se crearon ligas ni seguimientos en cuentas de producción.

Documentación: https://api-sports.io/documentation/basketball/v1
