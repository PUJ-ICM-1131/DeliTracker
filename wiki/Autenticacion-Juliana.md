# Autenticación — Juliana

## Alcance
Implementación de autenticación con backend propio de DeliTracker sobre la rama `develop`.
Incluye registro, inicio de sesión, consulta de sesión persistente, edición de nombre/correo,
cierre de sesión y eliminación de cuenta. No modifica mapas, rutas, sensores ni cámara.
Los dos archivos ZIP recibidos son APK compilados; se conservan sin alteraciones en `originales/`.
La implementación está en el código fuente descargado del repositorio asignado.

## Evolución
Antes: validación de una única cuenta guardada localmente mediante DataStore.
Después: múltiples cuentas en SQLite en el backend; Android solicita autenticación por HTTP,
guarda únicamente el token de sesión y verifica el token al reiniciar.
Las cuentas locales anteriores no se migran: hay que registrar una cuenta en el backend.

## Arquitectura y seguridad
- Android: `AuthScreens` → `AppViewModel` → `UserProfileRepository` → API.
- Backend: `Handler` → `AuthStore` → SQLite; operaciones parametrizadas.
- Contraseñas con PBKDF2-HMAC-SHA256, 600.000 iteraciones, sal aleatoria de 16 bytes.
- Tokens aleatorios de 32 bytes; el servidor conserva solamente su hash SHA-256.
- Caducidad de 24 horas; revocación al cerrar sesión y eliminación de todas las sesiones al borrar cuenta.
- Máximo 10 solicitudes de registro/login por IP por minuto.
- Mensajes de error, bloqueo de envíos repetidos y acceso restringido a pantallas privadas.
- Sin autenticación local alternativa cuando falla la conexión; se debe iniciar sesión nuevamente.
- Sin copias automáticas de datos de la aplicación. El token se guarda en almacenamiento privado Android.
- Si el cierre no alcanza el servidor, se borra la sesión del dispositivo y se informa que la remota vencerá automáticamente.

## Ejecutar la demostración
1. Desde la carpeta raíz, ejecutar `iniciar-backend.ps1` con PowerShell. Requiere Python 3.10+; no instala paquetes.
2. Abrir el proyecto en Android Studio e iniciar un emulador Android.
3. Instalar `entrega/DeliTracker-Juliana-auth-debug.apk` o ejecutar la aplicación desde Android Studio.
4. Mantener el backend iniciado durante la demostración. El APK usa `http://10.0.2.2:8080`, dirección del equipo anfitrión desde el emulador.
5. Crear una cuenta de prueba con correo válido y contraseña de 8 a 128 caracteres. No hay credenciales predefinidas.

Para un celular físico conectado por USB: ejecutar `adb reverse tcp:8080 tcp:8080` y compilar con
`gradlew.bat :app:assembleDebug -PAUTH_BASE_URL=http://127.0.0.1:8080`.
Para red local: iniciar el backend con `python backend/server.py --host 0.0.0.0` y compilar con
`-PAUTH_BASE_URL=http://IP_DEL_PC:8080`. El celular y el PC deben estar en la misma red.
La versión debug permite HTTP exclusivamente para la demostración. La versión release exige HTTPS;
configurar `-PAUTH_BASE_URL=https://servidor-real:443` y desplegar detrás de un servidor de producción.
El servidor incluido es para demostración local; no está publicado en Internet.

## API
| Método | Ruta | Función |
|---|---|---|
| POST | `/auth/register` | Nombre, correo, contraseña → usuario y token |
| POST | `/auth/login` | Correo y contraseña → usuario y token |
| GET | `/auth/me` | Consultar usuario con `Authorization: Bearer TOKEN` |
| PATCH | `/auth/me` | Actualizar nombre y correo del usuario autenticado |
| POST | `/auth/logout` | Revocar el token recibido |
| DELETE | `/auth/me` | Eliminar cuenta y todas sus sesiones |
| GET | `/health` | Comprobar disponibilidad |

No se implementa recuperación por correo porque requiere un servicio de envío y verificación.
Se retiró el botón vacío que sugería esa funcionalidad.

## Demostración de Juliana (3–4 minutos)
- 30 s: explicar el cambio de cuenta local a backend propio y mostrar los diagramas SVG.
- 45 s: registrar cuenta y mostrar su perfil; intentar registrar el mismo correo.
- 45 s: cerrar sesión, probar contraseña incorrecta e iniciar con la correcta.
- 30 s: cerrar y abrir la aplicación; mostrar restauración validada por el servidor.
- 30 s: editar perfil y comprobarlo al iniciar sesión de nuevo.
- 30 s: eliminar cuenta; intentar volver a ingresar y comprobar el rechazo.

## Pruebas
`python -m unittest discover -s backend -v`
Pruebas HTTP con base de datos temporal: registro, duplicados, validación, login correcto/incorrecto,
perfil protegido, persistencia, aislamiento entre cuentas, expiración, revocación, eliminación y límite de intentos.

## Documentación en GitHub
Esta carpeta contiene el material de autenticación preparado para la wiki.
Copiar esta página a la wiki del repositorio y cargar los SVG antes de la sustentación.
La publicación en la wiki es independiente de subir el código a una rama del repositorio.

## Referencias técnicas
- https://docs.python.org/3/library/hashlib.html#key-derivation
- https://developer.android.com/studio/run/emulator-networking

## Resultado de validación local
Las 4 pruebas de integración HTTP del backend pasaron. La aplicación Android se compiló en modo debug. No se verificó la interfaz en un dispositivo físico.
