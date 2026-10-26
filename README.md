# 🔐 Keycloak OAuth2 / OIDC with Spring Boot

A Spring Boot 3 demo of single sign-on with **Keycloak** using **OAuth2 / OpenID Connect**.
Users log in through Keycloak, and their Keycloak client roles control which pages they can
open: **user**, **manager** or **admin**.

## Features

- 🔑 Login with Keycloak using the OAuth2 authorization code flow
- 🧑‍💼 Role-based access control mapped from Keycloak **client roles** (`resource_access`)
- 🚪 OIDC logout that also ends the Keycloak session
- 🖥️ Thymeleaf pages for each role

| Page | Who can open it |
|---|---|
| `/login` | Everyone |
| `/home` | Any logged-in user |
| `/user` | Users with the `USER` role |
| `/manager` | Users with the `MANAGER` role |
| `/admin` | Users with the `ADMIN` role |

## Tech stack

- Java 21
- Spring Boot 3.5 (Web, Security, OAuth2 Client)
- Thymeleaf
- Keycloak

## How it works

```text
Browser ──▶ Spring Boot app ──▶ redirect to Keycloak login
                                         │
            ◀── authorization code ──────┘
Spring Boot exchanges the code for tokens, then CustomOidcUserService
reads resource_access.<client>.roles and maps them to ROLE_USER / ROLE_MANAGER / ROLE_ADMIN.
SecurityConfig allows or blocks each page based on those roles.
```

## Getting started

### 1. Start Keycloak on port 8090
```bash
docker run -p 8090:8080 -e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak start-dev
```

### 2. Configure Keycloak
1. Create a realm called **`demo-realm`**.
2. Create a client called **`demo-client`** with client authentication turned on.
   Set the redirect URI to `http://localhost:8080/login/oauth2/code/keycloak`.
3. Add client roles **`USER`**, **`MANAGER`** and **`ADMIN`**.
4. Create test users and assign them roles.
5. Copy the client secret from the client's **Credentials** tab.

### 3. Run the app
```bash
export KEYCLOAK_CLIENT_SECRET=your_client_secret
./mvnw spring-boot:run
```
Open http://localhost:8080 and log in with one of your test users.

## Project structure

```text
src/main/java/com/learning/outh2_demo/
├── config/SecurityConfig.java           # Route rules, OAuth2 login and OIDC logout
├── service/CustomOidcUserService.java   # Maps Keycloak client roles to Spring authorities
└── controller/                          # Login, home and role pages
src/main/resources/
├── application.yml                      # Keycloak client registration
└── templates/                           # Thymeleaf pages
```
