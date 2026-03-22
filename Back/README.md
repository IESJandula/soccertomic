# Módulo Backend Core (`Back`)

Producto: `SeJuega!`

Eslogan: `Cortita y al pie, organiza tu partido en segundos`

## Responsabilidad del módulo

Gestiona el dominio social del producto:

- usuarios,
- perfil de jugador,
- partidos,
- organización del partido,
- amistades,
- invitaciones,
- votaciones post-partido,
- análisis y balance de equipos.

Base URL: `/api` en puerto `8080`.

## Entidades principales

- `UsuarioEntity`
- `PlayerProfileEntity`
- `PartidoEntity`
- `PartidoOrganizadorEntity`
- `AmistadEntity`
- `InvitacionEntity`
- `ChatPartidoEntity`
- `PartidoVotacionEntity`

## Casos de uso que gestiona

1. Crear/actualizar usuario actual (`/api/usuarios/me`).
2. Guardar perfil futbolístico (`/api/player-profile/me`).
3. Crear y administrar partido (`/api/partidos`).
4. Inscribir/desinscribir jugadores y asignarlos a equipos.
5. Transferir ownership y gestionar co-organizadores.
6. Balancear equipos por niveles/perfiles.
7. Gestionar amistades e invitaciones.
8. Registrar votación de resultado al finalizar partido.

## Dependencias internas

- `controller/` → `service/`.
- `service/` → `repository/`, otros servicios (`PlayerProfileService`, `TeamBalancingService`).
- `repository/` → JPA entities.
- `security/` y `config/` aplican contexto de request y CORS.

## Dependencias externas

- Spring Boot Web MVC
- Spring Data JPA
- Jakarta Validation
- Lombok
- JDBC drivers (MySQL/MariaDB/PostgreSQL)

## Flujo típico de petición

Ejemplo (crear partido):

1. `PartidoController.crearPartido()` obtiene UID con `FirebaseRequestContext.requireUid()`.
2. Resuelve usuario en `UserService`.
3. Crea entidad base y delega en `PartidoService.crearPartido()`.
4. `PartidoService` persiste partido + relación OWNER en `PartidoOrganizadorEntity`.
5. Controller retorna `PartidoResponseDTO`.

## Riesgos / puntos sensibles

- Uso de `RuntimeException` en varias rutas críticas.
- `PartidoEntity` usa varias colecciones `EAGER` (impacto rendimiento y carga).
- Reglas de estado de partido mezclan valores legacy y nuevos en `EstadoPartido`.
- `PlayerProfileService.calculateGlobalRating()` aplica modificador de tier aditivo (puede elevar rating por encima de escala base 0–5).

## Endpoints relevantes (Core)

### Usuarios
- `PUT /api/usuarios/me`
- `PUT /api/usuarios/me/rasgos`
- `GET /api/usuarios/me`
- `GET /api/usuarios/{id}`
- `GET /api/usuarios`

### Player Profile
- `PUT /api/player-profile/me`
- `GET /api/player-profile/me`

### Partidos
- `POST /api/partidos`
- `GET /api/partidos/{id}`
- `GET /api/partidos/mis-partidos`
- `GET /api/partidos/mi-historial`
- `GET /api/partidos/futuros`
- `GET /api/partidos/{id}/detalle`
- `POST /api/partidos/{id}/inscribirse`
- `POST /api/partidos/{id}/desinscribirse`
- `PUT /api/partidos/{id}/jugadores/{usuarioId}/equipo?equipo=A|B|INSCRITOS`
- `POST /api/partidos/{id}/jugadores/{usuarioId}/sin-equipo`
- `POST /api/partidos/{id}/balancear-equipos`
- `GET /api/partidos/{id}/analisis-balance`
- `POST /api/partidos/{id}/reservar-pista`
- `GET /api/partidos/{id}/reservas/estado-pago`
- `POST /api/partidos/{id}/transferir-creador`
- `POST /api/partidos/{id}/organizadores`
- `DELETE /api/partidos/{id}/organizadores/{usuarioId}`
- `POST /api/partidos/{id}/organizadores/salir`

### Votaciones
- `POST /api/partidos/{partidoId}/votaciones/me`
- `GET /api/partidos/{partidoId}/votaciones/me`
- `GET /api/partidos/{partidoId}/votaciones/asignacion`
- `GET /api/partidos/{partidoId}/votaciones/panel-compartido`

### Amistades
- `POST /api/amistades`
- `GET /api/amistades/mis-amigos`
- `GET /api/amistades/solicitudes-pendientes`
- `GET /api/amistades/solicitudes-enviadas`
- `PUT /api/amistades/{id}/aceptar`
- `PUT /api/amistades/{id}/rechazar`
- `DELETE /api/amistades/{id}`

### Invitaciones
- `POST /api/invitaciones`
- `GET /api/invitaciones/mis-invitaciones`
- `GET /api/invitaciones/pendientes`
- `PUT /api/invitaciones/{id}/aceptar`
- `PUT /api/invitaciones/{id}/rechazar`
- `DELETE /api/invitaciones/{id}`
- `PUT /api/invitaciones/{id}/marcar-pagada`

## Arranque módulo

```powershell
cd Back
.\mvnw.cmd spring-boot:run
```

## Firebase seguro (archivo JSON en variables-entorno)

1. Carga las variables desde `variables-entorno/backend.env`
2. Variables requeridas:
	- `FIREBASE_PROJECT_ID`
	- `FIREBASE_CREDENTIALS_PATH` (por defecto: `../variables-entorno/firebase-service-account.json`)
	- `CORE_DB_URL`, `CORE_DB_USER`, `CORE_DB_PASSWORD`
	- `CORS_ALLOWED_ORIGINS`
3. El backend valida token Bearer en todas las rutas `/api/**`.
