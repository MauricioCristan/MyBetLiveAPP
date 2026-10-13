# Renovación de sesión Android

Las llamadas protegidas leen la sesión actual antes de enviar credenciales. Un JWT vencido o cercano a vencer se renueva antes de consultar; un 401 también puede renovarse y repetirse una sola vez. El campo `exp` se usa solamente para decidir cuándo renovar, nunca para autorizar una acción.

Un mutex comparte la renovación entre las consultas del catálogo, seguimiento, suscripciones y notificaciones. Los 401 tardíos se comparan con el token realmente enviado: una petición con token anterior no provoca otra rotación innecesaria. Una respuesta antigua no puede restaurar una sesión después de logout o cambio de cuenta.

Si el refresh es inválido, venció, fue revocado, o la cuenta se deshabilitó, se limpia la sesión local, se cierran las burbujas y se muestra «Tu sesión expiró. Inicia sesión de nuevo». Si falla internet o el servidor responde 5xx, se conserva la sesión y puede reintentarse. Un segundo 401 termina el intento sin bucles.

La app sigue usando los endpoints de clientes, incluyendo Google. Los nuevos endpoints exclusivos de administración corresponden solo al web admin.
