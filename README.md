# TicketHub Backend

TicketHub is a Spring Boot backend for a ticket management system. The project currently focuses on authentication: JWT login and client registration, with MySQL persistence and Spring Security 6.

## Current status
- Authentication: JWT login and client registration are implemented.
- Roles: `ROLE_CLIENT`, `ROLE_TECH`, `ROLE_ADMIN` (registration assigns `ROLE_CLIENT`).
- Persistence: MySQL via JPA; Flyway is enabled for schema changes.
- Next planned work: Ticket CRUD and role-based endpoints.

## Tech stack
- Spring Boot 3 / Spring Security 6
- Spring Data JPA + MySQL
- JWT (HS512)
- Lombok
- Flyway

## Local configuration
Local-only settings live in `src/main/resources/application-local.properties` (not committed). Ensure your local file includes the correct MySQL credentials and JWT secret.

## Default test user (local profile)
- Email: `client@tickethub.local`
- Password: `password123`
- Roles: `ROLE_CLIENT`

## Quick start
1. Ensure MySQL is running and matches `src/main/resources/application-local.properties`.
2. Start the app.

```bash
./mvnw spring-boot:run
```

## Auth endpoints
### Register (client)
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"nom":"Doe","prenom":"Jane","tel":"0612345678","email":"jane.doe@example.com","password":"Password123","retypePassword":"Password123"}'
```

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"client@tickethub.local","password":"password123"}'
```

The login response includes `accessToken` and `tokenType` (Bearer). Use them in the `Authorization` header to call secured endpoints.

## Notes
- Flyway migration `V1__drop_username_column.sql` removes the legacy `username` column.
- For production, use a strong JWT secret from environment variables.
