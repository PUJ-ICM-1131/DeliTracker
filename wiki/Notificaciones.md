# Notificaciones — Entrega 3

Implementación sobre `develop` (`32e43b3`). No requiere desplegar funciones, modificar reglas de Firestore ni incluir credenciales de servidor.

## Qué se puede demostrar

- **Restaurantes cercanos:** con el mapa abierto y permiso de ubicación, avisa al encontrar un restaurante a un máximo de 200 metros cuya cocina coincida con alguna seleccionada en los filtros. Usa los restaurantes de Geoapify o el catálogo de ejemplo que ya muestra el mapa. Reconoce nombres de cocina en español e inglés. No afirma disponer de promociones ni certificaciones dietarias.
- **Ubicación de amigos:** al recibir una sesión de ubicación compartida desde la escucha existente de Firestore, muestra un aviso del sistema. Descarta sesiones vencidas o cuya posición no se actualizó en los últimos dos minutos.
- **Configuración:** Perfil → Ajustes de cuenta → Notificaciones. Activación independiente por categoría y por cuenta en este dispositivo; ambas categorías empiezan desactivadas. Permiso de Android 13+, estado del permiso, acceso a ajustes del sistema y botón de prueba con resultado explícito.
- **Tocar el aviso:** abre el restaurante correspondiente o el mapa. Espera a que haya sesión y verifica que la cuenta coincida con la destinataria. Los datos del restaurante se conservan en el aviso, también si venían de Geoapify. No repite la navegación al recrear la actividad.
- **Sin saturación:** máximo un aviso de cercanía cada cinco minutos y uno por restaurante por hora; un aviso social por dueño y vencimiento de sesión compartida. Las marcas persisten al reiniciar la app y solo se guardan cuando Android acepta la publicación.
- **Sesión:** cerrar sesión, eliminar la cuenta o detectar una sesión inválida retira los avisos visibles. Las preferencias son locales; no se escriben documentos de notificaciones en Firestore.

## Alcance y límites

Son notificaciones del sistema Android generadas por eventos que recibe la app, **no Firebase Cloud Messaging**. Cercanía se evalúa con el mapa abierto. La escucha de amigos vive durante la sesión del ViewModel y se libera al terminarla; puede recibir actualizaciones con la app temporalmente en segundo plano mientras el proceso permanezca activo, pero Android puede suspenderlo. **No se garantiza recibir eventos nuevos con la app cerrada, forzada a detenerse o con el proceso eliminado.** Los avisos ya publicados permanecen en la bandeja hasta que se toquen, descarten o cierre la sesión.

La recepción con la app cerrada requeriría una implementación adicional de FCM y un emisor de confianza en servidor. No se incluyen claves privadas en la app. No se solicita ubicación en segundo plano ni se agregan servicios permanentes.

Los avisos sociales dependen de que Firebase Authentication, Firestore y las reglas existentes de `shares` estén operativos. El modo local de Authentication del proyecto no inicia Firestore y por eso no permite esta demo social.

## Guion para dos dispositivos

1. Instalar el mismo APK en A y B. Iniciar sesión con dos cuentas diferentes del Firebase del proyecto.
2. En B, abrir Perfil → Ajustes de cuenta → Notificaciones, activar Ubicación de amigos y permitir los avisos de Android. Dejar la sesión abierta.
3. En A, agregar el correo exacto de B a los destinatarios de Compartir ubicación, habilitarlo, conceder ubicación e iniciar la sesión compartida.
4. En B, abrir la bandeja de Android: debe aparecer el aviso con el nombre de A. Tocarlo abre el mapa. Las actualizaciones de coordenadas de esa misma sesión no deben repetir el aviso.
5. Detener y comenzar una nueva sesión en A: un nuevo vencimiento permite un nuevo aviso. Si se revoca el acceso de B, B deja de recibir la ubicación mediante las reglas y la consulta existentes.
6. Para cercanía, activar Restaurantes cercanos en B, seleccionar cocina vegana y situar el emulador en `lat=4.6752, lon=-74.0495` con el catálogo de ejemplo (sin clave Geoapify). Abrir el mapa. El aviso debe identificar Verde Bowl; tocarlo abre su detalle.
7. Repetir la misma posición: no debe duplicarse el aviso. Desactivar la categoría: no se publican nuevos avisos. Bloquear las notificaciones en Android: la pantalla debe reflejarlo y la prueba debe indicar que no pudo enviarse.

## Verificación automatizada


`NotificationsTest` ejecuta casos instrumentados de distancia, preferencias, traducción de cocina, repetición, persistencia, separación de cuentas, sesiones sociales vencidas y datos de navegación. Comprueba avisos reales en `NotificationManager.activeNotifications`. La prueba de evidencia comprueba dos avisos emitidos con datos ficticios. Las capturas automáticas del emulador de 16 KB pueden quedar tapadas por el aviso de compatibilidad de las bibliotecas nativas existentes; no se consideran evidencia visual aprobada ni sustituyen una prueba entre dos cuentas reales de Firebase.

Ejecutar únicamente en un emulador de pruebas: la suite limpia las preferencias locales del módulo de notificaciones y concede su permiso a la app instalada.

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.delitracker.app.notifications.NotificationsTest'
```

Referencias: [crear notificaciones](https://developer.android.com/develop/ui/views/notifications/build-notification), [permiso de Android 13](https://developer.android.com/develop/ui/views/notifications/notification-permission).
