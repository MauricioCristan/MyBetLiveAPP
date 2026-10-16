# Verificación de entradas en API-Baseball

Consulta realizada el 13 de octubre de 2026. Revisión de solo lectura; no se modificaron ligas ni seguimientos.

## Muestra

GET /games con season=2024, timezone=UTC y league=22 (LMP), 21 (LMB), 1 (MLB). Se obtuvieron 380, 1002 y 2946 registros respectivamente: 4328 registros en total. La respuesta para temporadas 2025/2026 indicó que el plan Free solo permite temporadas 2022 a 2024. El catálogo y las consultas de 2024 respondieron correctamente.

## Casos reales

Fechas convertidas a UTC-6 (Ciudad de México). La columna de entrada indica la última entrada numerada con valor en scores.away.innings; no representa necesariamente la duración programada.

| Liga | Fecha | ID | Local vs visitante | Marcador | Estado | Última entrada visitante informada |
|---|---|---|---|---|---|---|
| LMP | 11/10/2024 | 161009 | Hermosillo vs Yaquis de Obregon | 4–2 | FT | 9 |
| LMP | 20/10/2024 | 161037 | Hermosillo vs Mazatlan | 2–1 | FT | 7 |
| LMP | 20/10/2024 | 161045 | Hermosillo vs Mazatlan | 5–4 | FT | 8 |
| LMP | 16/10/2024 | 161026 | Monterrey vs Aguilas de Mexicali | 0–6 | FT | 6 |
| LMB | 11/05/2024 | 159122 | Quintana Roo vs Bravos de Leon | 2–5 | FT | 7 |
| LMB | 11/05/2024 | 159109 | Quintana Roo vs Bravos de Leon | 3–4 | FT | 8 |

Los dos registros Hermosillo–Mazatlan corresponden al mismo día local y tienen distintos IDs. Esto es compatible con una doble jornada, pero la respuesta no incluye una bandera que la confirme. Tampoco confirma que el juego de ocho entradas fuera uno programado a siete con una entrada adicional.

## Campos y límites

Se revisaron las propiedades de los 4328 registros, incluidas las propiedades anidadas. No apareció un campo de entradas programadas, duración reglamentaria o número de juego de doble jornada. La información de entradas está únicamente en scores.home.innings y scores.away.innings (1 a 9 y extra).

Un valor null no debe tratarse como cero. Puede corresponder a una entrada no disputada o a datos no informados. En el juego 161009, la novena local es null mientras la visitante sí tiene valor: el local terminó por delante y el marcador es compatible con que no necesitara batear la parte baja.

La API informa FT para los casos de seis, siete y ocho entradas, pero no da una causa de terminación ni confirma la duración programada. No se verificaron reglas de liquidación de ningún casino.

## Consecuencia para MyBetLive

Una configuración fija de siete o nueve entradas por liga no describe todos sus partidos. La duración debe poder especificarse por partido cuando se conozca; los casos sin información suficiente requieren revisión. No es fiable inferir la duración programada solo de la última entrada informada ni del hecho de que haya dos juegos en un día. FT confirma el estado final reportado por el proveedor, no las reglas de liquidación del casino.

Documentación oficial: https://api-sports.io/documentation/baseball/v1

Las respuestas originales quedaron en la carpeta local ignorada del backend .local/baseball-innings-audit; no contienen la clave usada para las consultas.
