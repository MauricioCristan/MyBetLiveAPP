# Burbuja flotante de seguimiento

Diseño: Penpot, página **11 · Burbuja de seguimiento**. Disponible en Free y Premium sin modificar el límite de picks del plan.

## Uso

Desde el detalle de un pick activo, pulsa **Ver en burbuja**. La primera vez se explica y solicita el permiso de Android **Mostrar sobre otras aplicaciones**. Se abre una burbuja independiente por seguimiento activo, cada una con su propio marcador, estado y posición. Los nuevos activos se agregan mientras la sesión de burbujas siga activa. Toca una para expandirla y arrástrala para colocarla en un borde. La cruz o zona de descarte cierra únicamente esa burbuja, sin borrar el pick ni afectar las demás; no reaparece con las actualizaciones hasta activarla explícitamente otra vez. La acción de la notificación cierra todas. Los resultados finales se conservan en sus propias burbujas hasta cerrarlas. No hay contador compartido ni flechas para cambiar de pick.

Ganador y empate usan una cápsula pequeña de 80 dp: cada escudo con su propio marcador centrado debajo, sin separador de dos puntos, sin el texto de selección. El empate en curso usa contorno verde si se apostó al empate, rojo si ya no está empatado y amarillo para un equipo elegido que está empatando. Todos los demás mercados usan también 80 dp: totales muestran ambos escudos y valor/línea; props, foto y valor/línea; hándicap, escudos y margen ajustado. La vista compacta omite nombre, etiquetas y descripción del mercado; todos esos detalles se conservan en la tarjeta expandida. Los mercados adicionales muestran el valor correspondiente. Un resultado final conserva el marcador o estadística e indica Ganada/Perdida/Push/Cancelada. Datos ausentes no equivalen a cero; sin actualización se indica Dato guardado y no se representa el contorno como favorable en vivo.

La burbuja se oculta en MyBetLive y con el dispositivo bloqueado. Cerrar sesión o revocar el permiso detiene el servicio. No se reactiva sola después de reiniciar el celular o morir el proceso. La posición se conserva, pero no se inicia un seguimiento automáticamente.

## Datos y arquitectura

`LiveTrackingStore` mantiene snapshots por sesión y protege versiones, resultados finales y eliminaciones. `LiveTrackingCoordinator` serializa las lecturas de pantalla y ventana. Activity, notificaciones y servicio comparten una instancia del repositorio y su renovación de tokens. Firebase actualiza los snapshots conocidos del propietario actual, conservando la diferencia entre cero y ausencia de datos y los marcadores por alcance NHL.

Con la app visible se conserva el ciclo de 15 segundos. En segundo plano, con la burbuja activa y picks no liquidados, se lee el backend cada 60 segundos. No se consulta con pantalla bloqueada ni sin red. Las lecturas parciales preservan el historial ya guardado. La fecha visible es la del dato deportivo, no la del último HTTP. Las burbujas no consultan APIs deportivas ni alteran sus intervalos o cuotas.

Backend: `GET /api/v1/tracking/bets?activeOnly=true&includeBetIds=<UUID,UUID,...>&timezone=<zona>`. Devuelve activos del propietario y los picks de las ventanas abiertas aunque hayan finalizado. Mantiene el parámetro singular `includeBetId` por compatibilidad; combina ambos sin duplicados. Los identificadores ajenos no agregan información. Todas las ventanas comparten una sola lectura por ciclo, no una consulta por burbuja. Sin parámetros conserva el listado existente. Filtrado en persistencia, solo lectura, sin migración.

## Android y publicación

Servicio en primer plano `specialUse` para la ventana interactiva solicitada por el usuario, con notificación privada de baja importancia y cierre accesible. Declarar el permiso de overlay, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` y el subtipo en el manifest. La declaración y demostración del servicio deben presentarse en Play Console antes de publicar; su aceptación no está garantizada por las pruebas locales.

No hay anuncios en la ventana. Las pantallas protegidas por Android u otras aplicaciones pueden ocultar overlays. No se fuerza su aparición sobre esas pantallas ni se solicita desactivar protecciones.

## Verificación

Pruebas automatizadas de propietarios y versiones, respuestas parciales, finales, cero frente a datos ausentes, scopes NHL, cadencia y exclusión del ciclo en primer plano. Pruebas Compose de controles, desplazamiento y texto grande. Backend: propietarios, estados, inclusión del final, compatibilidad, validación y autorización mediante proveedor demo.

Prueba física: HONOR YAL-L21, Android 10. Se concedió el permiso desde la pantalla del sistema y se verificaron aparición fuera de MyBetLive, expansión, minimización, arrastre a ambos bordes, descarte y apertura del detalle. El pick temporal fue eliminado al terminar; los cuatro resultados previos del historial se conservaron. Los controles Compose también se probaron en API 35. La aceptación del servicio en Android 14–16 y Play Console requiere la validación del dispositivo/emulador y la revisión de publicación correspondientes; no se ha publicado.


