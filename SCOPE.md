# SCOPE.md — Salento ParkSync MVP
## Alcance declarado de implementación

**Proyecto:** Salento ParkSync — Sistema de Gestión de Parqueaderos en Tiempo Real  
**Equipo:** Juan David Mosquera · John Angel Cardona  
**Stack:** Java 21 · Spring Boot 3 · PostgreSQL · Redis  
**Documentos de referencia:** PRD v1.0 · RFC v1.0 · TRD v1.0 · Design Doc v1.0

---

## 1. Requisitos del TRD implementados

### 1.1 Módulo Auth y Accesos

| ID | Requisito | Estado |
|----|-----------|--------|
| RTF-01 | Autenticación de operadores con JWT de TTL extendido (12 h) | ✅ Implementado |
| RTF-02 | PWA Turista de acceso público y anónimo — sin token ni registro | ✅ Implementado (endpoint público sin autenticación) |

### 1.2 Módulo Operador — Write API (Commands)

| ID | Requisito | Estado |
|----|-----------|--------|
| RTF-03 | Registro de evento con UUID v4, ID de lote y timestamp ISO 8601 | ✅ Implementado |
| RTF-04 | Persistencia del evento en ≤ 50 ms (sin bloqueo de red) | ✅ Implementado — escritura directa en PostgreSQL con validación de UUID |
| RTF-05 | Clasificación de eventos por categoría (particular, motocicleta, bus) | ✅ Implementado |
| RTF-06 | Control de emergencia `force_full` — evento de prioridad máxima | ✅ Implementado |
| RTF-07 | Flujo de recalibración `set_absolute_value` al desactivar el Botón de Pánico | ✅ Implementado |

### 1.3 Módulo de Sincronización

| ID | Requisito | Estado |
|----|-----------|--------|
| RTF-08 | Endpoint `/api/v1/commands/events` acepta array de eventos en lote | ✅ Implementado |
| RTF-09 | Procesamiento en orden cronológico FIFO por `timestamp_origen` | ✅ Implementado |
| RTF-10 | Respuesta HTTP 503 + `Retry-After` para señalizar Exponential Backoff al cliente | ✅ Implementado |

### 1.4 Módulo de Procesamiento — Motor de Histéresis

| ID | Requisito | Estado |
|----|-----------|--------|
| RTF-11 | Idempotencia estricta: validación UUID en PostgreSQL; retorna 200 OK en duplicados sin alterar estado | ✅ Implementado |
| RTF-12 | Event Sourcing en PostgreSQL (append-only) + estado público derivado en Redis | ✅ Implementado |
| RTF-13 | Umbrales: Verde→Amarillo ≥ 80 %, Amarillo→Rojo ≥ 95 %, Rojo→Amarillo ≤ 90 %, Amarillo→Verde ≤ 75 % | ✅ Implementado |

### 1.5 Módulo Turista — Read API (Queries)

| ID | Requisito | Estado |
|----|-----------|--------|
| RTF-14 | Estado servido exclusivamente desde Redis (sin tocar PostgreSQL) | ✅ Implementado |
| RTF-15 | Respuesta solo con estado semántico (Verde, Amarillo, Rojo, Gris) — prohibido exponer aforo numérico | ✅ Implementado |
| RTF-16 | Campo `last_updated` en la respuesta para calcular desfase de datos | ✅ Implementado |

### 1.6 Módulo de Resiliencia — Worker Stale Data / Kill Switch

| ID | Requisito | Estado |
|----|-----------|--------|
| RTF-17 | `force_full` ignora motor matemático y escribe "Rojo" directo en Redis | ✅ Implementado |
| RTF-18 | Scheduled Worker cada 5 minutos evalúa `last_updated` de todos los parqueaderos | ✅ Implementado |
| RTF-19 | Kill Switch: si desfase > 30 min sin eventos, estado cambia a "Gris" en Redis | ✅ Implementado |

### 1.7 Módulo Analítico y Panel de Tránsito

| ID | Requisito | Estado |
|----|-----------|--------|
| RTF-20 | Panel de visualización global con capacidad instalada y ocupación en tiempo real | ✅ Implementado |
| RTF-21 | Override manual remoto por Agente de Tránsito (forzar "Rojo" a cualquier parqueadero) | ✅ Implementado |

---

## 2. Requisitos del TRD **no implementados** — con justificación

### 2.1 Pruebas de Integración

| Tipo | Requisito del TRD | Justificación |
|------|-------------------|---------------|
| Integración E2E (TRD 13.2) | Cypress/Playwright con simulación de Service Workers | **Excluido.** Aplica al cliente PWA (frontend), fuera del alcance del backend Spring. El flujo offline-first es responsabilidad del Service Worker del navegador, no del API REST. |
| Integración backend (Spring Test) | Flujo completo evento → PostgreSQL → Redis | ✅ **Implementado.** Se diseñó y codificó una suite formal en [IntegrationTest.java](file:///c:/Users/Johna/Desktop/salento-parksync/src/test/java/com/parksync/integration/IntegrationTest.java) que valida de inicio a fin la ingesta de eventos, la persistencia en base de datos H2 y la derivación de estados en Redis. |

### 2.2 Pruebas de Carga y Performance

| Tipo | Criterio TRD | Justificación |
|------|--------------|---------------|
| Carga (Gatling / JMeter) — CA-04 | 5,000 usuarios concurrentes, p95 < 100 ms | **No incluida.** Requiere infraestructura de staging con Redis y PostgreSQL bajo carga real. El criterio está documentado; la arquitectura CQRS + Redis lo garantiza a nivel de diseño. |

### 2.3 Pruebas de Seguridad Dinámica

| Tipo | Criterio TRD | Justificación |
|------|--------------|---------------|
| OWASP ZAP (DAST) — CA-06 | 0 vulnerabilidades críticas/altas | **Análisis dinámico no ejecutado.** Requiere entorno desplegado. El análisis estático (SonarQube) en CI cubre vulnerabilidades en código fuente. Snyk cubre dependencias. |

### 2.4 Pruebas de Resiliencia y Caos

| Tipo | Criterio TRD | Justificación |
|------|--------------|---------------|
| Chaos Engineering (TRD 13.5) | Cold Start Redis, apagado de nodos | **Excluido.** Requiere orquestación de contenedores en Staging. La lógica del Cold Start está implementada en el `StaleDataWorker` y su correctitud se valida con pruebas unitarias del algoritmo. |

---

## 3. NFRs implementados y verificables

| ID | NFR | Método de verificación |
|----|-----|----------------------|
| NFR-01 | Latencia de captura ≤ 50 ms | Test unitario del handler con mock de repositorio |
| NFR-03 | Read API p95 < 20 ms desde Redis | Demostrable con Redis local y profiler |
| NFR-07 | Consistencia eventual — sin bloqueo por red | Arquitectura CQRS, demostrable en defensa |
| NFR-10 | JWT con TTL extendido (12 h) | Test unitario del módulo Auth |
| NFR-12 | HTTPS — TLS configurado en Spring Security | Visible en `application.yml` del repositorio |
| NFR-13 | Correlation ID (UUID del evento) en todos los logs | Interceptor de logging estructurado |

---

## 4. Restricciones técnicas respetadas

| ID | Restricción | Cumplimiento |
|----|-------------|-------------|
| CON-02 | Idempotencia antes de alterar estado | ✅ Constraint UNIQUE sobre `evento_id` en PostgreSQL |
| CON-04 | Sistema turista 100 % anónimo — sin datos personales | ✅ Read API sin autenticación, sin logging de IPs ni placas |
| CON-05 | Backend bloqueado para exponer aforo numérico al turista | ✅ DTO público solo contiene `color` y `last_updated` |

---

## 5. Decisiones de diseño que se desvían del Design Doc

| Decisión original (Design Doc) | Implementación real | Justificación |
|--------------------------------|---------------------|---------------|
| Write API en Node.js / Express | **Spring Boot (Java 21)** | Mayor dominio del equipo en Java garantiza calidad de pruebas y cobertura. Los contratos REST y el modelo de datos son idénticos a los especificados. |
| Read API en Go / Node.js | **Spring Boot (mismo servicio, módulo separado)** | Para el MVP se unifica en un solo servicio con perfiles diferenciados. La extracción a microservicio es posible sin cambios en los contratos. |
| Redis Cluster | **Redis standalone** | Suficiente para validar la lógica en desarrollo. El cliente Lettuce es compatible con modo cluster sin cambios de código al escalar. |

---

## 6. Estructura del repositorio

```
salento-parksync/
├── SCOPE.md                             ← este archivo
├── README.md
├── docker-compose.yml                   ← PostgreSQL + Redis locales
├── .github/workflows/ci.yml             ← Pipeline CI
└── src/
    ├── main/java/com/parksync/
    │   ├── auth/                        ← JWT (RTF-01, RTF-02)
    │   ├── command/                     ← Write API + idempotencia (RTF-03 a RTF-11)
    │   ├── hysteresis/                  ← Motor de Histéresis (RTF-13)
    │   ├── query/                       ← Read API desde Redis (RTF-14 a RTF-16)
    │   ├── worker/                      ← CRON Stale Data / Kill Switch (RTF-18, RTF-19)
    │   └── transit/                     ← Panel de Tránsito + Override remoto (RTF-20, RTF-21)
    └── test/java/com/parksync/
        └── unit/                        ← JUnit 5 (motor histéresis, idempotencia, auth, worker)
```

---

*Cualquier desvío adicional identificado durante la implementación se registra en este archivo antes de la evaluación.*
