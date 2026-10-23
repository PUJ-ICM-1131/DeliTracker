# Lugares y rutas con Geoapify · Reseñas y fotos en Firebase

## Qué usa cada pantalla
| Dato | Fuente |
|---|---|
| Restaurantes cercanos (mapa) | Geoapify **Places API** (OpenStreetMap), categoría `catering.restaurant`, radio de 2 km |
| Nombre, cocina, dirección, teléfono, sitio web, horario | Geoapify (solo se muestran los que OpenStreetMap tenga) |
| Ruta y tiempo (pantalla Ruta) | Geoapify **Routing API** (`walk` / `drive`); si falla o no hay clave, un servidor OSRM público |
| Calificación, comentarios y fotos | **Reseñas de los usuarios de la app**, en Cloud Firestore |

OpenStreetMap no trae calificaciones ni fotos de restaurantes; por eso esas dos cosas las aportan las reseñas propias.

## Configurar la clave de Geoapify (cada integrante, una vez)
1. Crear una cuenta gratis en [myprojects.geoapify.com](https://myprojects.geoapify.com) (no pide tarjeta) y un proyecto con su **API key**.
2. En la raíz del proyecto, en `local.properties` (Git lo ignora), añadir:
   ```
   GEOAPIFY_API_KEY=la_clave
   ```
3. Sync y volver a ejecutar. Cambiar la clave exige recompilar.

Nunca subir la clave a Git ni pegarla en el chat. Una clave dentro del APK puede extraerse; para una versión pública habría que
llamar a Geoapify desde un backend propio. En el panel de Geoapify se puede restringir la clave.

Plan gratuito: 3.000 créditos por día y 5 solicitudes por segundo. La app hace una búsqueda de lugares por cada ~550 m que se
mueve el usuario, guarda el resultado 10 minutos en memoria y pide una ruta por cada cambio de destino o de modo de viaje.
**Atribución obligatoria:** la pantalla del mapa muestra "Geoapify · © colaboradores de OpenStreetMap".

## Reseñas en Firestore
- Documento `restaurants/{idDelRestaurante}/reviews/{uidDelAutor}`: `uid`, `userName`, `stars` (1-5), `text`, `photo`
  (opcional), `createdAtMs`. Hay **una reseña por persona y restaurante**: volver a publicar la reemplaza.
- La foto **no** usa Firebase Storage (desde 2024 exige el plan de pago). Se reduce a 800 px, JPEG calidad 70, y se guarda como
  texto Base64 dentro del documento (máx. 400.000 caracteres, ~300 KB).
- Detalle muestra el promedio, las 30 reseñas más recientes y usa como foto principal la de la reseña más reciente que tenga una.
- Reglas en `firestore.rules`: leen todas las personas con sesión; cada una escribe solo su reseña, con 1-5 estrellas, texto de
  hasta 1000 caracteres y foto de hasta 400.000 caracteres. **Hay que volver a pegar las reglas en Firebase Console > Firestore > Reglas > Publicar.**
- No funciona con el emulador local de Firebase (solo se inicia el de Authentication); la app lo avisa.

## Límites conocidos
- Si el restaurante no tiene reseñas, Detalle no muestra calificación ni foto: no se inventan.
- Los restaurantes de OpenStreetMap pueden no tener teléfono, sitio ni horario, y el nombre puede faltar en algunos lugares (se omiten).
- Los identificadores de lugar de Geoapify son estables, así que las reseñas siguen asociadas al mismo restaurante entre sesiones.
- El modo "Bus" de Ruta se calcula como auto: Geoapify no ofrece transporte público en su plan gratuito.
