# Web admin MyBetLive — propuesta Penpot

Objetivo: extender la identidad aprobada de MyBetLive al portal administrativo. Fondo oscuro, superficies azul carbón, verde lima para acciones y verde/ámbar/rojo para estados. Reutilizar Impulso (logo aprobado) y tipografía existente.

## Vistas previstas

- Acceso administrativo.
- Dashboard: clientes, Free/Premium, accesos vigentes, deportes/ligas, planes y próximos vencimientos. Sin ingresos, picks ni marcadores.
- Clientes: lista y crear/editar cliente.
- Suscripciones: lista, detalle y alta de acceso manual.
- Deportes: lista y crear/editar; conservar catálogo e integración como estados distintos.
- Ligas: lista y crear/editar.
- Planes: lista y crear/editar.
- Operación de APIs: resumen por deporte, selección de proveedor y edición de intervalos, presupuesto, caché y pausa.
- Publicidad: anuncios Free, ubicaciones, frecuencia, recompensa y estado de preparación. Las credenciales permanecen fuera de Settings.
- Estados vacíos, carga, error y cambios sin guardar.
- Adaptaciones móviles de dashboard, clientes y operación.

## Reglas de la propuesta

Datos ilustrativos, no métricas reales del proyecto. El cambio de proveedor afecta nuevos eventos/picks; los existentes conservan su proveedor original. Mostrar ambos consumos cuando correspondan. Guardar o recargar no fuerza consultas externas. BALLDONTLIE Free admite mercados de equipo; estadísticas individuales NBA/NFL requieren acceso compatible; MLB mantiene sus mercados actuales.

La entrega es de diseño editable en Penpot. No cambia el frontend implementado ni beneficios, cobros o datos existentes.

## Entrega completada

Penpot: archivo MyBetLive, páginas **09 · Web admin** y **10 · Web admin móvil**. 25 pantallas de escritorio y 7 móviles, con cuatro entradas de prototipo: Acceso, Navegación, Proveedores y navegación móvil.

Incluye crear/editar clientes, deportes, ligas y planes; acceso manual y detalle de suscripción; configuración independiente MLB/NBA/NFL/fútbol; confirmación de cambio de proveedor; publicidad; carga, vacío, error, guardado y cambios pendientes. Las pantallas móviles cubren dashboard, clientes, menú, proveedores y edición de MLB/NBA/NFL.

Se reutilizaron colores de la biblioteca, Inter Tight y el emblema Impulso aprobado. Sidebar con iconos vectoriales, controles editables y enlaces de navegación. La revisión visual cubrió dashboard de escritorio/móvil y Operación de APIs. Comprobación de límites: cero elementos o textos fuera de los marcos.

- penpot-manifest.json: identificadores, tamaños y flujos.
- dashboard.png, operations.png y mobile-dashboard.png: vistas exportadas para revisión.
- Archivos SVG: exportaciones nativas y previews con la misma tipografía incrustada. Los previews permiten revisar sin depender del servidor de fuentes de Penpot.

Los números son ilustrativos. El nuevo tema todavía no está aplicado al frontend.
