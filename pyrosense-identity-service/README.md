# PyroSense Identity Service

## Responsibility
Authentication, authorization, and user management for the PyroSense platform.

## Bounded Context
Identity & Access Management (IAM)

## Key Features
- User registration and lifecycle management
- Role-based access control (ADMIN, MANAGER, TECHNICIAN, VIEWER)
- OAuth2/OIDC integration with Keycloak
- JWT token validation

## API Endpoints
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/v1/users | Register a new user |
| GET | /api/v1/users/{id} | Get user by ID |
| GET | /api/v1/users/me | Get current user |

## Port
8081

## Dependencies
- PostgreSQL (dedicated schema)
- Keycloak (OIDC provider)
