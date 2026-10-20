# Mapa, localización y sensores

Pantalla: **Mapa** (`screens/MapaScreen.kt`). Cubre las historias **#3** (mapa con GPS en tiempo real),
**#11** (modo de transporte con acelerómetro) y **#12** (orientación con giroscopio).

## Historia #3 · Localización y mapa
- `location/UserLocation.kt` → `rememberUserLocation(enabled)`: FusedLocationProvider con
  actualizaciones continuas (cada 2 s o 5 m). Se registra al entrar a la pantalla y se
  desregistra al salir.
- El permiso (`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`) se pide en tiempo de ejecución
  al abrir el mapa. Si se niega, aparece un aviso con el botón **Permitir**.
- `map/OsmMap.kt`: OpenStreetMap con osmdroid (sin API key). Un marcador por restaurante
  (`data/Restaurant.kt` → `RestaurantCatalog`) y un marcador con la posición del usuario.
- Botón para centrar en la ubicación y tarjetas inferiores con nombre, cocina y distancia.

## Historia #11 · Modo de transporte (ACELERÓMETRO)
- `sensors/TransportClassifier.kt` (Kotlin puro, sin Android) clasifica cada ventana de 4 s:
  - **Detenido:** desviación estándar de la aceleración < 0.12 m/s².
  - **Caminando:** desviación ≥ 0.5 m/s² **y** un pico periódico de pasos (autocorrelación entre 0.35 y 1.5 s ≥ 0.45).
  - **En vehículo:** cualquier otro movimiento (vibración sin ritmo de pasos).
  - El modo solo cambia si 2 ventanas seguidas coinciden (≈ 8 s) para no parpadear.
- `sensors/DeviceSensors.kt` → `rememberTransportMode()`: lee el acelerómetro a ~16 Hz (`SENSOR_DELAY_UI`,
  bajo consumo) y lo libera al salir de la pantalla.
- `data/Recommendations.kt` → `recommend(...)`: **caminando** = los más cercanos (≤ 1.5 km);
  **en vehículo** = primero los que tienen parqueadero; **detenido** = por distancia.
- El usuario puede **corregir el modo a mano** (botones Detenido / Caminando / En vehículo) y volver a
  **Automático**.
- Los umbrales son heurísticos: hay que afinarlos con la prueba de campo de la historia (caminar y
  viajar en vehículo comparando con el estado real).

## Historia #12 · Orientación (GIROSCOPIO)
- `sensors/DeviceSensors.kt` → `rememberOrientation()`: usa el sensor de **vector de rotación** de Android,
  que el sistema calcula fusionando giroscopio + acelerómetro + magnetómetro. Si el dispositivo no tiene
  giroscopio, usa acelerómetro + magnetómetro con filtro paso-bajo.
- Se combina con el **GPS**: el rumbo hacia cada restaurante (`bearingDegrees`) se compara con hacia
  dónde apunta el celular (corregido a norte verdadero con la declinación magnética del lugar).
- La flecha apunta al restaurante elegido y los que están dentro de ±30° del frente se **resaltan**
  ("Al frente") y se listan en la tarjeta.
- **Calibración:** si el sistema reporta lectura inestable aparece el aviso "mueve el celular dibujando un 8".
- El giroscopio también funciona con el celular de pie (cámara al frente), usando otro eje.
- Pendiente: el overlay de realidad aumentada (pantalla AR, historia #6) todavía no usa esta orientación.

## Cómo probar en el emulador
1. **Ubicación:** `...` (Extended controls) → *Location* → `4.6763, -74.0483` → *Set location*.
2. **Orientación:** Extended controls → *Virtual sensors* → *Device pose* (rotar el celular mueve la flecha).
3. **Acelerómetro:** *Virtual sensors* → *Additional sensors* (accelerometer) para simular movimiento,
   o usar un celular real y caminar / viajar en vehículo.
4. El botón **Sugerir** pasa al siguiente restaurante recomendado.

## Notas
- Las coordenadas y el dato "tiene parqueadero" son de ejemplo; vendrán de la API (Foursquare Places).
- Los textos están en `res/values/strings_mapa.xml`.
