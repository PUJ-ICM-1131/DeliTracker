# Compartir ubicación en tiempo real (historias 8 y 9)

## Qué hace
- **Compartir (historia 8):** Perfil > Compartir ubicación. Eliges con qué amigos compartir (interruptor) y
  pulsas **Compartir mi ubicación**. Durante 1 hora tu posición se publica en Cloud Firestore y tus amigos
  la ven como un marcador en su mapa, que se mueve en tiempo real. **Detener** (o cerrar sesión) la corta.
- **Amigos:** se identifican por el **correo de su cuenta de DeliTracker**. Se agregan escribiendo el correo,
  o desde los contactos del teléfono. La lista se guarda solo en el dispositivo (por cuenta).
- **Invitar (historia 9):** Invitar más amigos > *Elegir contacto e invitar por SMS* abre el selector de contactos de
  Android y deja listo un SMS de invitación. *Agregar amigo desde mis contactos* toma el correo del contacto.
  No se pide el permiso `READ_CONTACTS`: el selector del sistema solo entrega el contacto elegido.

## Configuración (una sola vez, por quien administra el proyecto de Firebase)
1. Firebase Console > **Firestore Database** > *Crear base de datos* (modo producción; la región da igual).
2. Pestaña **Reglas**: pegar el contenido de `firestore.rules` (en la raíz del repositorio) y **Publicar**.
3. Nada más: no hace falta otro `google-services.json`; el mismo de Authentication sirve.

El plan gratuito (Spark) alcanza para pruebas de clase.

## Cómo funciona por dentro
Documento `shares/{uid del dueño}`: `ownerName`, `ownerEmail`, `active`, `expiresAtMs`, `updatedAtMs`, `lat`, `lon`
y `allowed` (correos que pueden verlo). Las reglas dejan escribir solo al dueño y leer al dueño y a los correos de `allowed`.
Cada amigo escucha la consulta `allowed array-contains <su correo>`.
La ubicación se lee cada 5 s o 5 m (`LocationSharer`) y se publica como máximo cada 3 s.

## Límites conocidos
- Solo comparte con **la app abierta**: no hay servicio en primer plano, así que si se minimiza, Android reduce o
  detiene las lecturas. Los amigos dejan de ver la ubicación tras 2 minutos sin actualizar.
- No funciona con el emulador local de Firebase (`-PFIREBASE_AUTH_EMULATOR=true`): solo se inicia el de Authentication.
- Los amigos deben tener cuenta con ese mismo correo. Quien te agregó no recibe aviso: ve tu ubicación mientras compartas.
- Cambiar el correo de la cuenta cambia quién ve tu ubicación; los amigos lo agregan con el nuevo.
