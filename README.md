# TicketHub Backend

TicketHub is a Spring Boot REST API for a full-featured IT ticket management system. It handles user authentication, role-based access control, ticket lifecycle management, technician assignment, SLA monitoring, real-time notifications, and admin dashboards.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Data Model](#data-model)
- [API Endpoints](#api-endpoints)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Default Test Users](#default-test-users)
- [API Documentation](#api-documentation)

---

## Features

### Authentication & Authorization
- JWT-based authentication (HS512, configurable expiration)
- Client self-registration with admin approval workflow
- Role-based access control with three roles: `CLIENT`, `TECH`, `ADMIN`
- Secure endpoints protected via Spring Security 6 and `@PreAuthorize`

### Ticket Management
- Create tickets with title, description, priority, and category
- List tickets with filtering by status, priority, and category
- Pagination and sorting support
- Update ticket content (CLIENT / ADMIN)
- Delete tickets (CLIENT / ADMIN)
- Update ticket status with optional resolution note (TECH / ADMIN)
- Assign a technician to a ticket (TECH / ADMIN)
- Full ticket lifecycle: `NEW → OPEN → ACCEPTED → IN_PROGRESS → RESOLVED → CLOSED`

### Admin Features
- Approve pending user registrations
- Create users with any role (CLIENT, TECH, ADMIN)
- List all users or filter by role
- View global dashboard statistics (ticket counts by status, priority, category)
- Assign technicians to tickets
- View technician availability and workload

### Technician Features
- View all tickets across the system
- View personal workload statistics
- Update ticket status and provide resolution notes

### Notifications
- Real-time Server-Sent Events (SSE) push notifications
- Subscribe to a personal notification stream via JWT token

### SLA Monitoring
- Automatic SLA deadline computation per ticket priority
- Background monitoring service tracks approaching/breached SLA deadlines

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 4 / Spring MVC |
| Security | Spring Security 6, JWT (JJWT 0.11.5, HS512) |
| Persistence | Spring Data JPA + Hibernate, MySQL |
| Schema Migrations | Flyway |
| Validation | Jakarta Bean Validation (spring-boot-starter-validation) |
| API Documentation | SpringDoc OpenAPI / Swagger UI |
| Monitoring | Spring Boot Actuator |
| Code Generation | Lombok |
| Build | Maven (mvnw wrapper) |
| Java Version | 21 |

---

## Architecture

The project follows a standard layered architecture:

```
src/main/java/com/tickethub/
├── config/          # Spring beans, OpenAPI config, DataInitializer (seed data)
├── controller/      # REST controllers (entry points)
├── service/         # Business logic and orchestration
│   └── impl/        # Service implementations
├── repository/      # Spring Data JPA repositories
├── model/           # JPA entities and enums
├── dto/
│   ├── request/     # Input DTOs (Java records with validation)
│   └── response/    # Output DTOs (Java records)
├── security/        # JWT filter, token provider, UserDetailsService, SecurityConfig
└── exception/       # Global exception handler and custom exceptions
```

---

## Data Model

### Roles

| Role | Description |
|---|---|
| `ROLE_CLIENT` | End user — opens tickets, views own tickets |
| `ROLE_TECH` | Technician — handles and resolves tickets |
| `ROLE_ADMIN` | Administrator — full system access |

### Ticket Status Lifecycle

```
NEW → OPEN → ACCEPTED → IN_PROGRESS → RESOLVED → CLOSED
```

### Priority Levels

| Priority | Description |
|---|---|
| `LOW` | Non-urgent issues |
| `MEDIUM` | Standard issues (default) |
| `HIGH` | Important issues |
| `CRITICAL` | Critical issues with shortest SLA |

### Ticket Categories

| Category | Description |
|---|---|
| `NETWORK` | Network and connectivity issues |
| `HARDWARE` | Physical hardware problems |
| `SOFTWARE` | Application or OS issues |
| `ACCESS` | Access rights and permissions |

---

## API Endpoints

> All secured endpoints require `Authorization: Bearer <token>` header.

### Authentication — `/api/auth`

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Register as a new client (pending admin approval) |
| `POST` | `/api/auth/login` | Public | Log in and receive a JWT |

### Tickets — `/api/tickets`

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/tickets` | CLIENT, TECH, ADMIN | Create a new ticket |
| `GET` | `/api/tickets` | CLIENT, TECH, ADMIN | List tickets (filtered by role; paginated) |
| `PATCH` | `/api/tickets/{id}/status` | TECH, ADMIN | Update ticket status (+ optional solution) |
| `PATCH` | `/api/tickets/{id}/assign` | TECH, ADMIN | Assign a technician to a ticket |
| `PATCH` | `/api/tickets/{id}` | CLIENT, ADMIN | Update ticket title/description/priority/category |
| `DELETE` | `/api/tickets/{id}` | CLIENT, ADMIN | Delete a ticket |
| `GET` | `/api/tickets/stats` | TECH, ADMIN | Get technician workload statistics |

### Admin — Ticket Management

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `PATCH` | `/api/admin/tickets/{id}/assign` | ADMIN | Assign a technician to a ticket (admin route) |

### Admin — User Management — `/api/admin/users`

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/admin/users` | ADMIN | List all users |
| `GET` | `/api/admin/users/pending` | ADMIN | List users pending approval |
| `POST` | `/api/admin/users/{id}/approve` | ADMIN | Approve a pending user |
| `POST` | `/api/admin/users` | ADMIN | Create a new user with any role |

### Admin — Statistics

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/admin/stats/global` | ADMIN | Get global ticket dashboard statistics |

### Users & Technicians

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/users` | ADMIN | List all users (optionally filter by `?role=`) |
| `GET` | `/api/technicians` | TECH, ADMIN | List all technicians |
| `GET` | `/api/users/technicians` | TECH, ADMIN | Alias for `/api/technicians` |
| `GET` | `/api/admin/technicians` | ADMIN | List all technicians (admin route) |
| `GET` | `/api/technicians/availability` | ADMIN | View technician availability and open-ticket counts |

### Notifications

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/notifications/subscribe?token=<jwt>` | Authenticated | Subscribe to real-time SSE push notifications |

---

## Quick Start

### Prerequisites
- Java 21+
- Maven (or use the included `./mvnw` wrapper)
- MySQL 8+ running locally

### 1. Create the database

```sql
CREATE DATABASE tickethub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Configure local properties

Create `src/main/resources/application-local.properties` (not committed):

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/tickethub?serverTimezone=UTC
spring.datasource.username=your_mysql_user
spring.datasource.password=your_mysql_password
app.jwt.secret=your_strong_secret_here
```

### 3. Run the application

```bash
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`.

---

## Configuration

All configurable properties (with their environment variable overrides):

| Property | Env Variable | Default | Description |
|---|---|---|---|
| `spring.datasource.url` | `DB_URL` | `jdbc:mysql://localhost:3306/tickethub?serverTimezone=UTC` | MySQL connection URL |
| `spring.datasource.username` | `DB_USERNAME` | `root` | MySQL username |
| `spring.datasource.password` | `DB_PASSWORD` | `root` | MySQL password |
| `app.jwt.secret` | `JWT_SECRET` | *(dev default)* | JWT signing secret — **change for production** |
| `app.jwt.expiration-ms` | `JWT_EXPIRATION_MS` | `86400000` (24 h) | JWT token validity in milliseconds |

> **Production note:** Always override `JWT_SECRET` and database credentials using environment variables or a secrets manager. Never rely on the defaults.

---

## Default Test Users (local profile)

The `DataInitializer` bean seeds the following accounts when the `local` profile is active:

| Email | Password | Role |
|---|---|---|
| `client@tickethub.local` | `password123` | `ROLE_CLIENT` |
| `tech@tickethub.local` | `password123` | `ROLE_TECH` |
| `admin@tickethub.local` | `password123` | `ROLE_ADMIN` |

### Example: Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@tickethub.local","password":"password123"}'
```

Response:
```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer"
}
```

Use the token in subsequent requests:
```bash
curl http://localhost:8080/api/tickets \
  -H "Authorization: Bearer <jwt>"
```

### Example: Register a new client

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Doe",
    "prenom": "Jane",
    "tel": "0612345678",
    "email": "jane.doe@example.com",
    "password": "Password123",
    "retypePassword": "Password123"
  }'
```

The account is created in a **pending** state and must be approved by an admin via `POST /api/admin/users/{id}/approve`.

---

## API Documentation

When the application is running, interactive Swagger UI is available at:

```
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON spec:

```
http://localhost:8080/v3/api-docs
```
