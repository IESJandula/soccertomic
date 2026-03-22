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

