# Un seguimiento extra por anuncio

Estado: aprobado e implementado en modo de prueba. Fecha: 2026-10-05.

## Experiencia recomendada

Free conserva el límite normal de su plan, actualmente 1 seguimiento activo. Al alcanzarlo, puede elegir **Ver anuncio y desbloquear 1 seguimiento extra** o consultar Premium. El anuncio es voluntario y el beneficio se explica antes de abrirlo.

Un anuncio completado permite crear un único seguimiento adicional de equipos. La recompensa no incluye props ni modifica el plan. El usuario aprobó como máximo un seguimiento recompensado activo por cliente. El seguimiento extra conserva notificaciones y acceso hasta terminar; nunca se interrumpe a mitad del partido porque pase cierto número de horas.

Cuando finaliza el seguimiento recompensado, otra recompensa requiere ver otro anuncio. Si termina primero el seguimiento normal, su espacio normal vuelve a estar libre aunque el extra continúe. Eliminar un seguimiento no devuelve el anuncio consumido ni permite acumular créditos reutilizables. Premium sigue sin anuncios.

Si el anuncio no carga, se cierra sin completar o falla, se mantiene el límite actual y se permite reintentar. La app muestra la recompensa pendiente de confirmación cuando el servidor todavía no recibe la verificación. No debe prometer un espacio que todavía no existe en el servidor.

## Cambios implementados

- Android: integrar el formato Rewarded de AdMob, botón en el bloqueo por límite y estados de carga, cierre, recompensa pendiente y espacio disponible. Reutilizar consentimiento e aislamiento entre IDs debug/release.
- Backend: persistir intentos y recompensas, asociar cada consumo a un seguimiento y comprobar el límite bajo el bloqueo de cliente que ya utiliza `AccessService.requireCreate`. Consumir la recompensa en la misma transacción que crea el seguimiento, sin perderla si la creación falla.
- Verificar callbacks de AdMob mediante firma de Google, cliente, intento y bloque permitido. Registrar el identificador de transacción de forma única: reintentos del proveedor no crean recompensas duplicadas. Una confirmación enviada únicamente por Android no concede beneficios en producción.
- Administración: interruptor del formato recompensado e ID independiente del bloque Rewarded. Conservar los controles existentes de anuncios nativos; activar o desactivar nuevos anuncios no elimina seguimientos ya obtenidos.
- Persistencia: nueva migración para intentos/recompensas y su relación con seguimientos. Los planes y las compras permanecen sin cambios.

Se aplica un máximo de **un extra activo**. Los créditos todavía no utilizados vencen 24 horas después de la confirmación; una vez usados, el seguimiento continúa hasta terminar. Esta caducidad no cambia el límite normal del plan.

## Validación

Probar dos creaciones simultáneas, recompensa duplicada, firma inválida, otro cliente, premio de otro bloque, cierre temprano, cuota completa, creación rechazada sin consumo, reinicio del backend, vencimiento de crédito, finalización del normal antes del extra y Premium vigente. Usar proveedores simulados y anuncios de prueba.

## Producción

El bloque **Rewarded** propio se creó el 6 de octubre de 2026: `ca-app-pub-7769623175751624/1529195275`, para el App ID `ca-app-pub-7769623175751624~9357829812`. Sigue pendiente guardarlo en Settings y configurar su verificación SSV mediante una URL pública HTTPS del backend. El backend local de la laptop no recibe callbacks de Google directamente.

El usuario eligió mantener los grupos de anuncios (pods): AdMob puede servir dos videos seguidos por una sola impresión recompensada. No se conceden dos créditos; sigue siendo una recompensa `tracking_slot`, cantidad `1`, con máximo un seguimiento extra activo. La aplicación espera la señal de recompensa del SDK y, en producción, la confirmación firmada del servidor.

En AdMob configura la recompensa `tracking_slot`, cantidad `1`, y la URL `https://TU_BACKEND/api/v1/rewards/admob/ssv`. El backend verifica firma, bloque, cuenta, intento, fecha, cantidad, tipo y transacción única. Android informa de la recompensa visual; en producción no concede acceso por sí mismo.

Settings añade `mybet.ads.rewardedEnabled` e `mybet.ads.rewardedUnitId`. El segundo es distinto del nativo. La activación de producción requiere modo de prueba apagado y un ID propio válido; sin él no se ofrece el botón en release.

El perfil `mybet-local` habilita `mybet.rewarded.test-enabled=true` para probar el anuncio de muestra. Por defecto esta propiedad es **false**: los intentos y confirmaciones de prueba son rechazados en producción. Nunca activar el perfil local en un servidor público.

API autenticada: `POST /api/v1/rewards/intents`, cuerpo `{"test":false}`. Devuelve ID, bloque, identificador opaco de cuenta y vencimiento del intento (15 minutos). Android configura esos identificadores en SSV antes de mostrar el anuncio. `/api/v1/subscriptions/me` incluye `reward` con elegibilidad, crédito disponible, vencimiento y extras activos. La confirmación `/intents/{id}/test-complete` solo funciona en el entorno de prueba habilitado.

Migración: `V11__rewarded_tracking.sql`; tabla `tracking_reward` y asociación única `tracked_bet.reward_id`. No se modifican planes ni seguimientos existentes.

Fuentes: [Rewarded Android](https://developers.google.com/admob/android/rewarded), [verificación SSV](https://developers.google.com/admob/android/ssv).
