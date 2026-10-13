# Totales estadísticos de fútbol

Fecha: 11 de octubre de 2026.

En Nuevo seguimiento → Totales aparecen Tiros de esquina, Total de tarjetas, Remates y Remates a puerta. Se usan los selectores horizontales, Over/Under y la rueda vertical de líneas existente. El catálogo y los límites proceden del backend. Texto traducido a español/inglés según el dispositivo.

Se muestran el valor estadístico y la línea en seguimiento, historial, notificaciones y burbujas de 80 dp, con los estados de evaluación existentes. El marcador del partido sigue siendo el marcador; no reemplaza la estadística del mercado. Datos ausentes muestran espera, sin inventar cero.

Regla acordada para tarjetas: amarilla 1, roja directa 2, doble amarilla 2 en total, amarilla y roja directa 3. Solo jugadores en cancha al recibirlas; no cuentan banca, cuerpo técnico, sustituidos ni ya expulsados. Alcance: 90 minutos más compensación. La explicación se conserva en el detalle; se omite del formulario de Nuevo seguimiento.

API-Football debe proporcionar alineaciones, eventos y estadísticas suficientes. Si no permite comprobar el conteo, el backend conserva el seguimiento pendiente de datos. Las pruebas automatizadas no consumen cuota real.

Verificación: 41 pruebas backend y 132 Android sin fallos; APK y frontend nfl-live compilados. APK instalada en HONOR YAL-L21. En teléfono se comprobaron los cuatro mercados en el selector horizontal, sus unidades y ejemplos de rueda (9.5 córners, 4.5 tarjetas, 25.5 remates, 8.5 remates a puerta). No se guardaron picks de prueba en la cuenta del teléfono. Liquidación y reglas se verificaron con proveedores simulados, sin cuotas reales en las pruebas automatizadas.
