# Selector de líneas de props

Archivo Penpot: MyBetLive. Página: **07 · Selector de líneas**.

Diseño editable con 26 estados de ejemplo para fútbol, NBA y NFL. Se reutilizan la marca Impulso, colores y tipografías existentes. Los ejemplos son ilustrativos; no son ofertas ni cuotas de un casino.

## Interacción

- Elegir jugador y estadística, luego seleccionar una opción de línea: sin campo numérico, teclado ni opción Otro.
- Opciones rápidas y selector cerrado Ver todas las líneas. Las listas largas se agrupan por rangos y se desplazan; NFL muestra ejemplos exactos 223.5 y 267.5.
- Over/Under conserva la línea. Enteras y Con .5 son formatos separados; una igualdad con línea entera es push.
- Sin línea elegida, Revisar está deshabilitado. Si no hay catálogo, bloquear Guardar y ofrecer Actualizar opciones.
- La revisión presenta jugador, estadística, lado, línea y condición de cumplimiento antes de guardar.
- Cambiar deporte, estadística o jugador revalida la selección. No elegir una aproximación si falta la línea de la apuesta original.

## Prototipos

Flujos de ejemplo: **Prop / Goles · elegir y guardar** y **Prop / NFL · línea exacta**. Las pantallas de tiros y NBA muestran la aplicación del mismo patrón. Algunas opciones ilustran el catálogo sin simular todas las combinaciones posibles.

## Implementación pendiente

Esta entrega modifica Penpot; Android todavía utiliza el campo de captura con validación de rango. Para implementar el selector, añadir opciones permitidas al contrato del catálogo, validar pertenencia en backend además de min/max/step y renderizar las opciones recibidas. El catálogo de líneas no debe asumirse equivalente a las cuotas ofrecidas por un casino. Los valores propuestos pueden ajustarse antes de implementarse.

QA: vistas exportadas y revisadas; se comprobaron destinos y límites de los marcos. Manifiesto: penpot-manifest.json.
