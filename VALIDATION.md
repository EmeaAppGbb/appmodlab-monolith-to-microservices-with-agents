# EduVerse Academy — Validation Checklist

This document provides the comprehensive validation checklist for the fully decomposed EduVerse Academy microservices architecture. Use it to verify correctness at every stage: local development, CI/CD, and Azure production.

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Service Inventory](#2-service-inventory)
3. [Local Development Validation](#3-local-development-validation)
4. [Individual Service Tests](#4-individual-service-tests)
5. [Integration Test Scenarios](#5-integration-test-scenarios)
6. [Event-Driven Flow Validation](#6-event-driven-flow-validation)
7. [API Gateway Validation](#7-api-gateway-validation)
8. [Database-per-Service Validation](#8-database-per-service-validation)
9. [Docker Build Validation](#9-docker-build-validation)
10. [Azure Deployment Validation](#10-azure-deployment-validation)
11. [CI/CD Pipeline Validation](#11-cicd-pipeline-validation)
12. [Performance & Resilience](#12-performance--resilience)
13. [Security Checklist](#13-security-checklist)

---

## 1. Architecture Overview

```
┌────────────────────────────────────────────────────────────────────┐
│                        API Gateway (:8090)                        │
│                   Spring Cloud Gateway                            │
├──────────┬──────────┬──────────┬──────────┬──────────┬────────────┤
│ Course   │Enrollment│Assessment│  Video   │Certificate│  Payment  │
│ Catalog  │ Service  │ Service  │ Service  │  Service  │  Service  │
│ (:8085)  │ (:8092)  │ (:8089)  │ (:8087)  │  (:8088)  │ (:8086)  │
├──────────┴──────────┴──────────┴──────────┴──────────┴────────────┤
│       Notification Service (:8084)  │  Progress Service (:8091)   │
├───────────────────────────────────────────────────────────────────┤
│                    Azure Service Bus (Topics)                     │
├───────────────────────────────────────────────────────────────────┤
│   PostgreSQL (Database-per-Service: 8 databases)                  │
└───────────────────────────────────────────────────────────────────┘
```

## 2. Service Inventory

| # | Service                | Port | Database       | Publishes Events              | Consumes Events                                      |
|---|------------------------|------|----------------|-------------------------------|------------------------------------------------------|
| 1 | notification-service   | 8084 | notifications  | —                             | student-enrolled, payment-completed, certificate-issued |
| 2 | course-catalog-service | 8085 | coursecatalog  | —                             | —                                                    |
| 3 | payment-service        | 8086 | payments       | payment-completed, payment-failed | student-enrolled                                  |
| 4 | video-service          | 8087 | videos         | —                             | —                                                    |
| 5 | certificate-service    | 8088 | certificates   | certificate-issued            | enrollment-completed                                 |
| 6 | assessment-service     | 8089 | assessments    | assessment-passed             | —                                                    |
| 7 | progress-service       | 8091 | progress       | progress-updated              | enrollment-activated, assessment-passed               |
| 8 | enrollment-service     | 8092 | enrollments    | student-enrolled, enrollment-activated, enrollment-completed | payment-completed, payment-failed, progress-updated, certificate-issued |
| 9 | api-gateway            | 8090 | —              | —                             | —                                                    |
|10 | monolith (fallback)    | 8080 | eduverse       | —                             | —                                                    |

## 3. Local Development Validation

### Prerequisites
- [ ] Docker Desktop installed and running
- [ ] At least 8 GB RAM allocated to Docker
- [ ] Ports 5432, 5433, 8080, 8084-8092 available

### Start Full Stack
```bash
docker compose up -d
```

### Verify All Containers Running
```bash
docker compose ps
# Expected: 11 running containers (2 postgres + monolith + gateway + 8 services)
# shared-events shows as a build-only image (not a running container)
```

### Health Check All Services
```bash
# API Gateway
curl http://localhost:8090/actuator/health

# Each microservice directly
for port in 8084 8085 8086 8087 8088 8089 8091 8092; do
  echo "Port $port: $(curl -s -o /dev/null -w '%{http_code}' http://localhost:$port/actuator/health)"
done
```

**Expected**: All return HTTP 200 with `{"status":"UP"}`.

## 4. Individual Service Tests

Run unit and integration tests for each service:

```bash
# From repository root, for each service:
cd <service-name>
mvn test

# Services with tests:
# ✅ notification-service    — controller + service + repository tests
# ✅ course-catalog-service  — controller + service + repository tests
# ✅ payment-service         — controller + service + repository tests
# ✅ video-service           — controller + service + repository tests
# ✅ certificate-service     — controller + service + repository tests
# ✅ assessment-service      — controller + service + repository tests
# ✅ progress-service        — controller + service + repository tests
# ✅ enrollment-service      — controller + service + repository tests
```

### Test Verification Checklist
- [ ] All 8 services pass `mvn test` with 0 failures
- [ ] Tests use H2 in-memory database (`@ActiveProfiles("test")`)
- [ ] Controller tests use `@WebMvcTest` + `@MockBean`
- [ ] Repository tests use `@DataJpaTest`

## 5. Integration Test Scenarios

### Scenario 1: Course Browsing (Read Path)
```bash
# List all courses via gateway
curl http://localhost:8090/api/courses
# Expected: 200 OK, JSON array of courses

# Get single course
curl http://localhost:8090/api/courses/1
# Expected: 200 OK, course JSON object
```

### Scenario 2: Student Enrollment (Write Path)
```bash
# Create enrollment via gateway
curl -X POST http://localhost:8090/api/enrollments \
  -H "Content-Type: application/json" \
  -d '{"studentId": 1, "courseId": 1}'
# Expected: 201 Created, enrollment JSON with PENDING status
```

### Scenario 3: Payment Processing
```bash
# Process payment via gateway
curl -X POST http://localhost:8090/api/payments \
  -H "Content-Type: application/json" \
  -d '{"enrollmentId": 1, "amount": 99.99, "currency": "USD"}'
# Expected: 200 OK, payment confirmation
```

### Scenario 4: Assessment Submission
```bash
# Submit assessment via gateway
curl -X POST http://localhost:8090/api/assessments/submit \
  -H "Content-Type: application/json" \
  -d '{"studentId": 1, "courseId": 1, "score": 85}'
# Expected: 200 OK, assessment result
```

### Scenario 5: Video Management
```bash
# List course videos
curl http://localhost:8090/api/videos/course/1
# Expected: 200 OK, JSON array of videos
```

### Scenario 6: Certificate Retrieval
```bash
# Get certificates for student
curl http://localhost:8090/api/certificates/student/1
# Expected: 200 OK, JSON array of certificates
```

### Scenario 7: Progress Tracking
```bash
# Get student progress
curl http://localhost:8090/api/progress/student/1
# Expected: 200 OK, progress data
```

### Scenario 8: Notification History
```bash
# Get notifications for student
curl http://localhost:8090/api/notifications/student/1
# Expected: 200 OK, JSON array of notifications
```

## 6. Event-Driven Flow Validation

### Complete Enrollment Flow (Happy Path)
This validates the full event chain across services:

```
1. POST /api/enrollments → enrollment-service creates enrollment (PENDING)
   └─ Publishes: student-enrolled

2. payment-service receives student-enrolled
   └─ Creates payment record, processes payment
   └─ Publishes: payment-completed

3. enrollment-service receives payment-completed
   └─ Updates enrollment status to ACTIVATED
   └─ Publishes: enrollment-activated

4. notification-service receives student-enrolled
   └─ Sends enrollment confirmation notification

5. notification-service receives payment-completed
   └─ Sends payment confirmation notification

6. progress-service receives enrollment-activated
   └─ Initializes progress tracking for student

7. (After course completion) enrollment-service publishes enrollment-completed

8. certificate-service receives enrollment-completed
   └─ Generates certificate
   └─ Publishes: certificate-issued

9. enrollment-service receives certificate-issued
   └─ Updates enrollment with certificate reference

10. notification-service receives certificate-issued
    └─ Sends certificate notification
```

### Validation Steps
- [ ] Check enrollment status transitions: PENDING → ACTIVATED → COMPLETED
- [ ] Verify payment record created after enrollment
- [ ] Verify progress initialized after payment
- [ ] Verify certificate generated after completion
- [ ] Verify all notifications sent at each stage
- [ ] Check dead-letter queues are empty (no failed messages)

### Payment Failure Flow
```
1. POST /api/enrollments → enrollment-service creates enrollment
   └─ Publishes: student-enrolled

2. payment-service receives student-enrolled
   └─ Payment fails (insufficient funds, etc.)
   └─ Publishes: payment-failed

3. enrollment-service receives payment-failed
   └─ Updates enrollment status to PAYMENT_FAILED
```

- [ ] Enrollment status changes to PAYMENT_FAILED
- [ ] No enrollment-activated event is published
- [ ] Progress is NOT initialized

## 7. API Gateway Validation

### Route Verification
```bash
# Verify each route forwards correctly
curl -v http://localhost:8090/api/courses          # → course-catalog:8085
curl -v http://localhost:8090/api/enrollments       # → enrollment:8092
curl -v http://localhost:8090/api/assessments       # → assessment:8089
curl -v http://localhost:8090/api/videos            # → video:8087
curl -v http://localhost:8090/api/certificates      # → certificate:8088
curl -v http://localhost:8090/api/payments          # → payment:8086
curl -v http://localhost:8090/api/notifications     # → notification:8084
curl -v http://localhost:8090/api/progress          # → progress:8091
```

### Gateway Health
```bash
# Gateway actuator
curl http://localhost:8090/actuator/health
curl http://localhost:8090/actuator/gateway/routes
```

### Strangler Fig Pattern
- [ ] All `/api/*` routes forward to extracted microservices
- [ ] `/**` fallback route sends unmatched paths to monolith
- [ ] No direct client access to microservice ports required

## 8. Database-per-Service Validation

### Verify Database Isolation
```bash
# Connect to services database
docker exec -it eduverse-postgres-services psql -U postgres

# List databases
\l
# Expected: notifications, coursecatalog, payments, videos,
#           certificates, assessments, progress, enrollments

# Verify tables exist in each database
\c notifications
\dt
\c coursecatalog
\dt
# ... repeat for all databases
```

### Data Isolation Checks
- [ ] Each service only writes to its own database
- [ ] No cross-database joins or foreign keys
- [ ] Schema migrations run independently per service
- [ ] Service starts even if other databases are unavailable

## 9. Docker Build Validation

### Build All Images
```bash
docker compose build
# Expected: All 10 images build successfully
```

### Verify Image Sizes
```bash
docker images | grep eduverse
# Guideline: Each service image should be < 400 MB (JRE-based)
```

### Build Checklist
- [ ] All Dockerfiles use multi-stage builds (build + runtime)
- [ ] Runtime stage uses `eclipse-temurin:21-jre` (not JDK)
- [ ] shared-events dependency resolved for all 8 microservices
- [ ] Each image exposes the correct port
- [ ] No secrets baked into images

## 10. Azure Deployment Validation

### Infrastructure Verification
```bash
# After Bicep deployment:
az deployment group show \
  --resource-group rg-eduverse-dev \
  --name main \
  --query properties.outputs

# Verify resources created
az resource list -g rg-eduverse-dev -o table
```

### Expected Azure Resources
- [ ] Azure Container Apps Environment
- [ ] 9 Container Apps (8 services + gateway)
- [ ] Azure Container Registry (Standard SKU)
- [ ] Azure Database for PostgreSQL Flexible Server with 8 databases
- [ ] Azure Service Bus namespace with 8 topics and 11 subscriptions
- [ ] Log Analytics Workspace
- [ ] User-Assigned Managed Identity
- [ ] API Management (prod environment only)

### Container Apps Health
```bash
# Check each container app status
for app in api-gateway notification course-catalog payment video certificate assessment progress enrollment; do
  STATUS=$(az containerapp show \
    --name "eduverse-dev-${app}" \
    --resource-group rg-eduverse-dev \
    --query "properties.runningStatus.observedState" -o tsv)
  echo "${app}: ${STATUS}"
done
```

### Service Bus Verification
```bash
# List topics
az servicebus topic list \
  --namespace-name eduverse-dev-servicebus \
  --resource-group rg-eduverse-dev \
  --query "[].name" -o tsv

# Expected topics: student-enrolled, enrollment-activated, enrollment-completed,
# payment-completed, payment-failed, assessment-passed, progress-updated, certificate-issued
```

### Database Connectivity
```bash
# Test PostgreSQL connectivity from Container Apps
az containerapp exec \
  --name eduverse-dev-course-catalog \
  --resource-group rg-eduverse-dev \
  --command "curl -s localhost:8085/actuator/health"
# Expected: {"status":"UP","components":{"db":{"status":"UP"}}}
```

## 11. CI/CD Pipeline Validation

### GitHub Actions Secrets Required
| Secret                    | Description                          |
|---------------------------|--------------------------------------|
| `AZURE_CLIENT_ID`         | Service principal client ID          |
| `AZURE_TENANT_ID`         | Azure AD tenant ID                   |
| `AZURE_SUBSCRIPTION_ID`   | Azure subscription ID                |
| `DB_ADMIN_LOGIN`          | PostgreSQL admin username            |
| `DB_ADMIN_PASSWORD`       | PostgreSQL admin password            |

### Pipeline Stages
- [ ] **Build shared-events**: Compiles shared-events library
- [ ] **Build & Push**: All 9 images built in parallel, pushed to ACR
- [ ] **Deploy Infrastructure**: Bicep template applied successfully
- [ ] **Deploy Services**: All container apps updated with new images
- [ ] **Smoke Tests**: All health endpoints return 200

### Pipeline Checklist
- [ ] Workflow triggers on push to `main`
- [ ] Manual dispatch available with environment selection
- [ ] Build matrix runs all 9 services in parallel
- [ ] Deployment uses OIDC (federated credentials, no secrets in code)
- [ ] Smoke tests validate all routes through the gateway
- [ ] Failed builds do not trigger deployment

## 12. Performance & Resilience

### Load Testing Baseline
```bash
# Example with hey (https://github.com/rakyll/hey)
hey -n 1000 -c 50 http://localhost:8090/api/courses

# Expected baseline (local):
# - p99 latency < 500ms for read endpoints
# - p99 latency < 2s for write endpoints (with event publishing)
# - 0% error rate under moderate load
```

### Resilience Checks
- [ ] Service continues when Service Bus is unavailable (graceful degradation)
- [ ] API Gateway returns 503 when a backend service is down (not 500)
- [ ] Services recover automatically when dependencies come back online
- [ ] Container Apps auto-scale under load (HTTP scaling rule at 50 concurrent requests)

### Failure Injection
```bash
# Stop a single service and verify gateway behavior
docker compose stop payment
curl http://localhost:8090/api/payments
# Expected: 503 Service Unavailable (not a connection error)

# Restart and verify recovery
docker compose start payment
sleep 10
curl http://localhost:8090/api/payments
# Expected: 200 OK
```

## 13. Security Checklist

### Infrastructure Security
- [ ] Azure Container Registry: admin user disabled, ACR pull via managed identity
- [ ] PostgreSQL: SSL required (`sslmode=require` in JDBC URL)
- [ ] Service Bus: TLS 1.2 minimum
- [ ] Container Apps: HTTPS ingress with auto TLS certificates
- [ ] Internal services: no external ingress (only gateway is externally accessible)
- [ ] API Management: rate limiting and throttling (prod)

### Application Security
- [ ] No secrets in Docker images or source code
- [ ] Secrets passed via environment variables / Azure Key Vault
- [ ] Database credentials stored in GitHub Secrets (CI) and Container App secrets (runtime)
- [ ] Service Bus connection string stored as Container App secret
- [ ] CORS configured on API Gateway only

### Network Isolation
- [ ] Microservices communicate only within Container Apps environment
- [ ] Only API Gateway has external ingress
- [ ] PostgreSQL accessible only from Container Apps (firewall rules)
- [ ] Service Bus accessible only from Azure services

---

## Quick Validation Script

Run this script to perform a rapid validation of local development setup:

```bash
#!/bin/bash
echo "=== EduVerse Academy Validation ==="

echo ""
echo "1. Checking Docker containers..."
RUNNING=$(docker compose ps --format json | grep -c '"running"')
echo "   Running containers: $RUNNING"

echo ""
echo "2. Health checks..."
SERVICES=("8084:notification" "8085:course-catalog" "8086:payment" "8087:video" "8088:certificate" "8089:assessment" "8091:progress" "8092:enrollment" "8090:api-gateway")
PASS=0
FAIL=0
for svc in "${SERVICES[@]}"; do
  PORT="${svc%%:*}"
  NAME="${svc##*:}"
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${PORT}/actuator/health" --max-time 5 2>/dev/null)
  if [ "$STATUS" = "200" ]; then
    echo "   ✅ ${NAME} (${PORT})"
    PASS=$((PASS + 1))
  else
    echo "   ❌ ${NAME} (${PORT}) — HTTP ${STATUS}"
    FAIL=$((FAIL + 1))
  fi
done

echo ""
echo "3. Gateway routing..."
ROUTES=("/api/courses" "/api/enrollments" "/api/assessments" "/api/videos" "/api/certificates" "/api/payments" "/api/notifications" "/api/progress")
for route in "${ROUTES[@]}"; do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8090${route}" --max-time 5 2>/dev/null)
  echo "   ${route} → HTTP ${STATUS}"
done

echo ""
echo "=== Results: ${PASS} passed, ${FAIL} failed ==="
```

---

*Generated as part of the EduVerse Academy monolith-to-microservices decomposition.*
*Last updated: April 2026*
