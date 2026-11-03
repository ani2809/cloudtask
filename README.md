# CloudTask

**An enterprise-style task management platform built with Java, Spring Boot, React, PostgreSQL, and cloud-native technologies.**

CloudTask is a full-stack task management application designed to demonstrate modern backend engineering, secure authentication, REST API development, database management, and containerized deployment.

The project is being developed incrementally, with an emphasis on clean architecture, maintainability, security, and production-oriented engineering practices.

## Project Goals

- Build secure and scalable REST APIs using Java and Spring Boot.
- Implement JWT-based authentication and role-based access control.
- Design and manage relational data using PostgreSQL and Flyway.
- Develop a responsive frontend using React and TypeScript.
- Containerize services with Docker.
- Explore Kubernetes-based deployment and service management.
- Establish automated build and CI/CD workflows.

## Technology Stack

### Implemented

| Category | Technology |
|---|---|
| Language | Java 21 |
| Backend Framework | Spring Boot 4 |
| Security | Spring Security, JWT, BCrypt |
| API Architecture | REST APIs |
| Database | PostgreSQL 17 |
| ORM | Spring Data JPA / Hibernate |
| Database Migrations | Flyway |
| Build Tool | Maven |
| Containerization | Docker, Docker Compose |
| Version Control | Git, GitHub |

### Planned

| Category | Technology |
|---|---|
| Frontend | React, TypeScript |
| Messaging | Apache Kafka |
| Orchestration | Kubernetes, Minikube |
| Ingress | Traefik |
| CI/CD | GitHub Actions |
| API Documentation | Swagger / OpenAPI |

## Implemented Features

- User registration with password hashing using BCrypt.
- JWT-based access token authentication.
- Refresh token generation and persistent token storage.
- Secure REST API endpoints using Spring Security.
- Task creation and task listing.
- Task status updates with ownership validation.
- PostgreSQL persistence using Spring Data JPA.
- Version-controlled database migrations using Flyway.
- Docker Compose configuration for PostgreSQL.

## Project Status

## Technology Stack

### Implemented

| Category | Technology |
|---|---|
| Language | Java 21 |
| Backend Framework | Spring Boot 4 |
| Security | Spring Security, JWT, BCrypt |
| API Architecture | REST APIs |
| Database | PostgreSQL 17 |
| ORM | Spring Data JPA / Hibernate |
| Database Migrations | Flyway |
| Build Tool | Maven |
| Containerization | Docker, Docker Compose |
| Version Control | Git, GitHub |

### Planned

| Category | Technology |
|---|---|
| Frontend | React, TypeScript |
| Messaging | Apache Kafka |
| Orchestration | Kubernetes, Minikube |
| Ingress | Traefik |
| CI/CD | GitHub Actions |
| API Documentation | Swagger / OpenAPI |

## Implemented Features

- User registration with password hashing using BCrypt.
- JWT-based access token authentication.
- Refresh token generation and persistent token storage.
- Secure REST API endpoints using Spring Security.
- Task creation and task listing.
- Task status updates with ownership validation.
- PostgreSQL persistence using Spring Data JPA.
- Version-controlled database migrations using Flyway.
- Docker Compose configuration for PostgreSQL.

## Project Status

## Technology Stack

### Implemented

| Category | Technology |
|---|---|
| Language | Java 21 |
| Backend Framework | Spring Boot 4 |
| Security | Spring Security, JWT, BCrypt |
| API Architecture | REST APIs |
| Database | PostgreSQL 17 |
| ORM | Spring Data JPA / Hibernate |
| Database Migrations | Flyway |
| Build Tool | Maven |
| Containerization | Docker, Docker Compose |
| Version Control | Git, GitHub |

### Planned

| Category | Technology |
|---|---|
| Frontend | React, TypeScript |
| Messaging | Apache Kafka |
| Orchestration | Kubernetes, Minikube |
| Ingress | Traefik |
| CI/CD | GitHub Actions |
| API Documentation | Swagger / OpenAPI |

## Implemented Features

- User registration with password hashing using BCrypt.
- JWT-based access token authentication.
- Refresh token generation and persistent token storage.
- Secure REST API endpoints using Spring Security.
- Task creation and task listing.
- Task status updates with ownership validation.
- PostgreSQL persistence using Spring Data JPA.
- Version-controlled database migrations using Flyway.
- Docker Compose configuration for PostgreSQL.



## Project Status

CloudTask is under active development. The backend foundation, authentication, and initial task management APIs are implemented. Frontend development, messaging, Kubernetes deployment, and CI/CD are planned next.


## Getting Started

## API Endpoints

All API endpoints use the base URL:

`http://localhost:8080`

### Authentication

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/api/auth/register` | Register a new user | Public |
| POST | `/api/auth/login` | Authenticate and obtain JWT tokens | Public |

### Task Management

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/api/tasks` | Create a task | Authenticated |
| GET | `/api/tasks` | Retrieve tasks created by the authenticated user | Authenticated |
| PATCH | `/api/tasks/{taskId}/status` | Update a task's status | Authenticated |

### Health Check

| Method | Endpoint | Description | Access |
|---|---|---|---|
| GET | `/actuator/health` | Check application health | Public |

### Supported Task Statuses

- `TODO`
- `IN_PROGRESS`
- `DONE`

### Authentication Details

CloudTask currently uses:

- JWT access tokens for API authentication.
- BCrypt password hashing.
- Stateless Spring Security configuration.
- Persistent refresh-token storage.

Protected endpoints require a valid JWT access token in the HTTP Authorization header:

`Authorization: Bearer <access-token>`

### Prerequisites

Make sure you have the following installed:

- Java 21
- Docker Desktop
- Git
- Maven (or use the included Maven Wrapper)

### 1. Clone the repository

```bash
git clone https://github.com/ani2809/cloudtask.git
cd cloudtask
```

### 2. Configure environment variables

Create a `.env` file in the project root:

```dotenv
POSTGRES_PASSWORD=your_local_database_password
DB_PASSWORD=your_local_database_password
```

Do not commit `.env` or share its contents.

### 3. Start PostgreSQL

```bash
docker compose up -d
```

Verify the container is running:

```bash
docker ps
```

### 4. Configure the JWT secret

Generate a local development secret:

```bash
export CLOUDTASK_JWT_SECRET="$(openssl rand -base64 32)"
```

### 5. Start the backend

```bash
cd backend/cloudtask-api
export DB_PASSWORD=your_local_database_password
./mvnw spring-boot:run
```

The backend will start at:

`http://localhost:8080`

Flyway automatically applies database migrations during application startup.

### 6. Verify the application

Open the following endpoint:

`http://localhost:8080/actuator/health`

A successful response should look like:

```json
{
  "status": "UP"
}
```