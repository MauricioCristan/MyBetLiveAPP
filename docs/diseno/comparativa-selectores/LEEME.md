# Tres controles para elegir líneas

Penpot: MyBetLive → **08 · Comparativa de selectores**. Seis pantallas editables: cada propuesta aplicada a goles y a yardas NFL.

1. Carrusel horizontal: deslizar de lado a lado, centrar y ajustar a una tarjeta. Adecuado para pocos valores.
2. Rueda vertical: desplazar arriba/abajo; la fila central indica la elección. Preferencia propuesta para unificar props y seleccionar cifras exactas en listas largas.
3. Deslizador discreto con −/+: recorrer opciones permitidas y afinar con botones. Para yardas se muestra un rango acotado, evitando intentar arrastrar un catálogo entero con poca precisión.

En los tres casos el valor procede de una opción del catálogo, nunca de escritura libre o de un valor continuo calculado con el gesto. Las pantallas son propuestas; el gesto nativo y su validación de pertenencia todavía se implementarán en Android después de elegir la variante.

QA: revisión visual de las tres propuestas, comprobación de marcos y conservación del resto del archivo. Manifiesto: penpot-manifest.json.

## Variantes adicionales
4. Rueda con − / +: desplazamiento vertical con ajuste de una opción por toque.
5. Regla horizontal: mover las cifras bajo un indicador fijo y ajustar a una opción del catálogo.
6. Entero y decimal: dos columnas para elegir la parte entera y .0/.5; únicamente combinaciones permitidas.

Ahora hay 12 vistas: seis alternativas aplicadas a goles y yardas. Entre las nuevas se propone la 4; elección del usuario pendiente. Las tres propuestas originales se conservaron.


## Variante elegida e implementada
El usuario eligió la opción 2, rueda vertical sin botones de ajuste. Android la utiliza para props, totales y hándicaps, con ajuste al centro, selección por toque, filtros enteras/.5 y valores del catálogo del backend. Las otras cinco variantes quedan como propuestas de diseño.
