---
title: "Monolith to Microservices with Agents"
description: "Decompose Java monolith to microservices using Spec2Cloud & SQUAD with Strangler Fig pattern"
authors: ["marconsilva"]
category: "Code Modernization"
industry: "Education"
services: ["Azure Container Apps", "Azure Service Bus", "Azure API Management", "Azure SQL Database"]
languages: ["Java"]
frameworks: ["Spring Boot", "Spring Cloud Gateway"]
modernizationTools: []
agenticTools: ["Spec2Cloud", "SQUAD"]
tags: ["microservices", "strangler-fig", "bounded-context", "saga-pattern", "spring-boot"]
extensions: ["github.copilot"]
thumbnail: ""
video: ""
version: "1.0.0"
---

## Overview

This lab decomposes a Java monolith into microservices using Spec2Cloud and SQUAD with the Strangler Fig pattern. The target application is EduVerse Academy, an education platform built on Tomcat 9 with PostgreSQL 14.

## Understanding the Monolith

The following sections show the EduVerse Academy monolith running locally via Docker Compose (Tomcat 9 + PostgreSQL 14). Understanding the current application is essential before identifying bounded contexts for microservice extraction.

The landing page features a hero banner, featured courses section, and navigation bar with links to the course catalog, sign-in, and more:

![Homepage](assets/screenshots/01-homepage.png)

Here is a wider view of the full homepage layout including all navigation elements and content sections:

![Homepage Full View](assets/screenshots/05-homepage-full.png)

![Homepage Alternate View](assets/screenshots/04-home-page.png)

### Authentication

Spring Security form-based authentication with username/password fields and a "Remember me" option:

![Login Page](assets/screenshots/02-login-page.png)

### Course Catalog — Bounded Context Example

The course catalog page (empty database — no courses seeded yet). This is one of the 8 bounded contexts targeted for extraction into a standalone microservice:

![Course Catalog](assets/screenshots/03-course-catalog.png)

## Screenshots Reference

| Screenshot | Description |
|---|---|
| ![Homepage](assets/screenshots/01-homepage.png) | Main landing page |
| ![Login Page](assets/screenshots/02-login-page.png) | Spring Security login |
| ![Course Catalog](assets/screenshots/03-course-catalog.png) | Course catalog (bounded context) |
| ![Homepage Alt](assets/screenshots/04-home-page.png) | Homepage alternate view |
| ![Homepage Full](assets/screenshots/05-homepage-full.png) | Full homepage layout |

## Solution Walkthrough

The `solution-final` branch contains the complete decomposition executed using **GH Copilot CLI** and **Squad CLI**. Each step is tagged for easy navigation.

### Prerequisites

- Java 21+ / JDK 21
- Docker Desktop
- Node.js 18+ (for Squad CLI)
- GitHub CLI with Copilot extension
- Azure subscription (for deployment)

### Branch & Tags

| Tag | Description |
|---|---|
| `step-01-spec2cloud-analysis` | Spec2Cloud bounded context identification and coupling analysis |
| `step-02-squad-init` | Squad workspace initialization with base roles |
| `step-03-squad-roles` | Agent hiring — Architect, ServiceExtractor, InfraOps, QualityGate |
| `step-04-squad-triage` | 8-phase Strangler Fig decomposition plan |
| `step-05-strangler-setup` | API Gateway (Spring Cloud Gateway) + shared events module + monolith REST APIs |
| `step-06-notification-service` | First extraction — Notification Service (least coupled) |
| `step-07-course-catalog` | Course Catalog Service with own database and event publishing |
| `step-08-payment-service` | Payment Service with enrollment-payment saga orchestrator |
| `step-09-full-decomposition` | Video, Certificate, Assessment, Progress, Enrollment services |
| `step-10-final-validation` | Azure Bicep IaC, GitHub Actions CI/CD, validation checklist |

### Step-by-Step Execution

#### Step 1 — Spec2Cloud Analysis

```bash
gh copilot -- -p "Analyze this Java monolith and identify bounded contexts..." --allow-all-tools --yolo
```

**Outcome:** Identified 8 bounded contexts with coupling matrix. Extraction order determined by coupling score (lowest first): Notification → Video → Certificate → Course Catalog → Assessment → Payment → Student Progress → Enrollment.

See: `eduverse-academy/BOUNDED-CONTEXT-ANALYSIS.md`

#### Step 2 — Squad Init

```bash
npx @bradygaster/squad-cli init --roles --no-workflows
```

**Outcome:** Initialized `.squad/` workspace with Scribe and Ralph agents, ceremonies, routing, and Copilot skills.

#### Step 3 — Squad Roles & Hiring

```bash
npx @bradygaster/squad-cli roles
npx @bradygaster/squad-cli hire lead --name "Architect"
npx @bradygaster/squad-cli hire backend --name "ServiceExtractor"
npx @bradygaster/squad-cli hire devops --name "InfraOps"
npx @bradygaster/squad-cli hire tester --name "QualityGate"
```

**Outcome:** 4 specialized agents hired for the decomposition pipeline.

#### Step 4 — Decomposition Plan (Squad Triage)

```bash
gh copilot -- -p "Create DECOMPOSITION-PLAN.md with 8-phase Strangler Fig plan..." --allow-all-tools --yolo
```

**Outcome:** Detailed plan with per-phase extraction steps, API contracts, saga design, gateway routing rules, and rollback strategies.

See: `DECOMPOSITION-PLAN.md`

#### Step 5 — Strangler Fig Setup

```bash
gh copilot -- -p "Set up API Gateway with Spring Cloud Gateway..." --allow-all-tools --yolo
```

**Outcome:**
- `api-gateway/` — Spring Cloud Gateway (Boot 3.2.5, Java 21) with 8 route definitions
- `shared-events/` — 13 domain event classes for Azure Service Bus
- `docker-compose.override.yml` — Gateway on port 8090 alongside monolith on 8080
- Monolith REST API controllers under `com.eduverse.controller.api`

#### Step 6 — Extract Notification Service

```bash
gh copilot -- -p "Extract Notification Service as first Strangler Fig microservice..." --allow-all-tools --yolo
```

**Outcome:** `notification-service/` with Spring Boot 3.x, Azure Service Bus listeners for StudentEnrolledEvent/PaymentCompletedEvent/CertificateIssuedEvent, email notification logic, own database schema.

#### Step 7 — Extract Course Catalog Service

```bash
gh copilot -- -p "Extract Course Catalog Service..." --allow-all-tools --yolo
```

**Outcome:** `course-catalog-service/` with Course/Module/Lesson entities, CRUD REST API, CoursePublishedEvent publisher, seed data for 5+ courses.

#### Step 8 — Extract Payment Service with Saga

```bash
gh copilot -- -p "Extract Payment Service with enrollment-payment saga..." --allow-all-tools --yolo
```

**Outcome:** `payment-service/` with EnrollmentPaymentSaga orchestrator (states: INITIATED → PAYMENT_PENDING → PAYMENT_COMPLETED/FAILED → REFUNDED), Stripe mock integration, compensating transactions.

#### Step 9 — Full Decomposition

```bash
gh copilot -- -p "Create remaining 5 services: video, certificate, assessment, progress, enrollment..." --allow-all-tools --yolo
```

**Outcome:** All 8 microservices running independently:
- `video-service/` — Upload, transcode, stream with Azure Blob Storage mock
- `certificate-service/` — PDF generation triggered by EnrollmentCompletedEvent
- `assessment-service/` — Quiz creation, grading, AssessmentPassedEvent
- `progress-service/` — Lesson completion tracking, percentage calculation
- `enrollment-service/` — Student enrollment with prerequisite checks, saga participant

#### Step 10 — Azure Deployment & Validation

```bash
gh copilot -- -p "Create Azure Bicep templates and GitHub Actions CI/CD..." --allow-all-tools --yolo
```

**Outcome:**
- `deployment/azure/main.bicep` — Container Apps environment, SQL databases, Service Bus, ACR, API Management
- `.github/workflows/deploy-all.yml` — Build all 9 Docker images, push to ACR, deploy to Container Apps
- `VALIDATION.md` — Comprehensive validation checklist and test scenarios

### Architecture Summary

```
┌─────────────────────────────────────────────────┐
│              Azure API Management                │
└──────────────────────┬──────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────┐
│            Spring Cloud Gateway (:8090)          │
│  /api/courses/**  → course-catalog-service       │
│  /api/enrollments/** → enrollment-service        │
│  /api/assessments/** → assessment-service        │
│  /api/videos/**   → video-service                │
│  /api/certificates/** → certificate-service      │
│  /api/payments/** → payment-service              │
│  /api/notifications/** → notification-service    │
│  /api/progress/** → progress-service             │
│  /**              → monolith (fallback)          │
└──────────────────────┬──────────────────────────┘
                       │
    ┌──────────────────┼──────────────────┐
    │                  │                  │
┌───▼───┐  ┌──────────▼──────┐  ┌───────▼────────┐
│Course │  │  Enrollment     │  │  Payment       │
│Catalog│  │  Service        │  │  Service       │
│Service│  │  (Saga Partic.) │  │  (Saga Orch.)  │
└───┬───┘  └────────┬────────┘  └───────┬────────┘
    │               │                   │
    └───────────────┼───────────────────┘
                    │
         ┌──────────▼──────────┐
         │  Azure Service Bus  │
         │  (Domain Events)    │
         └─────────────────────┘
```

### Key Patterns Demonstrated

| Pattern | Where Applied |
|---|---|
| **Strangler Fig** | Incremental extraction with gateway routing |
| **Database per Service** | Each service owns its schema and data |
| **Saga Orchestration** | Enrollment-payment flow in payment-service |
| **Event-Driven** | 13 domain events via Azure Service Bus |
| **API Gateway** | Spring Cloud Gateway with feature-flag cutover |
| **Bounded Context** | 8 contexts identified via Spec2Cloud analysis |

### CLI Outputs

All CLI tool outputs are captured in `assets/outputs/`:

| File | Tool Used |
|---|---|
| `step-01-spec2cloud-analysis.txt` | GH Copilot CLI |
| `step-02-squad-init.txt` | Squad CLI |
| `step-03-squad-roles.txt` | Squad CLI |
| `step-04-squad-triage.txt` | GH Copilot CLI |
| `step-05-strangler-setup.txt` | GH Copilot CLI |
| `step-06-notification-service.txt` | GH Copilot CLI |
| `step-07-course-catalog.txt` | GH Copilot CLI |
| `step-08-payment-service.txt` | GH Copilot CLI |
| `step-09-full-decomposition.txt` | GH Copilot CLI |
| `step-10-final-validation.txt` | GH Copilot CLI |
