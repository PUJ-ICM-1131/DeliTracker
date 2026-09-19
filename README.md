# DeliTracker — Proyecto Android Studio (Kotlin + Jetpack Compose)

Este proyecto convierte el prototipo interactivo `DeliTracker_Prototipo.html`
en una app Android real, usando **Kotlin + Jetpack Compose + Navigation Compose**,
que es el equivalente nativo más directo al patrón "una pantalla = una función
que renderiza contenido" que usaba el prototipo en JavaScript.

## Cómo abrirlo

1. Abre Android Studio (Koala/Ladybug o más reciente).
2. `File > Open...` y selecciona la carpeta `DeliTracker` (la que contiene
   `settings.gradle.kts`).
3. Si Android Studio te avisa que falta el Gradle Wrapper o pide actualizarlo,
   dale a "OK" / "Sync Now" — lo completa automáticamente la primera vez
   (solo incluí `gradle-wrapper.properties`, no el `.jar`, para no subir
   binarios; Android Studio lo genera solo).
4. Espera el Gradle Sync (descarga Compose, Navigation, etc.).
5. Ejecuta en un emulador o dispositivo con `Run ▶` (mínimo Android 7.0 / API 24).

## Cómo está organizado

```
app/src/main/java/com/delitracker/app/
├── MainActivity.kt              // Punto de entrada, monta el tema y el NavHost
├── nav/
│   ├── Routes.kt                 // Un id de ruta por cada pantalla del prototipo
│   └── DeliTrackerNavHost.kt      // Grafo de navegación + Scaffold (topbar/bottombar)
├── state/
│   └── AppState.kt               // ViewModel equivalente al objeto `state` del prototipo
├── components/
│   └── CommonComponents.kt        // TopBar, BottomBar, Chips, Avatar, Row (equivalentes
│                                   // a topbar(), tabbar(), chipRow(), pillRow(), .row)
├── screens/
│   ├── AuthScreens.kt             // Historias #1, #2, #14 (registro, login, onboarding)
│   ├── DiscoveryScreens.kt        // Historias #3, #16, #4 (mapa, buscar, filtros)
│   ├── DetailScreens.kt           // Historias #15, #5, #17, #6 (detalle, ruta, reseña, AR)
│   ├── FavoritesSocialScreens.kt  // Historias #13, #8, #9 (favoritos, compartir, invitar)
│   └── ProfileScreens.kt          // Historias #7, #18, #11/#12, #19 (perfil y cuenta)
└── ui/theme/                      // Colores extraídos 1:1 de las variables CSS
                                    // (--accent, --accent2, --surface-2, etc.), tema claro/oscuro
```

## Cómo se mapea la navegación del prototipo

| Prototipo (JS)      | Proyecto Android (Compose)                                   |
|----------------------|---------------------------------------------------------------|
| `nav(id)`             | `navController.navigate(id)`                                  |
| `goBack()`            | `navController.popBackStack()`                                 |
| `tab(id)`             | `navController.navigate(id) { popUpTo(start) }` (limpia backstack) |
| `state.*` global      | `AppViewModel` (sobrevive a la navegación, igual que `state`)  |
| `topbar(title, opts)` | `DtTopBar` — el estilo (ninguno / back / close) se decide por ruta en `Routes.kt` |
| `tabbar()`            | `DtBottomBar` — solo visible en `mapa`, `favoritos`, `perfil`   |

## Qué falta para producción

Este proyecto cubre la interfaz y navegación de las 19 historias de usuario,
pero (igual que el prototipo) usa datos de ejemplo embebidos en el código:

- No hay backend/API real ni persistencia (Room/DataStore).
- El mapa es una superficie decorativa; falta integrar Google Maps SDK
  (los permisos de ubicación ya están declarados en el `AndroidManifest.xml`).
- La cámara AR es una maqueta visual; falta integrar CameraX + ARCore/ML Kit
  para el reconocimiento real del menú (el permiso de cámara ya está declarado).
- No hay autenticación real (Firebase Auth u otro backend).

Cada pantalla está separada por archivo para que puedas ir conectando estas
piezas una historia de usuario a la vez sin tocar las demás.
