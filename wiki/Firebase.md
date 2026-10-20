# Firebase Authentication

## Estado de la conexión

La aplicación usa el SDK de Firebase Authentication para registrar cuentas, iniciar y cerrar sesión,
restaurar la sesión, editar el nombre, verificar cambios de correo, recuperar contraseñas y eliminar cuentas.
Ya no usa la dirección del computador ni el servidor Python para autenticar usuarios.

El proyecto real configurado es `delitracker-bd700` y la aplicación registrada es `com.delitracker.app`.
El archivo real está en `app/google-services.json` y el acceso con correo electrónico/contraseña está habilitado.
La compilación normal se conecta a Firebase por Internet y no necesita el servidor local.

## Conectar el proyecto real una sola vez

1. Crear o seleccionar un proyecto del equipo en [Firebase Console](https://console.firebase.google.com/).
2. Registrar una aplicación Android con el identificador `com.delitracker.app`.
3. Descargar su `google-services.json` y guardarlo en `app/google-services.json`.
4. En Authentication → Sign-in method, habilitar **Correo electrónico/contraseña**.
5. Compilar con `gradlew.bat :app:assembleDebug` en Windows o `./gradlew :app:assembleDebug` en macOS/Linux.
6. Instalar el APK de `app/build/outputs/apk/debug/app-debug.apk` en los celulares compatibles.

Una vez configurado, los celulares se conectan a Firebase por Internet y usan las mismas cuentas.
No hace falta encender un computador, abrir puertos, configurar una IP local ni iniciar el servidor Python.
El mínimo del proyecto es Android 7.0 (API 24). El correo debe permitir recibir los mensajes de recuperación/verificación.
Las cuentas anteriores de SQLite no se migran automáticamente: se deben crear en Firebase.

No utilizar una clave de cuenta de servicio ni Firebase Admin dentro de Android. El archivo requerido es
la configuración del cliente Android descargada desde Firebase. Se incluye en el repositorio para que el equipo compile contra el mismo proyecto Firebase; no es una clave de administración.
Antes de distribuir una versión release, la compilación exige el archivo real y rechaza el modo local.

## Iniciar Firebase local en cualquier computador

Requisito: Node.js 22 LTS y conexión a Internet para instalar las herramientas la primera vez.

- Windows: abrir `iniciar-firebase.cmd` desde la carpeta del proyecto.
- macOS/Linux: ejecutar `sh iniciar-firebase.sh` desde la carpeta del proyecto.
- Alternativa común: `npm ci` y luego `npm run firebase:local`.

Esto inicia el emulador oficial de Firebase Authentication en el puerto 9099 para el proyecto de pruebas
`demo-delitracker`. No necesita una sesión de Google, no crea recursos en la nube ni envía correos reales.
Los enlaces de recuperación/verificación se muestran en la salida del emulador.
Usar solo datos ficticios. El emulador está destinado a desarrollo dentro de una red de confianza.
Al detenerlo se pierden las cuentas de prueba; no son las cuentas del proyecto de producción.

Para detenerlo, presionar Ctrl+C en la ventana donde se inició.

### Probar desde el emulador Android

Mantener Firebase local iniciado y compilar:

```
gradlew.bat :app:assembleDebug -PFIREBASE_AUTH_EMULATOR=true
```

En macOS/Linux sustituir `gradlew.bat` por `./gradlew`.
El emulador Android usa `10.0.2.2:9099` para llegar al computador anfitrión.
Esta variante tiene el identificador `com.delitracker.app.local` y muestra “Firebase local: cuentas de prueba”.
Puede coexistir con la aplicación conectada a la nube y no utiliza su configuración ni sus cuentas.

### Probar desde un celular físico

Por USB, ejecutar `adb reverse tcp:9099 tcp:9099` y compilar con:

```
gradlew.bat :app:assembleDebug -PFIREBASE_AUTH_EMULATOR=true -PFIREBASE_EMULATOR_HOST=127.0.0.1
```

Por Wi-Fi, conectar ambos dispositivos a la misma red y sustituir el host por la IP del computador:

```
gradlew.bat :app:assembleDebug -PFIREBASE_AUTH_EMULATOR=true -PFIREBASE_EMULATOR_HOST=192.168.1.50
```

La IP del ejemplo debe sustituirse por la real. La red debe permitir el puerto 9099.
La variante local depende de ese computador mientras se prueba; la variante con Firebase real no.

## Comportamiento de la cuenta

- Firebase administra los tokens y su renovación; la aplicación no guarda contraseñas.
- Al abrir la aplicación, se recarga el usuario y se valida un token con el servidor.
- Cerrar sesión retira la sesión de ese dispositivo.
- Cambiar el correo envía primero una verificación al correo nuevo. Hasta confirmarla, se conserva el anterior.
- Eliminar una cuenta o cambiar datos sensibles puede exigir iniciar sesión nuevamente.
- La foto de perfil sigue almacenada en cada celular, tal como implementó el equipo; no se sincroniza a Firebase Storage.
- Si el registro crea la cuenta pero falla al guardar el nombre, se informa cómo completarlo al iniciar sesión.

## Prueba de integración Android

Con Firebase local iniciado y un emulador Android conectado:

```
gradlew.bat :app:connectedDebugAndroidTest -PFIREBASE_AUTH_EMULATOR=true
```

La prueba utiliza el repositorio real de la aplicación y el SDK de Firebase para verificar registro,
rechazo de duplicados, acceso incorrecto/correcto, restauración, edición del nombre, recuperación y eliminación.
Se bloquea si la compilación no utiliza explícitamente el proyecto de pruebas local.

## Referencias

- [Configurar Firebase en Android](https://firebase.google.com/docs/android/setup)
- [Autenticación en Android](https://firebase.google.com/docs/auth/android/start)
- [Administrar usuarios y verificar cambios de correo](https://firebase.google.com/docs/auth/android/manage-users)
- [Emulador de autenticación](https://firebase.google.com/docs/emulator-suite/connect_auth)

## Validación local realizada

- Aplicación y paquete de pruebas compilados correctamente.
- Prueba de integración Android ejecutada contra Firebase local: 1 prueba, 0 fallos y 0 errores.
- Verificados registro, duplicados, contraseña incorrecta, acceso, restauración, edición del nombre, recuperación y eliminación.
- Verificación de configuración: rechaza generar una entrega release cuando falta el archivo real.
- APK de pruebas: `entrega/DeliTracker-Firebase-local-debug.apk`. Requiere Firebase local iniciado; no es un APK conectado a la nube.
- Informe Android: `entrega/informe-pruebas/index.html`.
- Configuración real integrada para delitracker-bd700. Comprobada la respuesta de Authentication en la nube con credenciales inexistentes rechazadas. El registro y la entrega de correos reales deben probarse desde la aplicación.
