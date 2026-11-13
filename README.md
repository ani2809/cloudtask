# CloudTask – Full-Stack Task Management Application

CloudTask is a full-stack task management application built with **Java, Spring Boot, React, and PostgreSQL**. It provides secure user authentication, role-based access control, and task management features through a responsive web interface.

The project demonstrates end-to-end software development, REST API design, application security, containerization, and Kubernetes deployment.

## Features

### Authentication and Security
- User registration and login.
- JWT-based authentication.
- Access token and refresh token management.
- Refresh token revocation on logout.
- Role-based access control (ADMIN, MANAGER, USER).
- Secure API access with appropriate HTTP 401 and 403 responses.

### Task Management
- Create and view tasks.
- Update task status and priority.
- Filter tasks by status and priority.
- Filter tasks by assignee.
- Support task priorities: HIGH, MEDIUM, LOW.
- Support task statuses: TODO, IN_PROGRESS, DONE.
- Task pagination and sorting.

### Frontend
- React-based user interface.
- Login and registration screens.
- Task dashboard.
- Task creation and management.
- Status and priority filters.
- JWT token handling and automatic token refresh.

### Backend
- RESTful APIs using Spring Boot.
- Layered architecture with controllers, services, repositories, and DTOs.
- PostgreSQL persistence using Spring Data JPA.
- Database migrations using Flyway.
- Centralized exception handling.
- Health monitoring using Spring Boot Actuator.

## Technology Stack

| Area | Technologies |
|---|---|
| Backend | Java 21, Spring Boot |
| Frontend | React, JavaScript, Vite |
| Database | PostgreSQL 17 |
| Security | Spring Security, JWT |
| Persistence | Spring Data JPA, Hibernate |
| Database migrations | Flyway |
| Testing | JUnit, Mockito, Spring Boot Test |
| API testing | Postman |
| Containers | Docker |
| Orchestration | Kubernetes, Minikube |
| Traffic routing | Kubernetes Services, Ingress |
| Build tools | Maven, npm |
| Version control | Git, GitHub |

## Architecture

```text
                 Browser
                    |
                    v
             CloudTask Frontend
                React / Vite
                    |
                    v
             Kubernetes Ingress
                    |
                    v
             Frontend Service
                    |
                    v
              Backend Service
                    |
             ----------------
             |              |
             v              v
       Backend Pod 1   Backend Pod 2
        Spring Boot     Spring Boot
             |              |
             ----------------
                    |
                    v
             PostgreSQL
```

The application is deployed locally on Kubernetes using Minikube, with two backend replicas and two frontend replicas.

## API Overview

Base URL: `/api`

| Method | Endpoint | Description |
|---|---|---|
| POST | `/auth/register` | Register a user |
| POST | `/auth/login` | Authenticate a user |
| POST | `/auth/refresh` | Refresh access token |
| POST | `/auth/logout` | Log out and revoke refresh token |
| POST | `/tasks` | Create a task |
| GET | `/tasks` | Retrieve tasks |
| PATCH | `/tasks/{taskId}/status` | Update task status |
| PATCH | `/tasks/{taskId}/priority` | Update task priority |
| GET | `/admin/test` | Test ADMIN authorization |
| GET | `/manager/test` | Test MANAGER authorization |

Task listing supports filtering and pagination.

## Running Locally

### Prerequisites

- Java 21
- Node.js and npm
- PostgreSQL 17
- Maven (or use the included Maven wrapper)

### Backend

Navigate to the backend directory:

```bash
cd backend/cloudtask-api
```

Configure the required environment variables:

```text
DB_PASSWORD=your_database_password
CLOUDTASK_JWT_SECRET=your_jwt_secret
```

Make sure PostgreSQL is running and the `cloudtask` database and user are configured.

Start the Spring Boot application:

```bash
./mvnw spring-boot:run
```

Backend API runs on:

```text
http://localhost:8080
```

### Frontend

Open another terminal:

```bash
cd frontend
npm install
npm run dev
```

Open the local Vite URL shown in the terminal.

## Docker and Kubernetes Deployment

CloudTask is containerized using Docker and deployed to a local Kubernetes cluster using Minikube.

Deployment resources are maintained under:

```text
k8s/
└── base/
    ├── backend.yaml
    ├── frontend.yaml
    └── ...
```

The Kubernetes deployment includes:

- Backend Deployment with two replicas.
- Frontend Deployment with two replicas.
- PostgreSQL deployment.
- Kubernetes Services for internal communication.
- ConfigMaps and Secrets for configuration.
- Readiness and liveness probes.
- Ingress for application access.

The application is accessible locally through:

```text
http://cloudtask.local
```

### Verify Kubernetes Deployment

```bash
kubectl get pods -n cloudtask
kubectl get services -n cloudtask
kubectl get ingress -n cloudtask
```

## Testing

Backend tests cover areas including:

- Authentication and token handling.
- Role-based authorization.
- Task creation and retrieval.
- Task filtering.
- Task status and priority updates.
- Exception handling.
- Unauthorized and forbidden API requests.

API workflows can also be tested using the Postman collection:

```text
postman/CloudTaskAPI's.postman_collection.json
```

## Project Goals

CloudTask was developed as a hands-on full-stack engineering portfolio project to demonstrate:

- Backend engineering with Java and Spring Boot.
- Frontend development with React.
- REST API development and integration.
- Authentication and application security.
- Relational database integration.
- Automated testing.
- Docker and Kubernetes deployment.
- Git-based version control and delivery.

## Future Enhancements

Potential improvements include:

- Automated CI/CD pipeline.
- Cloud deployment on AWS.
- Enhanced application monitoring and observability.
- Additional dashboard analytics.
- Automated end-to-end frontend tests.

---

**CloudTask** – Built with Java, Spring Boot, React, PostgreSQL, Docker, and Kubernetes.
