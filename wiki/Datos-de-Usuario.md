# Favoritos, preferencias y borrado de datos (historias 13, 14 y 19)

## Qué se guarda y dónde
Documento de Cloud Firestore `users/{uid}`, que solo lee y escribe su dueño (ver `firestore.rules`):

| Campo | Qué es | Historia |
|---|---|---|
| `favorites` | nombres de los restaurantes favoritos | #13 |
| `cuisines`, `diet`, `price`, `rating` | preferencias del onboarding y de los filtros | #14 |
| `reviewedRestaurants` | ids de los restaurantes donde dejó reseña | #19 |

- **Favoritos:** el corazón del detalle guarda al instante. Una cuenta nueva empieza sin favoritos.
- **Preferencias:** se guardan al pulsar *Continuar* en el onboarding y *Aplicar* en los filtros. Al iniciar sesión se cargan.
- **Cerrar sesión:** favoritos y preferencias vuelven a los valores de una cuenta nueva, para que no se vean en la cuenta siguiente.

## Eliminar la cuenta (historia 19)
Antes de eliminar la cuenta de Authentication se borra, en este orden: las reseñas del usuario (`restaurants/{id}/reviews/{uid}`),
su ubicación compartida (`shares/{uid}`) y su documento `users/{uid}`. Si no hay conexión, se avisa y **no se borra nada**.
Las reseñas hechas antes de esta versión no estaban registradas: se buscan en los restaurantes que la app conoce (los vistos y el catálogo de ejemplo).

## Configuración
Hay que **publicar de nuevo `firestore.rules`** en Firebase Console > Firestore > Reglas.
