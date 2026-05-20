# Salento ParkSync — Backend

Sistema de gestión de parqueaderos en tiempo real para el municipio de Salento, Quindío.

## Stack

- Java 21 · Spring Boot 3.2
- PostgreSQL 16 (Event Store inmutable)
- Redis 7 (Read Model / estado público)

## Levantar en local

```bash
# 1. Iniciar PostgreSQL + Redis
docker-compose up -d

# 2. Compilar y correr
./mvnw spring-boot:run
```

## Variables de entorno requeridas en producción

| Variable | Descripción |
|----------|-------------|
| `DATASOURCE_URL` | URL JDBC de PostgreSQL |
| `DATASOURCE_USER` | Usuario de BD |
| `DATASOURCE_PASSWORD` | Contraseña de BD |
| `REDIS_HOST` | Host de Redis |
| `JWT_SECRET` | Secreto JWT (mín. 32 caracteres) |

> **Nunca** incluir secretos en el repositorio (ver Design Doc §12.2).

## Ejecutar pruebas

```bash
./mvnw test                # pruebas unitarias
./mvnw verify              # pruebas + gate de cobertura JaCoCo ≥ 80 %
```

## Módulos

| Módulo | Responsabilidad | RTFs cubiertos |
|--------|-----------------|----------------|
| `auth` | JWT 12 h para operadores | RTF-01, RTF-02 |
| `command` | Write API + idempotencia UUID | RTF-03 a RTF-11 |
| `hysteresis` | Motor Verde/Amarillo/Rojo | RTF-13 |
| `query` | Read API exclusiva desde Redis | RTF-14 a RTF-16 |
| `worker` | CRON Stale Data / Kill Switch | RTF-17 a RTF-19 |
| `transit` | Panel de Tránsito + Override remoto | RTF-20, RTF-21 |

## Alcance declarado

Ver [SCOPE.md](SCOPE.md) para la lista completa de requisitos implementados y excluidos con justificación.
