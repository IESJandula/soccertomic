# SeJuega! — Documentación Estructural

> Cortita y al pie, organiza tu partido en segundos

Aplicación social para organizar partidos de fútbol amateur, balancear equipos, invitar amigos y registrar votaciones post-partido.

## Propósito

- Reducir fricción para crear y gestionar partidos.
- Permitir colaboración de organizadores (OWNER y CO_ORGANIZER).
- Mantener trazabilidad de invitaciones, amistades y votaciones.
- Soportar perfil futbolístico de jugador para balanceo más justo.

## Arquitectura global (explicada)

El proyecto está dividido en dos módulos:

1. `Front` (Vue 3 + Pinia + Router): UI, navegación y estado cliente.
2. `Back` (Spring Boot): API core social (`/api/usuarios`, `/api/partidos`, `/api/amistades`, `/api/invitaciones`, `/api/player-profile`, `/api/partidos/{id}/votaciones`).

Flujo general:

- Usuario interactúa en `Front`.
- `Front` llama a `Back` mediante `apiService` y endpoints declarados en `Front/src/config.js`.
- `Back` procesa reglas de negocio en servicios y persiste con JPA.
- Para operaciones de balance de posiciones, `Back` delega en `TeamBalancingService`.

## Stack tecnológico

### Front
- Vue `3.5.x`
- Vue Router `5.x`
- Pinia `3.x`
- Vite `7.x`

### Back
- Java 21
- Spring Boot `4.0.2`
- Spring Web MVC
- Spring Data JPA
- Bean Validation
- Lombok

### Base de datos
- Core (`Back`): MySQL/MariaDB/PostgreSQL driver disponible (usa URL por variable)

## Puertos y URLs

- Front (dev): `http://localhost:5173`
- Back (core): `http://localhost:8080`

## Variables de entorno necesarias

### Back (Core)
- `CORE_DB_URL` (requerida)
- `CORE_DB_USER` (requerida)
- `CORE_DB_PASSWORD` (requerida)
- `CORS_ALLOWED_ORIGINS` (requerida)
- `FIREBASE_PROJECT_ID`
- `FIREBASE_CREDENTIALS_PATH`

Producción (perfil `prod`):
- `FIREBASE_PROJECT_ID`
- `FIREBASE_CREDENTIALS_PATH`

### Front
- `VITE_CORE_API_URL` (ej. `http://localhost:8080`)
- `VITE_FIREBASE_API_KEY`
- `VITE_FIREBASE_AUTH_DOMAIN`
- `VITE_FIREBASE_PROJECT_ID`
- `VITE_FIREBASE_APP_ID`

## Cómo levantar el backend Core

Desde `Back/`:

```bash
./mvnw spring-boot:run
```

En Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

## Cómo levantar el frontend

Desde `Front/`:

```bash
pnpm install
pnpm dev
```

## Tests rápidos

Backend:

```powershell
cd Back
.\mvnw.cmd test
```

Frontend:

```powershell
cd Front
pnpm.cmd test
```

## Flujo de autenticación (estado actual)

- Se eliminó el modo local de autenticación sin token.
- En `Front`, el login usa Firebase Auth con Google Sign-In y envía `Authorization: Bearer <idToken>`.
- En `Back`, un interceptor valida el ID token con Firebase Admin para cada `/api/**`.
- `FirebaseRequestContext.requireUid()` exige contexto autenticado y responde `401` si falta o el token es inválido.

## Estructura general de módulos

- `Back/README.md`: detalle del módulo core.
- `docs/DIRECTORIOS_RELEVANTES.md`: guía por directorio.
- `docs/ARCHIVOS_CRITICOS.md`: guía archivo a archivo.

## Convenciones de desarrollo

- Back usa patrón Controller → Service → Repository.
- DTOs para contrato API; evitar exponer entidades en respuestas nuevas.
- Front concentra llamadas HTTP en `services/`.
- Estado global en `stores/`; lógica de vista en componentes.
- Cambios de reglas de negocio deben ir en servicios backend.

## Si vas a contribuir

1. Lee primero `docs/GUIA_JUNIOR.md`.
2. Revisa `docs/DOMINIO_Y_REGLAS.md` antes de tocar reglas de partido.
3. Para cambios de flujo, valida `docs/FLUJO_E2E_CREAR_PARTIDO.md`.
4. Mantén la documentación sincronizada si cambias contratos o reglas.

## Índice de documentación

- `docs/01_AUDITORIA_PREVIA.md`
- `Back/README.md`
- `docs/DIRECTORIOS_RELEVANTES.md`
- `docs/ARCHIVOS_CRITICOS.md`
- `docs/MATRIZ_TRAZABILIDAD.md`
- `docs/DOMINIO_Y_REGLAS.md`
- `docs/FLUJO_E2E_CREAR_PARTIDO.md`
- `docs/GUIA_JUNIOR.md`
