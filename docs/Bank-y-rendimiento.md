# Bank y rendimiento Android

Implementado con Compose usando el diseño de Penpot «12 · Bank y rendimiento», fondo `#0C141B`, paneles `#17232E` y verde de MyBetLive. La navegación agrega Bank cuando `/subscriptions/me` devuelve `bankAccess`; conserva las otras pestañas y el destino de producción.

## Pantallas

Lista/selector de banks privados, creación y edición, resumen de disponible/comprometido/capital, beneficio y ROI del periodo, gráfica confirmada y últimas apuestas. El menú del bank lleva a apuestas, rendimiento, movimientos y herramientas. Incluye detalle, registro manual o vinculado, combinadas/Back/Lay, promociones, importe fijo o porcentaje manual, confirmación/corrección de retorno, depósitos/retiros, notas y valores predeterminados.

El calendario usa la fecha de liquidación y se consulta en Herramientas. Rendimiento incluye TWR, TIR anualizada, CLV y desgloses por deporte, mercado y casino. Los datos insuficientes muestran «—». La cuota de cierre se introduce manualmente; un retorno deportivo sugerido exige confirmación del usuario.

CSV: descargar plantilla versión 1, seleccionar archivo, validar, revisar errores/duplicados y confirmar lote completo. Exportar permite conservar los datos al vencer Premium. Compartir genera PNG con marca, nombre del bank, moneda, periodo, métricas y gráfica; primero muestra vista previa y después abre el selector de Android. El PNG no incluye correo ni identificadores de cuenta.

«Exportar historial financiero completo» conserva también el ledger de correcciones y reversas. Ese archivo de auditoría no se importa como movimientos nuevos.

## Sesión y lectura offline

`BankRepository` y `BankViewModel` independientes reutilizan el coordinador de renovación de sesión existente. Dinero y cuotas llegan como cadenas; las entradas y el porcentaje usan `BigDecimal`. Solo el dibujo de coordenadas usa Float.

La copia del último bank/historial/movimientos/estadísticas consultados se cifra mediante `SessionVault`, separada por cuenta y generación de sesión. Conserva el periodo y fecha de actualización. Sin conexión permite consultar ese último contenido; no mantiene una cola de modificaciones. Cerrar sesión o cambiar de cuenta limpia cache y reportes. Banks no consultados requieren conexión.

Los identificadores de escrituras cuyo resultado no se recibió se conservan cifrados junto con su versión: reintentar los mismos datos reutiliza el ID incluso después de recrear el ViewModel. Una respuesta 409 recarga los datos y exige revisar el cambio.

Cada operación de Bank tiene un límite global de espera de 45 segundos. Al agotarse, se libera el indicador de carga y se solicita actualizar antes de reintentar. Las escrituras sin respuesta conservan su identificador para evitar duplicados si el servidor sí las procesó.

## QA y distribución

El registro y edición de apuestas usa la opción 4 de Figcraft: datos del boleto, selección, importe/formato y detalles/promociones en bloques desplegables. La selección empieza abierta; plegar un bloque conserva el borrador. Guardar permanece fuera del scroll y se deshabilita durante la escritura o sin conexión. La exposición mostrada es una vista previa con BigDecimal; los saldos siguen siendo responsabilidad del backend. Las pruebas visuales cubren 390dp y 320dp con texto grande y conservación de los campos al plegar.

Casa de apuestas es un selector con alta desde «Añadir casa de apuestas». El catálogo pertenece a la cuenta, se consulta y guarda mediante GET/POST `/api/v1/banks/bookmakers` y se conserva en la caché cifrada. V16 incorpora las casas usadas en apuestas y valores predeterminados anteriores. El alta normaliza espacios y evita duplicados sin distinguir mayúsculas; los nombres ya registrados en las apuestas permanecen intactos. Desplegar backend con V16 antes de distribuir esta APK. Se permite usar el catálogo en Free y Premium.

Las fechas de apertura, apuesta, liquidación, movimientos y periodo se eligen con calendario nativo; las horas de apuesta, liquidación y movimientos con reloj nativo. La presentación de hora respeta el ajuste de 12/24 horas del teléfono y la fecha se muestra como dd/MM/yyyy. El contrato conserva YYYY-MM-DD y HH:mm; al guardar se convierte a Instant usando la zona horaria del bank. Cancelar el selector no cambia el borrador.

El detalle de apuesta sigue la propuesta de Figcraft: importe/cuota/exposición agrupados, tarjeta de retorno, información del boleto y acción de liquidación fuera del scroll. Mientras está pendiente muestra retorno potencial y propuesta deportiva sin contabilizarlos como dinero confirmado; después de liquidar muestra retorno recibido y beneficio neto. El menú superior conserva modificar/copiar/eliminar y la confirmación de borrado. Combinadas muestran todas sus selecciones; notas, CLV y promociones están en detalles desplegables. Las acciones de escritura se deshabilitan sin conexión.

Pruebas locales: `BankViewModelTest`, `BankScreensTest` y renovación compartida en `AuthorizedCallsTest`, además de la suite previa. Capturas de revisión generadas en `app/build/bank-screenshots` con Android 15, pantalla 390dp y 320dp con texto 1.3. Los datos de esas pruebas son fixtures locales y no se envían a producción.

La revisión del HONOR usa `BankDevicePreviewActivity`, solo en debug, no exportada y sin acceso a sesión/API. `scripts/Validar-BankDispositivo.ps1` compila e instala con `-r` y ejecuta instrumentación directamente. Evitar `connectedDebugAndroidTest` sobre un teléfono con datos reales: su ejecutor puede desinstalar la APK al finalizar. Las capturas físicas quedan en `files/bank-qa`, accesibles con `adb exec-out run-as com.mybetlive cat files/bank-qa/...png`.

Compilar con JDK 21:
```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
.\gradlew.bat :app:assembleRelease :app:bundleRelease '-PMYBET_ADMOB_APP_ID=ca-app-pub-7769623175751624~9357829812'
```

La firma de release se obtiene del archivo privado existente; no incluir claves/contraseñas en Git. Primero desplegar backend y V15; después instalar la APK. Usar `adb install -r` con la misma firma para conservar datos. No desinstalar una versión con otra firma sin autorización. El módulo no necesita cambios en API-Sports ni panel web financiero del admin.

Para usar el backend local por USB: compilar debug con `-PMYBET_DEBUG_BACKEND_URL=http://127.0.0.1:8081` y activar `adb reverse tcp:8081 tcp:8081`. El puente debe reactivarse al reconectar el teléfono. Una selección explícita de otro backend invalida únicamente la sesión/caché local del servidor anterior y pide iniciar sesión en el nuevo entorno; nunca reutiliza sus tokens. Release y las actualizaciones normales conservan su comportamiento anterior.

## Navegación flotante de Bank (opción 3)

Inicio, Apuestas y Estadísticas permanecen en una barra inferior flotante dentro de Bank. La navegación principal se oculta en este módulo; la flecha superior vuelve a Seguimientos. El botón Atrás vuelve primero a Inicio desde otra pestaña y después sale del módulo. Cada cambio de sección comienza arriba del contenido. Movimientos y Herramientas conservan acceso desde el menú superior.

Inicio prioriza disponible, capital y comprometido, seguido por rendimiento. La barra tiene espacio propio para no cubrir controles ni la última fila. Apuestas conserva paginación de 10 y Estadísticas muestra el resumen básico junto al acceso Premium a métricas avanzadas. Sin conexión se conserva la lectura y se deshabilitan las escrituras. No cambia contratos, ledger ni datos del backend.

La barra principal de Seguimientos, Partidos, Bank y Perfil utiliza el mismo acabado flotante: fondo azul oscuro, esquinas redondeadas, iconos y texto verde para la sección activa. Respeta los márgenes y el área de gestos del sistema. Al entrar en Bank se mantiene su navegación interna de Inicio, Apuestas y Estadísticas.

Añadir apuesta tiene un único acceso mediante el botón + en la barra flotante de Bank, disponible desde Inicio, Apuestas y Estadísticas. Se eliminaron los botones del contenido; Movimientos permanece en el menú superior. El botón + respeta las restricciones de conexión, operación en curso y bank de solo lectura.

Apuestas sigue la opción 3 de FigCraft: título Tus apuestas, buscador, chips Todas/Pendientes/Filtros, agrupación por fecha de colocación en la zona del bank y tarjetas con franja de estado. La búsqueda se envía con q (máximo 120 caracteres, título o casino, sin distinguir mayúsculas) antes de paginar; los siguientes/anterior conservan q y el resto de filtros. Se mantiene el único botón + en la barra flotante. Actualizar backend antes de distribuir la APK para habilitar la búsqueda global.

Inicio de Bank no muestra últimas apuestas ni acceso Ver todas. El listado y los detalles de apuestas se consultan desde la pestaña Apuestas.

La pestaña Apuestas carga los registros mediante scroll continuo en bloques de 10. Acumula las páginas sin duplicar IDs, conserva búsqueda/filtros en cada petición y detiene la carga al terminar. Ante error conserva lo ya cargado y ofrece Reintentar; no repite automáticamente una petición fallida. Actualizar o aplicar filtros reinicia desde los primeros 10. Movimientos conserva su paginación independiente.

Apuestas permite agrupar por Día, Semana (lunes a domingo) o Mes, usando la fecha de colocación en la zona horaria del bank. La agrupación se aplica a los registros cargados, reúne las páginas en el mismo encabezado y mantiene filtros y scroll de 10 en 10; cambiar de agrupación no descarga todo el historial. Los encabezados semanales incluyen fechas y año para evitar ambigüedades entre años.

Los encabezados de Día/Semana/Mes son expandibles. Mes es la vista inicial. Cada encabezado conserva ganado, perdido y balance confirmado aunque esté contraído. Los totales vienen de groups en GET /bets, calculados antes de paginar con todos los registros que cumplen los filtros y por fecha de colocación en la zona del bank; las pendientes solo aumentan su contador. No son la suma de los diez registros visibles. Contraer el último grupo detiene la carga automática; Cargar más periodos permite avanzar sin expandirlo. Desplegar backend antes de Android para disponer de estos totales.

Se retiró el selector Día/Semana/Mes. La lista muestra meses desplegables con semanas desplegables dentro, seguidas por sus apuestas. MONTH_WEEK devuelve totales de cada tramo semanal dentro del mes; una semana que cruza de mes se divide para que los totales de los hijos cuadren con su mes sin doble conteo.

En Apuestas se eliminó el botón Filtros y su formulario de deporte, casino, fechas y estado avanzado. Permanecen el buscador y los accesos Todas/Pendientes; sus peticiones envían solo búsqueda y pendiente opcional. Los filtros de Estadísticas se conservan de forma independiente.

El encabezado semanal es compacto: rango como 12 a 18 oct, balance firmado en cápsula (verde positivo, rojo negativo, neutro cero) y flecha desplegable. Los detalles de ganado/perdido permanecen en el encabezado mensual; la semana muestra únicamente su balance confirmado.

Solo el mes es contraíble. La semana es un separador estático con rango y balance; sus días siempre se muestran mientras el mes esté abierto. Cada día tiene un contenedor único con título, balance completo y filas de apuestas separadas por divisores; cada fila conserva acceso al detalle. La precarga comprueba la distancia real al final del bloque diario, para evitar descargar todo cuando un solo día ocupa más que la pantalla.

El encabezado mensual se compactó a nombre del mes, balance neto firmado y flecha de expansión. No muestra contadores ni desglose de ganado/perdido; conserva el total completo calculado en el backend, con una cápsula verde, roja o neutra y un borde que indica el estado de expansión.

Se retiró Movimientos de Android y backend: menú, formulario, estado/caché cliente, consultas y endpoints GET/POST/PUT /banks/{id}/movements y POST .../delete. Los CSV MOVEMENTS ya no se aceptan para plantilla, exportación ni importación; la interfaz trabaja solo con apuestas. Se conservan los asientos históricos y el ledger de apertura/reserva/liquidación/corrección para no alterar saldos ni métricas; LEDGER permanece como exportación de auditoría de solo lectura. No requiere borrar datos ni modificar migraciones publicadas. Actualizar primero Android o en una ventana coordinada: las APK antiguas que consultan movimientos dejarán de poder recargar al retirar su endpoint.

El título de Bank muestra el nombre del bank activo en todas sus secciones. Al tocarlo abre el selector con los banks de la cuenta, marca el actual y ofrece Crear otro bank; mantiene el límite Free/Premium y bloquea creaciones sin conexión. La moneda y privacidad permanecen como metadatos debajo, sin repetir el nombre.


Home de rendimiento: gráfica principal con escala y fechas, accesos Día/Semana/Mes/Año (hasta hoy en zona del bank) y periodo personalizado. Tarjetas de apuestas del periodo, beneficio, ROI y progresión; saldos actuales en bloque secundario. Se retiraron MXN · Privado y Tu dinero, en un vistazo.
El rango se comparte con historial y estadísticas, incluidas búsquedas y páginas posteriores de diez. Cambiar periodo reinicia historial y estadísticas; conserva búsqueda/estado y filtros estadísticos. Si falla la carga conserva el periodo anterior. El snapshot offline guarda el mismo rango y no permite modificarlo.
metrics.bets y summary.totalBets cuentan apuestas por fecha de colocación dentro del periodo, incluidas pendientes. metrics.pending y summary.pending cuentan las pendientes colocadas en él. metrics.settledBets cuenta liquidaciones del periodo; beneficio/ROI/gráfica mantienen fecha de liquidación. Una apuesta de otro periodo liquidada en este sí aporta rendimiento. Los saldos actuales no se recortan por fecha. No cambia migraciones ni saldos.

Ajuste del Home: primero las cuatro tarjetas de Apuestas del periodo, Beneficio, ROI y Progresión; debajo, la gráfica y sus controles de periodo. Se retiran de esta pantalla Saldo actual (disponible/capital/comprometido) y el texto explicativo por fecha de registro/liquidación. Los cálculos financieros y el rango compartido no cambian.

16 de octubre: se elimina el título Evolución del beneficio del Home. La gráfica inicia en 110 dp y se amplía a 240 dp al tocarla; otro toque la reduce. El rango queda visible y Personalizado comparte la fila desplazable de Día/Semana/Mes/Año, sin cambiar la aplicación del periodo a Apuestas y Estadísticas.

El menú de tres puntos incluye Compartir imagen: abre la vista previa existente del resumen antes del selector de compartir de Android. Mantiene el acceso según el plan y permite usar el último resumen guardado sin conexión.
