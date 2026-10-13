# Registro y recuperación por correo

Android ofrece Crear cuenta con correo y Olvidé mi contraseña junto al acceso con Google. Registro requiere usuario, email, contraseña y confirmación. El correo debe confirmarse antes del login; la cuenta inicia en Free. Se permite reenviar confirmación y acceder por correo o username. Textos y errores en español/inglés.

Los correos usan páginas públicas HTTPS de MyBetLiveAdmin para confirmar email o cambiar la contraseña, y ofrecen continuar en Android con un intent dirigido exclusivamente a com.mybetlive. El servidor de destino está fijado por la app, no por parámetros externos. Enlaces incompletos o de otros hosts/rutas se ignoran. Se conservan las contraseñas sin recortar espacios y se valida su longitud UTF-8. Las operaciones públicas no envían un JWT guardado, aunque esté vencido.

Las nuevas sesiones apuntan a Railway; sesiones existentes conservan su servidor y picks. El acceso no muestra URL del backend, credenciales demo ni configuración técnica, incluyendo la APK debug instalada para revisión visual. Para desarrollo el destino se configura al compilar con AUTH_BACKEND_URL. El esquema mybetlive://auth/verify y /reset requiere token hex de 64 caracteres; no representa un App Link HTTPS verificado. Para asociación automática de dominio falta el certificado de firma de publicación.

Se debe desplegar primero el backend y las páginas públicas, y configurar SMTP_USER/SMTP_PASS en Railway. Sin SMTP no puede completarse el registro ni enviarse recuperación; el backend responde con un error y revierte una nueva cuenta. Las pruebas usan HTTP/SMTP simulados. No se modifican cuentas reales para QA.

Verificación: 135 pruebas Android sin fallos y APK compilada/instalada. Contrato HTTP de las cinco rutas de correo comprobado con MockWebServer, incluyendo exclusión de credenciales antiguas y conservación de la contraseña exacta. En el celular se revisó la pantalla del enlace de confirmación sin enviarlo al backend ni cambiar cuentas.

## Presentación del acceso

Logo compacto de 34 dp, título de 28 sp y controles de acceso visibles antes del texto informativo. Google aparece en login; registro y recuperación tienen sus propios títulos y formularios. Registro ofrece reenviar la confirmación después de mostrar el resultado del envío. Contraseñas incluyen Mostrar/Ocultar; el teclado permite avanzar entre campos y se cierra al pulsar el botón principal. Los formularios admiten desplazamiento y respetan las áreas del sistema y teclado. Atrás vuelve al login desde registro/recuperación y cierra las pantallas de enlaces. Los valores de contraseña no se guardan en estado persistente.

Revisión visual en HONOR YAL-L21: login, registro, recuperación, teclado abierto y formulario de nueva contraseña con token ficticio sin enviar peticiones ni correos reales. Se conserva español/inglés mediante el catálogo de traducciones.
