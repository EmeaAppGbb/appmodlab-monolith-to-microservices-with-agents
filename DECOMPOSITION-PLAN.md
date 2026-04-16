# EduVerse Academy — Strangler Fig Decomposition Plan

> **Created:** 2026-04-16 | **Source:** Bounded Context Analysis  
> **Monolith:** Java 11 / Spring MVC 5.3 / Hibernate 5.6 / PostgreSQL 14  
> **Target:** Azure Container Apps + Azure Service Bus + Azure API Management  
> **Pattern:** Strangler Fig with Choreography Saga  
> **Timeline:** 20 weeks (5 months)

---

## Table of Contents

1. [Strategy Overview](#1-strategy-overview)
2. [Prerequisites & Foundation](#2-prerequisites--foundation)
3. [Phase 1 — Notification Service](#3-phase-1--notification-service-weeks-13)
4. [Phase 2 — Video Service](#4-phase-2--video-service-weeks-46)
5. [Phase 3 — Certificate Service](#5-phase-3--certificate-service-weeks-68)
6. [Phase 4 — Course Catalog Service](#6-phase-4--course-catalog-service-weeks-911)
7. [Phase 5 — Assessment Service](#7-phase-5--assessment-service-weeks-1113)
8. [Phase 6 — Payment Service](#8-phase-6--payment-service-weeks-1316)
9. [Phase 7 — Student Progress Service](#9-phase-7--student-progress-service-weeks-1618)
10. [Phase 8 — Enrollment Service](#10-phase-8--enrollment-service-weeks-1820)
11. [Saga Pattern: Enrollment-Payment Flow](#11-saga-pattern-enrollment-payment-flow)
12. [API Gateway Routing Rules](#12-api-gateway-routing-rules)
13. [Risk Assessment](#13-risk-assessment)
14. [Rollback Strategy](#14-rollback-strategy)
15. [Success Criteria & Observability](#15-success-criteria--observability)

---

## 1. Strategy Overview

### Strangler Fig Approach

The Strangler Fig pattern incrementally replaces monolith functionality by routing traffic through an API Gateway that can direct requests to either the monolith or new microservices. Each phase:

1. **Build** the new microservice with its own database (Database-per-Service pattern).
2. **Deploy** alongside the monolith behind the API Gateway.
3. **Route** specific URL patterns from the monolith to the new service.
4. **Verify** with parallel execution and shadow traffic.
5. **Decommission** the corresponding code from the monolith.

### Extraction Order Rationale

Services are extracted from **least coupled to most coupled**, building confidence and event infrastructure incrementally:

```
Phase 1:  [1. Notification]  ← Pure event sink, zero outgoing deps
              │
Phase 2:  [2. Video]         ← Zero incoming callers, isolated pipeline
              │
Phase 3:  [3. Certificate]   ← Read-only outgoing deps, one incoming caller
              │
Phase 4:  [4. Course Catalog] ← Read-heavy, stable domain
              │
Phase 5:  [5. Assessment]    ← Two outgoing deps (both already event-driven)
              │
Phase 6:  [6. Payment]       ← Critical anti-patterns to fix, saga participant
              │
Phase 7:  [7. Student Progress] ← Writes to Enrollment, reads Course structure
              │
Phase 8:  [8. Enrollment]    ← Central orchestrator, saga coordinator (LAST)
```

### Key Principles

- **Database-per-Service**: Each microservice owns its tables exclusively. No cross-service SQL joins.
- **Soft References**: Cross-context FKs become application-level references (store IDs, no DB constraints).
- **Event-Driven Communication**: Synchronous service calls become Azure Service Bus topic/subscription messages.
- **Shared Kernel**: User/Identity remains a shared concern via JWT claims and a lightweight Identity API.
- **Feature Flags**: Every routing change is gated behind feature flags for instant rollback.

---

## 2. Prerequisites & Foundation

### Infrastructure Setup (Week 0)

Before extracting any service, establish the target platform:

| Component | Technology | Purpose |
|---|---|---|
| **Container Platform** | Azure Container Apps | Host microservices with auto-scaling |
| **API Gateway** | Azure API Management | Traffic routing, rate limiting, auth |
| **Message Broker** | Azure Service Bus (Premium) | Async event-driven communication |
| **Service Registry** | Azure Container Apps built-in | Service discovery |
| **Secrets Management** | Azure Key Vault | Credentials, connection strings |
| **Monitoring** | Azure Monitor + Application Insights | Distributed tracing, metrics, alerting |
| **Container Registry** | Azure Container Registry | Docker image storage |
| **CI/CD** | GitHub Actions | Build, test, deploy pipelines |

### Service Bus Topic Setup

Create all topics and subscriptions upfront to allow monolith-side event publishing from Phase 1:

```
Topics:
  ├── course-published
  ├── student-enrolled
  ├── enrollment-activated
  ├── enrollment-completed
  ├── payment-completed
  ├── payment-failed
  ├── payment-refunded
  ├── lesson-completed
  ├── progress-updated
  ├── assessment-passed
  ├── certificate-issued
  ├── video-ready
  └── reminder-needed
```

### Identity / Authentication Strategy

- Extract User and Authority tables into a **shared Identity database** (read-only for most services).
- Issue **JWT tokens** at the API Gateway. Microservices validate tokens locally using public keys.
- User profile data is fetched via a lightweight **Identity API** or embedded as JWT claims.

### Monolith Preparation

1. **Add event publishing to the monolith**: Introduce a `DomainEventPublisher` interface in the monolith that publishes to Azure Service Bus. Initially, the monolith publishes events alongside its existing synchronous calls (dual-write with idempotency keys).
2. **Add feature flags**: Integrate a feature flag system (e.g., Azure App Configuration) to control routing at the gateway level.
3. **Add correlation IDs**: Inject a `X-Correlation-ID` header at the API Gateway and propagate through all service calls and events for distributed tracing.

---

## 3. Phase 1 — Notification Service (Weeks 1–3)

### Rationale

Notification is the **purest event sink** in the system. It has zero outgoing service dependencies, no REST controllers, and is only called by other services and schedulers. This makes it the ideal first extraction: it validates the event infrastructure without affecting any user-facing API routes.

### Current State

| Aspect | Detail |
|---|---|
| Entities | Notification |
| Tables | `notifications` |
| Coupling | 0 outgoing service calls, 3 incoming callers (CourseService, EnrollmentService, EnrollmentReminderJob) |
| Anti-patterns | Hardcoded SMTP config, synchronous email sending |

### Target Architecture

```
Azure Service Bus                 ┌─────────────────────┐
  ├── course-published ──────────→│                     │
  ├── student-enrolled ──────────→│  Notification       │──→ SMTP / SendGrid
  ├── payment-completed ─────────→│  Service            │──→ Azure Notification Hub
  ├── enrollment-completed ──────→│  (Azure Container   │──→ In-App WebSocket
  ├── certificate-issued ────────→│   Apps)             │
  ├── assessment-passed ─────────→│                     │
  └── reminder-needed ───────────→└─────────────────────┘
                                          │
                                          ▼
                                   notifications DB
                                   (own PostgreSQL)
```

### Steps

| # | Task | Details |
|---|---|---|
| 1.1 | Create microservice skeleton | Spring Boot 3.x, Java 17, dedicated PostgreSQL Flexible Server |
| 1.2 | Migrate `notifications` table | Copy schema; no FK changes needed (user_id is a soft reference) |
| 1.3 | Implement Service Bus consumers | Subscribe to all relevant domain event topics |
| 1.4 | Replace SMTP with SendGrid/Azure Communication Services | Externalize email config via Key Vault |
| 1.5 | Add monolith event publishing | Modify `NotificationService` callers in monolith to publish events to Service Bus instead of calling `NotificationService.send()` directly |
| 1.6 | Shadow testing | Run both old and new notification paths; compare outputs |
| 1.7 | Cut over | Disable monolith `NotificationService`; all notifications flow through events |
| 1.8 | Decommission | Remove `NotificationService.java`, `NotificationRepository.java`, `Notification.java` from monolith |

### API Gateway Routing

No API Gateway routing changes needed — Notification has no REST endpoints. It is purely event-driven.

### Validation Criteria

- [ ] All notification types (EMAIL, IN_APP, SMS) delivered within SLA (< 30 seconds)
- [ ] No duplicate notifications (idempotency via `event_id` deduplication)
- [ ] Dead-letter queue alerting configured
- [ ] Zero impact on monolith response times

---

## 4. Phase 2 — Video Service (Weeks 4–6)

### Rationale

Video Service has **zero incoming callers** and a single outgoing write (Lesson.video_url). It contains a critical anti-pattern (`Thread.sleep` inside `@Transactional`) that causes connection pool exhaustion under load. Extraction eliminates this and enables true async video processing.

### Current State

| Aspect | Detail |
|---|---|
| Entities | Video |
| Tables | `videos` |
| Coupling | 0 incoming callers, 1 outgoing write (Lesson.video_url via LessonRepository) |
| Anti-patterns | `Thread.sleep(2000)` inside `@Transactional` in `processVideo()` |

### Target Architecture

```
                    ┌──────────────┐
  Upload API ──────→│ Video Service│──→ Azure Blob Storage
  /video/*          │ (Container   │
                    │  Apps)       │──→ Azure Media Services / FFmpeg worker
                    └──────┬───────┘
                           │
                    ┌──────┴───────┐
                    │  videos DB   │
                    │  (own PgSQL) │
                    └──────────────┘
                           │
                    VideoReady event
                           │
                           ▼
                    Service Bus → Monolith (updates Lesson.video_url)
```

### Steps

| # | Task | Details |
|---|---|---|
| 2.1 | Create microservice | Spring Boot 3.x with async processing via `@Async` or message-driven workers |
| 2.2 | Migrate `videos` table | Copy schema to dedicated database |
| 2.3 | Implement async transcoding | Replace `Thread.sleep` with real async worker; use Azure Blob Storage for uploads, emit status events |
| 2.4 | Implement `VideoReady` event publishing | On transcode completion, publish `VideoReady` event with `lessonId` and `transcodedUrls` |
| 2.5 | Monolith event consumer | Monolith subscribes to `VideoReady` to update `Lesson.video_url` (replaces direct LessonRepository write) |
| 2.6 | API Gateway routing | Route `/video/*` to new Video Service |
| 2.7 | Validation | Upload, process, verify streaming URL works end-to-end |
| 2.8 | Decommission | Remove `VideoService.java`, `VideoRepository.java`, `Video.java`, `VideoController.java` from monolith |

### API Gateway Routing

| Route Pattern | Target | Method | Auth |
|---|---|---|---|
| `/video/upload` | Video Service | POST | ROLE_INSTRUCTOR |
| `/video/{id}` | Video Service | GET | Authenticated |
| `/video/{id}/status` | Video Service | GET | ROLE_INSTRUCTOR |
| `/video/{id}/stream` | Video Service | GET | Authenticated |

### Validation Criteria

- [ ] Video upload and async processing works end-to-end
- [ ] `VideoReady` event correctly updates Lesson in monolith
- [ ] No `Thread.sleep` in any code path
- [ ] P95 upload latency < 2s (excluding actual transcoding)
- [ ] Video status polling returns real processing state

---

## 5. Phase 3 — Certificate Service (Weeks 6–8)

### Rationale

Certificate has only **one incoming caller** (EnrollmentService.completeEnrollment) and all outgoing dependencies are **read-only** (Enrollment, Course, User data for PDF content). PDF generation is a perfect isolated concern that benefits from async processing.

### Current State

| Aspect | Detail |
|---|---|
| Entities | Certificate |
| Tables | `certificates` |
| Coupling | 1 incoming caller, 3 outgoing reads (Enrollment, Course, User repos) |
| Notes | iTextPDF generation embedded in service layer |

### Target Architecture

```
EnrollmentCompleted event           ┌─────────────────────┐
  (Service Bus) ───────────────────→│ Certificate Service  │
                                    │ (Container Apps)     │
                                    │                      │──→ PDF stored in Azure Blob
  GET /certificate/* ──────────────→│ REST API             │
                                    └──────────┬───────────┘
                                               │
                                    ┌──────────┴───────────┐
                                    │ certificates DB      │
                                    │ + cached course/user │
                                    └──────────────────────┘
                                               │
                                    CertificateIssued event → Service Bus
```

### Steps

| # | Task | Details |
|---|---|---|
| 3.1 | Create microservice | Spring Boot 3.x with iTextPDF dependency |
| 3.2 | Migrate `certificates` table | Copy schema; `enrollment_id` becomes soft reference |
| 3.3 | Implement local cache for course/user data | Subscribe to `CoursePublished` events; call Identity API for user name/email. Cache with TTL. |
| 3.4 | Implement `EnrollmentCompleted` consumer | Generate certificate PDF asynchronously; store in Azure Blob Storage |
| 3.5 | Publish `CertificateIssued` event | Includes `certificateId`, `enrollmentId`, `pdfUrl` |
| 3.6 | Expose REST API | GET endpoints for certificate retrieval and verification |
| 3.7 | API Gateway routing | Route `/certificate/*` to new service |
| 3.8 | Decommission | Remove certificate code from monolith; EnrollmentService triggers via event only |

### API Gateway Routing

| Route Pattern | Target | Method | Auth |
|---|---|---|---|
| `/certificate/{id}` | Certificate Service | GET | Authenticated |
| `/certificate/{id}/download` | Certificate Service | GET | Authenticated |
| `/certificate/verify/{number}` | Certificate Service | GET | Public |
| `/certificate/enrollment/{id}` | Certificate Service | GET | Authenticated |

### Validation Criteria

- [ ] PDF generated correctly with course title, student name, completion date
- [ ] Certificate number uniqueness maintained across migration
- [ ] Download endpoint returns correct PDF from Blob Storage
- [ ] Verify endpoint works without authentication
- [ ] P95 generation time < 10 seconds (async, non-blocking)

---

## 6. Phase 4 — Course Catalog Service (Weeks 9–11)

### Rationale

Course Catalog is the **most read-heavy** domain context. It has a stable, well-defined aggregate (Course → Module → Lesson) and only a single outgoing dependency (Notification on publish, already event-driven from Phase 1). Extracting it provides a clean read API that all other services can depend on.

### Current State

| Aspect | Detail |
|---|---|
| Entities | Course, Module, Lesson |
| Tables | `courses`, `modules`, `lessons` |
| Coupling | 1 outgoing (Notification, now via events), 4 incoming readers |
| Aggregate Root | Course (cascade ALL → Module → Lesson) |

### Target Architecture

```
  REST API                   ┌─────────────────────┐
  /catalog/*, /course/* ────→│ Course Catalog       │
                             │ Service              │
                             │ (Container Apps)     │
                             └──────────┬───────────┘
                                        │
                             ┌──────────┴───────────┐
                             │ courses, modules,    │
                             │ lessons DB (own PgSQL)│
                             └──────────────────────┘
                                        │
                             CoursePublished event → Service Bus
                                        │
                             ┌──────────┴───────────┐
                             │ Consumers:           │
                             │  - Notification      │
                             │  - Video (Lesson ref)│
                             │  - Assessment (cache)│
                             │  - Progress (cache)  │
                             └──────────────────────┘
```

### Steps

| # | Task | Details |
|---|---|---|
| 4.1 | Create microservice | Spring Boot 3.x with full CRUD for Course/Module/Lesson |
| 4.2 | Migrate tables | `courses`, `modules`, `lessons` to dedicated database; `instructor_id` remains a soft reference |
| 4.3 | Expose read API | GET endpoints for course listing, search, module/lesson tree |
| 4.4 | Expose write API | POST/PUT/DELETE for instructors and admins |
| 4.5 | Publish `CoursePublished` event | On course status change to PUBLISHED |
| 4.6 | Update monolith consumers | Enrollment, Assessment, Progress, Certificate services call Course API instead of direct DB |
| 4.7 | Implement CQRS read model (optional) | Materialized views for search and listing |
| 4.8 | API Gateway routing | Route `/catalog/*`, `/course/*` to new service |
| 4.9 | Decommission | Remove course code from monolith; update `HomeController` to call Course API |

### API Gateway Routing

| Route Pattern | Target | Method | Auth |
|---|---|---|---|
| `/catalog` | Course Catalog Service | GET | Public |
| `/catalog/search` | Course Catalog Service | GET | Public |
| `/course/{id}` | Course Catalog Service | GET | Public |
| `/course/{id}/modules` | Course Catalog Service | GET | Authenticated |
| `/course` | Course Catalog Service | POST | ROLE_INSTRUCTOR |
| `/course/{id}` | Course Catalog Service | PUT | ROLE_INSTRUCTOR |
| `/course/{id}/publish` | Course Catalog Service | POST | ROLE_INSTRUCTOR |
| `/course/{id}/module` | Course Catalog Service | POST | ROLE_INSTRUCTOR |
| `/course/{id}/module/{mid}/lesson` | Course Catalog Service | POST | ROLE_INSTRUCTOR |

### Validation Criteria

- [ ] All existing course CRUD operations work through new service
- [ ] HomeController and search render correctly
- [ ] Enrollment, Assessment, and Progress services successfully call Course API
- [ ] `CoursePublished` event triggers notification
- [ ] No direct `courses`, `modules`, `lessons` table access from monolith

---

## 7. Phase 5 — Assessment Service (Weeks 11–13)

### Rationale

Assessment has two outgoing dependencies (ProgressService and EnrollmentRepository read), both of which are event-driven by this phase. It is a standalone entry point with no incoming callers from other services.

### Current State

| Aspect | Detail |
|---|---|
| Entities | Assessment, StudentAnswer |
| Tables | `assessments`, `student_answers` |
| Coupling | 2 outgoing (Progress + Enrollment read), 0 incoming |
| Notes | Auto-grading for quizzes; JSON-encoded questions/answers |

### Target Architecture

```
  REST API                   ┌─────────────────────┐
  /assessment/* ────────────→│ Assessment Service   │
                             │ (Container Apps)     │──→ AssessmentPassed event
                             └──────────┬───────────┘
                                        │
                             ┌──────────┴───────────┐
                             │ assessments,          │
                             │ student_answers DB    │
                             └──────────────────────┘
```

### Steps

| # | Task | Details |
|---|---|---|
| 5.1 | Create microservice | Spring Boot 3.x with quiz engine and grading logic |
| 5.2 | Migrate tables | `assessments`, `student_answers`; `lesson_id` and `student_id` become soft references |
| 5.3 | Replace ProgressService call with event | On quiz pass, publish `AssessmentPassed` event instead of calling `ProgressService.markLessonCompleted()` |
| 5.4 | Replace EnrollmentRepository read with API call | Call Enrollment API (still in monolith) to verify active enrollment |
| 5.5 | Expose REST API | Submit answers, view grades, manage assessments (instructor) |
| 5.6 | API Gateway routing | Route `/assessment/*` to new service |
| 5.7 | Decommission | Remove assessment code from monolith |

### API Gateway Routing

| Route Pattern | Target | Method | Auth |
|---|---|---|---|
| `/assessment/{id}` | Assessment Service | GET | Authenticated |
| `/assessment/lesson/{lessonId}` | Assessment Service | GET | Authenticated |
| `/assessment/{id}/submit` | Assessment Service | POST | ROLE_STUDENT |
| `/assessment/{id}/answers` | Assessment Service | GET | Authenticated |
| `/assessment` | Assessment Service | POST | ROLE_INSTRUCTOR |
| `/assessment/{id}` | Assessment Service | PUT | ROLE_INSTRUCTOR |

### Validation Criteria

- [ ] Quiz submission, auto-grading, and manual grading work end-to-end
- [ ] `AssessmentPassed` event correctly consumed by Progress (still in monolith)
- [ ] JSON questions/answers render correctly
- [ ] Student cannot submit assessments for courses they're not enrolled in

---

## 8. Phase 6 — Payment Service (Weeks 13–16)

### Rationale

Payment contains the most **critical anti-pattern**: Stripe API calls inside `@Transactional` boundaries. Extraction isolates the external payment gateway dependency, enables async webhook-based payment confirmation, and prepares the payment leg of the enrollment saga.

### Current State

| Aspect | Detail |
|---|---|
| Entities | Payment |
| Tables | `payments` |
| Coupling | 1 outgoing write (Enrollment status), 1 incoming caller (EnrollmentService) |
| Anti-patterns | Stripe API inside `@Transactional`; cross-domain write to `enrollments.status` |

### Target Architecture

```
  REST API                    ┌──────────────────────┐
  /payment/* ────────────────→│ Payment Service       │──→ Stripe API
                              │ (Container Apps)      │←── Stripe Webhooks
  StudentEnrolled event ─────→│                       │
  (Service Bus)               └──────────┬────────────┘
                                         │
                              ┌──────────┴────────────┐
                              │ payments DB (own PgSQL)│
                              └───────────────────────┘
                                         │
                              Events:
                              ├── PaymentCompleted → Enrollment, Notification
                              ├── PaymentFailed → Enrollment, Notification
                              └── PaymentRefunded → Enrollment, Notification
```

### Steps

| # | Task | Details |
|---|---|---|
| 6.1 | Create microservice | Spring Boot 3.x with Stripe SDK |
| 6.2 | Migrate `payments` table | Copy schema; `enrollment_id` becomes soft reference |
| 6.3 | Implement async payment flow | Create payment intent → return pending → Stripe webhook confirms/denies → publish event |
| 6.4 | Subscribe to `StudentEnrolled` event | Initiate payment processing when enrollment is created (saga step) |
| 6.5 | Publish payment result events | `PaymentCompleted`, `PaymentFailed`, `PaymentRefunded` |
| 6.6 | Remove cross-domain write | Stop writing directly to `enrollments.status`; emit events instead |
| 6.7 | Implement Stripe webhook endpoint | Secure webhook with signature verification |
| 6.8 | Expose REST API | Payment history, refund management |
| 6.9 | API Gateway routing | Route `/payment/*` to new service |
| 6.10 | Decommission | Remove payment code from monolith |

### API Gateway Routing

| Route Pattern | Target | Method | Auth |
|---|---|---|---|
| `/payment/enrollment/{id}` | Payment Service | GET | Authenticated |
| `/payment/{id}` | Payment Service | GET | Authenticated |
| `/payment/{id}/refund` | Payment Service | POST | ROLE_ADMIN |
| `/payment/webhook/stripe` | Payment Service | POST | Stripe Signature |
| `/payment/history` | Payment Service | GET | Authenticated |

### Validation Criteria

- [ ] Stripe payment intent created successfully
- [ ] Webhook correctly updates payment status
- [ ] `PaymentCompleted` event triggers enrollment activation in monolith
- [ ] `PaymentRefunded` event triggers enrollment cancellation in monolith
- [ ] No `@Transactional` wrapping Stripe API calls
- [ ] Idempotent payment processing (Stripe idempotency key)
- [ ] Refund flow works end-to-end

---

## 9. Phase 7 — Student Progress Service (Weeks 16–18)

### Rationale

Student Progress has two callers (EnrollmentService and AssessmentService, both now event-driven), reads Course structure (available via Course Catalog API), and writes to Enrollment (`progress_percent`). This cross-domain write becomes a `ProgressUpdated` event.

### Current State

| Aspect | Detail |
|---|---|
| Entities | Progress |
| Tables | `student_progress` |
| Coupling | 3 outgoing (writes Enrollment, reads Course structure), 2 incoming callers |
| Notes | Traverses Course → Module → Lesson to compute completion percentage |

### Target Architecture

```
  AssessmentPassed event ───→ ┌─────────────────────┐
  EnrollmentActivated event ─→│ Student Progress     │
                              │ Service              │
  REST API                    │ (Container Apps)     │──→ ProgressUpdated event
  /progress/* ───────────────→│                      │
                              └──────────┬───────────┘
                                         │
                              ┌──────────┴───────────┐
                              │ student_progress DB  │
                              │ + cached course      │
                              │   structure          │
                              └──────────────────────┘
```

### Steps

| # | Task | Details |
|---|---|---|
| 7.1 | Create microservice | Spring Boot 3.x |
| 7.2 | Migrate `student_progress` table | Copy schema; `enrollment_id`, `lesson_id`, `student_id` become soft references |
| 7.3 | Cache course structure locally | Subscribe to `CoursePublished` events; call Course Catalog API for module/lesson tree |
| 7.4 | Subscribe to `EnrollmentActivated` | Initialize progress tracking for new enrollment |
| 7.5 | Subscribe to `AssessmentPassed` | Mark lesson as completed, recalculate progress percentage |
| 7.6 | Publish `ProgressUpdated` event | Consumed by Enrollment Service to update `progress_percent` |
| 7.7 | Publish `LessonCompleted` event | For downstream consumers |
| 7.8 | Remove cross-domain write to Enrollment | `progress_percent` and `last_accessed` updated via events |
| 7.9 | Expose REST API | Progress dashboard, lesson completion status |
| 7.10 | API Gateway routing | Route `/progress/*` to new service |
| 7.11 | Decommission | Remove progress code from monolith |

### API Gateway Routing

| Route Pattern | Target | Method | Auth |
|---|---|---|---|
| `/progress/enrollment/{id}` | Student Progress Service | GET | Authenticated |
| `/progress/lesson/{id}/complete` | Student Progress Service | POST | ROLE_STUDENT |
| `/progress/student/{id}` | Student Progress Service | GET | Authenticated |
| `/progress/dashboard` | Student Progress Service | GET | Authenticated |

### Validation Criteria

- [ ] Progress percentage correctly calculated from course structure
- [ ] `ProgressUpdated` event consumed by Enrollment
- [ ] Cached course structure stays in sync with Course Catalog
- [ ] Progress tracking initializes correctly on new enrollment
- [ ] 100% completion triggers `EnrollmentCompleted` event from Enrollment

---

## 10. Phase 8 — Enrollment Service (Weeks 18–20)

### Rationale

Enrollment is extracted **last** because it is the **central orchestrator** with 5 outgoing dependencies and 3 incoming writers. By this phase, all dependencies are event-driven, and the monolithic `enrollStudent()` transaction becomes a choreography saga.

### Current State

| Aspect | Detail |
|---|---|
| Entities | Enrollment |
| Tables | `enrollments` |
| Coupling | 5 outgoing (4 services + 1 repo), 3 incoming writers |
| Anti-patterns | Distributed transaction in `enrollStudent()` spanning payment, progress init, and notification |

### Target Architecture

```
  REST API                         ┌──────────────────────┐
  /enroll/*, /student/* ──────────→│ Enrollment Service    │
                                   │ (Container Apps)      │
  PaymentCompleted event ─────────→│                       │──→ StudentEnrolled event
  PaymentFailed event ────────────→│ SAGA COORDINATOR      │──→ EnrollmentActivated event
  ProgressUpdated event ──────────→│                       │──→ EnrollmentCompleted event
  CertificateIssued event ────────→│                       │
                                   └──────────┬────────────┘
                                              │
                                   ┌──────────┴────────────┐
                                   │ enrollments DB        │
                                   │ (own PostgreSQL)      │
                                   └───────────────────────┘
```

### Steps

| # | Task | Details |
|---|---|---|
| 8.1 | Create microservice | Spring Boot 3.x with saga state machine |
| 8.2 | Migrate `enrollments` table | Copy schema; `course_id` and `student_id` become soft references |
| 8.3 | Implement enrollment saga | See [Section 11](#11-saga-pattern-enrollment-payment-flow) for full saga definition |
| 8.4 | Subscribe to payment events | `PaymentCompleted` → activate, `PaymentFailed` → cancel |
| 8.5 | Subscribe to progress events | `ProgressUpdated` → update `progress_percent`; trigger completion at 100% |
| 8.6 | Subscribe to certificate events | `CertificateIssued` → update enrollment with certificate reference |
| 8.7 | Migrate scheduler jobs | `EnrollmentReminderJob` → cron in Enrollment Service, publishes `ReminderNeeded` events |
| 8.8 | Expose REST API | Enrollment CRUD, student dashboard, admin/instructor views |
| 8.9 | API Gateway routing | Route `/enroll/*`, `/student/*` to new service |
| 8.10 | Decommission monolith | Fully decommission remaining monolith code |

### API Gateway Routing

| Route Pattern | Target | Method | Auth |
|---|---|---|---|
| `/enroll` | Enrollment Service | POST | ROLE_STUDENT |
| `/enroll/{id}` | Enrollment Service | GET | Authenticated |
| `/enroll/{id}/drop` | Enrollment Service | POST | Authenticated |
| `/enroll/student/{id}` | Enrollment Service | GET | Authenticated |
| `/enroll/course/{id}` | Enrollment Service | GET | ROLE_INSTRUCTOR |
| `/student/dashboard` | Enrollment Service | GET | ROLE_STUDENT |
| `/admin/enrollments` | Enrollment Service | GET | ROLE_ADMIN |

### Validation Criteria

- [ ] Full enrollment saga works end-to-end (enroll → pay → activate → progress → complete → certify)
- [ ] Compensating transactions work (payment failure → enrollment cancelled)
- [ ] Scheduler job runs correctly in new service
- [ ] Admin and instructor views render correctly
- [ ] Monolith fully decommissioned; no remaining WAR deployment

---

## 11. Saga Pattern: Enrollment-Payment Flow

### Overview

The monolithic `enrollStudent()` method currently executes payment processing, progress initialization, and notification sending in a single `@Transactional` block. This becomes a **choreography-based saga** where each service reacts to events and publishes results.

### Saga State Machine

```
                          ┌─────────────────────────────────────────────────────┐
                          │               ENROLLMENT SAGA                       │
                          └─────────────────────────────────────────────────────┘

  Student clicks "Enroll"
          │
          ▼
  ┌──────────────┐   StudentEnrolled    ┌──────────────┐
  │  ENROLLMENT  │ ────────────────────→│   PAYMENT    │
  │  PENDING     │                      │   SERVICE    │
  └──────┬───────┘                      └──────┬───────┘
         │                                     │
         │                          ┌──────────┼──────────┐
         │                          │          │          │
         │                          ▼          ▼          ▼
         │                   PaymentCompleted  PaymentFailed  (Timeout 15min)
         │                          │          │          │
         ▼                          ▼          ▼          ▼
  ┌──────────────┐           ┌────────────┐  ┌──────────┐  ┌───────────┐
  │  AWAITING    │──────────→│  ACTIVE    │  │ CANCELLED│  │  EXPIRED  │
  │  PAYMENT     │           └─────┬──────┘  └──────────┘  └───────────┘
  └──────────────┘                 │
                                   │  EnrollmentActivated
                          ┌────────┼────────┐
                          ▼        ▼        ▼
                    ┌──────────┐ ┌────────┐ ┌──────────────┐
                    │ Progress │ │ Notif  │ │ (Student     │
                    │ Service  │ │Service │ │  Dashboard)  │
                    │ (init)   │ │(email) │ │              │
                    └────┬─────┘ └────────┘ └──────────────┘
                         │
                         │  ProgressUpdated (100%)
                         ▼
                    ┌──────────────┐  EnrollmentCompleted   ┌──────────────┐
                    │  COMPLETED   │───────────────────────→│ Certificate  │
                    │              │                        │ Service      │
                    └──────────────┘                        └──────┬───────┘
                                                                   │
                                                            CertificateIssued
                                                                   │
                                                                   ▼
                                                           ┌──────────────┐
                                                           │ Notification │
                                                           │ Service      │
                                                           └──────────────┘
```

### Saga Steps

| Step | Trigger Event | Action | Success Event | Compensating Action |
|---|---|---|---|---|
| 1. Create enrollment | User POST `/enroll` | Create enrollment record with status `PENDING` | `StudentEnrolled` | — |
| 2. Process payment | `StudentEnrolled` | Payment Service creates Stripe PaymentIntent | `PaymentCompleted` | Mark payment as FAILED |
| 3. Activate enrollment | `PaymentCompleted` | Update enrollment status to `ACTIVE` | `EnrollmentActivated` | Refund payment, cancel enrollment |
| 4. Initialize progress | `EnrollmentActivated` | Create progress tracking entries for all lessons | `ProgressInitialized` | Delete progress records |
| 5. Send confirmation | `EnrollmentActivated` | Send enrollment confirmation email | — | — (best effort) |
| 6. Track progress | `LessonCompleted` / `AssessmentPassed` | Update completion percentage | `ProgressUpdated` | — |
| 7. Complete enrollment | `ProgressUpdated` (100%) | Mark enrollment as `COMPLETED` | `EnrollmentCompleted` | — |
| 8. Generate certificate | `EnrollmentCompleted` | Generate PDF certificate | `CertificateIssued` | — (retry) |

### Compensating Transactions

| Failure Scenario | Compensation |
|---|---|
| **Payment fails** | Enrollment status → `CANCELLED`; student notified of failure |
| **Payment times out (15 min)** | Enrollment status → `EXPIRED`; payment intent cancelled in Stripe |
| **Progress init fails** | Retry 3x; if persistent, alert ops; enrollment stays `ACTIVE` (progress can be initialized later) |
| **Certificate generation fails** | Retry 3x with exponential backoff; dead-letter for manual review; enrollment stays `COMPLETED` |

### Idempotency

Every saga step must be **idempotent**:

- **Event deduplication**: Each event carries a unique `eventId`. Consumers track processed event IDs to prevent duplicate processing.
- **Enrollment state guard**: State transitions only occur if the current state is valid (e.g., `PENDING` → `ACTIVE` but not `CANCELLED` → `ACTIVE`).
- **Stripe idempotency key**: Payment intents use `enrollment_id` as the Stripe idempotency key.

### Timeout & Dead Letter Handling

| Scenario | Timeout | Action |
|---|---|---|
| Payment not completed | 15 minutes | Expire enrollment, cancel Stripe PaymentIntent |
| Event processing fails | 3 retries (exponential backoff) | Move to dead-letter queue |
| Dead-letter accumulation | N/A | Alert operations team; manual review dashboard |

---

## 12. API Gateway Routing Rules

### Azure API Management Configuration

The API Gateway is the single entry point for all client traffic. During migration, it routes requests to either the monolith or the appropriate microservice based on URL patterns and feature flags.

### Master Routing Table

| Phase | Route Pattern | Target Service | Methods | Auth Required | Feature Flag |
|---|---|---|---|---|---|
| — | `/auth/*` | Identity / Monolith | ALL | Public | — |
| — | `/home` | Monolith → Course Catalog (Phase 4) | GET | Public | — |
| 2 | `/video/*` | Video Service | ALL | Yes | `ff-video-service` |
| 3 | `/certificate/*` | Certificate Service | ALL | Mixed | `ff-certificate-service` |
| 4 | `/catalog/*` | Course Catalog Service | GET | Public | `ff-course-catalog` |
| 4 | `/course/*` | Course Catalog Service | ALL | Mixed | `ff-course-catalog` |
| 5 | `/assessment/*` | Assessment Service | ALL | Yes | `ff-assessment-service` |
| 6 | `/payment/*` | Payment Service | ALL | Mixed | `ff-payment-service` |
| 7 | `/progress/*` | Student Progress Service | ALL | Yes | `ff-progress-service` |
| 8 | `/enroll/*` | Enrollment Service | ALL | Yes | `ff-enrollment-service` |
| 8 | `/student/*` | Enrollment Service | ALL | Yes | `ff-enrollment-service` |
| 8 | `/admin/enrollments` | Enrollment Service | GET | ROLE_ADMIN | `ff-enrollment-service` |
| 8 | `/admin/*` | Monolith (remaining) | ALL | ROLE_ADMIN | — |
| 8 | `/instructor/*` | Monolith (remaining) | ALL | ROLE_INSTRUCTOR | — |

### Routing Policies

#### Default Route (Monolith Fallback)

Any route not matching the patterns above is forwarded to the monolith. As services are extracted, routes are moved from the monolith to the new service.

```xml
<!-- Azure API Management Policy: Default backend -->
<set-backend-service base-url="https://monolith.internal.eduverse.io" />
```

#### Feature Flag Routing

Each extracted service route is gated by a feature flag. When the flag is disabled, traffic falls back to the monolith.

```xml
<!-- Conditional routing based on feature flag -->
<choose>
    <when condition="@(context.Variables.GetValueOrDefault<bool>("ff-video-service"))">
        <set-backend-service base-url="https://video-service.internal.eduverse.io" />
    </when>
    <otherwise>
        <set-backend-service base-url="https://monolith.internal.eduverse.io" />
    </otherwise>
</choose>
```

#### Cross-Cutting Policies

| Policy | Description |
|---|---|
| **JWT Validation** | Validate JWT token on all authenticated routes; extract `userId`, `roles` into context |
| **Correlation ID** | Inject `X-Correlation-ID` header if not present; propagate on all downstream calls |
| **Rate Limiting** | 100 req/min per user (adjustable per route) |
| **CORS** | Allow configured origins for browser-based clients |
| **Request Logging** | Log request/response metadata to Application Insights |
| **Circuit Breaker** | 5 failures in 30s → open circuit for 60s → fallback to monolith |
| **Retry** | 2 retries with 500ms backoff on 5xx from microservices |

### Internal Service-to-Service Communication

Microservices communicate with each other through two channels:

1. **Azure Service Bus** (async, event-driven): All domain events flow through topic/subscription messaging.
2. **Internal REST APIs** (sync, when needed): Service-to-service calls use internal URLs (not through the API Gateway). These are authenticated via managed identity.

| Calling Service | Target Service | Method | Purpose |
|---|---|---|---|
| Assessment | Enrollment (monolith/service) | GET `/internal/enrollment/{id}` | Verify active enrollment |
| Certificate | Course Catalog | GET `/internal/course/{id}` | Course title for PDF |
| Certificate | Identity API | GET `/internal/user/{id}` | Student name for PDF |
| Progress | Course Catalog | GET `/internal/course/{id}/structure` | Module/lesson tree for % calculation |

---

## 13. Risk Assessment

### Risk Matrix

| # | Risk | Likelihood | Impact | Phase | Mitigation |
|---|---|---|---|---|---|
| R1 | **Data inconsistency during enrollment saga** | High | High | 8 | Compensating transactions; dead-letter queues; idempotent consumers; saga state audit log |
| R2 | **Stripe payment webhook delivery failure** | Medium | High | 6 | Implement webhook retry logic; reconciliation job polls Stripe API every 5 minutes; dead-letter alerting |
| R3 | **Increased latency from sync API calls** | Medium | Medium | 4–8 | Cache course structure locally; use CQRS read models; set aggressive timeouts with circuit breakers |
| R4 | **Event ordering issues** | Medium | Medium | All | Use Service Bus sessions (ordered delivery) for same-entity events; design consumers to handle out-of-order events |
| R5 | **Data migration errors** | Medium | High | All | Run dual-write during transition; validate row counts and checksums; maintain rollback scripts |
| R6 | **Certificate PDF generation timeout** | Medium | Low | 3 | Async generation with download-when-ready pattern; auto-scaling workers; retry with backoff |
| R7 | **Video processing backlog** | Low | Medium | 2 | Auto-scaling worker containers; queue-based load leveling; priority queues for re-uploads |
| R8 | **Reporting jobs break across services** | High | Low | All | Event-sourced reporting via CDC or dedicated read store; Azure Data Explorer for analytics |
| R9 | **Team unfamiliarity with event-driven patterns** | Medium | Medium | 1–2 | Start with simplest extraction (Notification); invest in training; document patterns and runbooks |
| R10 | **Service Bus message loss** | Low | High | All | Premium tier with geo-disaster recovery; duplicate detection; at-least-once delivery with idempotent consumers |
| R11 | **Monolith becomes harder to maintain during migration** | High | Medium | All | Feature flags for instant rollback; maintain monolith CI/CD; limit monolith code changes to event publishing |
| R12 | **Identity/auth inconsistency across services** | Medium | High | All | Centralized JWT issuance at API Gateway; token validation libraries shared across services; short token TTL |

### Risk Response Plan

#### High Impact Risks

**R1 — Saga Data Inconsistency**
- **Prevention**: Extensive integration testing of all saga paths (happy path + every failure scenario)
- **Detection**: Saga state audit log; monitoring for enrollments stuck in `PENDING` or `AWAITING_PAYMENT` > 30 minutes
- **Recovery**: Admin dashboard to manually complete/cancel stuck enrollments; reconciliation job runs hourly

**R2 — Stripe Webhook Failure**
- **Prevention**: Multiple webhook endpoints (primary + failover); Stripe retry configuration
- **Detection**: Reconciliation job compares Stripe payment intents with local payment records every 5 minutes
- **Recovery**: Manual payment confirmation via admin API; automatic retry from reconciliation job

**R5 — Data Migration Errors**
- **Prevention**: Dry-run migrations in staging; row count validation; checksum comparison; dual-write period
- **Detection**: Automated data integrity checks post-migration
- **Recovery**: Rollback scripts prepared for each phase; feature flag instant rollback to monolith

**R12 — Auth Inconsistency**
- **Prevention**: Single JWT issuer; shared token validation library; integration tests for all auth flows
- **Detection**: 401/403 error rate monitoring per service
- **Recovery**: Fallback to monolith auth via feature flag

---

## 14. Rollback Strategy

### Per-Phase Rollback

Every phase includes a rollback plan that can be executed within **15 minutes**:

| Trigger | Action | Recovery Time |
|---|---|---|
| Feature flag disabled | API Gateway routes traffic back to monolith | < 1 minute |
| Microservice unhealthy | Circuit breaker triggers monolith fallback | Automatic (< 30 seconds) |
| Data inconsistency detected | Disable feature flag + restore monolith DB from replication | < 15 minutes |

### Rollback Prerequisites

1. **Monolith code is NOT removed** until the new service has been stable for 2+ weeks in production.
2. **Dual-write period**: During transition, both monolith and microservice write to their respective databases. A sync job ensures consistency.
3. **Database rollback scripts**: For each phase, maintain a script that migrates data back to the monolith database.

### Point of No Return

The monolith is fully decommissioned only after **all 8 phases** are complete and have been stable in production for at least **4 weeks**. Until then, the monolith remains deployable as a fallback.

---

## 15. Success Criteria & Observability

### Success Criteria per Phase

| Criteria | Target |
|---|---|
| Zero-downtime deployment | All traffic routed without service interruption |
| Error rate | < 0.1% increase compared to monolith baseline |
| Latency | P95 latency within 20% of monolith baseline |
| Data consistency | Zero data loss; dual-write validation passes |
| Feature parity | All user-facing features work identically |

### Observability Stack

| Component | Tool | Purpose |
|---|---|---|
| Distributed Tracing | Application Insights | End-to-end request tracing across services |
| Metrics | Azure Monitor | CPU, memory, request rate, error rate, queue depth |
| Logging | Azure Monitor Logs (Log Analytics) | Centralized structured logging with correlation IDs |
| Alerting | Azure Monitor Alerts | SLA violations, error spikes, dead-letter accumulation |
| Dashboards | Azure Workbooks / Grafana | Real-time service health, saga state distribution, event throughput |

### Key Metrics to Monitor

| Metric | Alert Threshold | Action |
|---|---|---|
| Service Bus dead-letter queue depth | > 10 messages | Investigate consumer failures |
| Saga enrollments in PENDING > 30 min | > 5 enrollments | Check payment service health |
| API Gateway 5xx rate | > 1% | Trigger circuit breaker; investigate service |
| Event processing latency (P95) | > 5 seconds | Scale consumers; investigate bottleneck |
| Cross-service API call latency (P95) | > 500ms | Review caching; check network |
| Database connection pool utilization | > 80% | Scale database; optimize queries |

---

## Appendix A: Technology Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Runtime | Java 17 / Spring Boot 3.x | Team familiarity; ecosystem; migration path from Java 11 |
| Containers | Azure Container Apps | Serverless containers; auto-scaling; Service Bus integration |
| Message Broker | Azure Service Bus Premium | Ordered delivery, dead-letter, geo-DR, large messages |
| API Gateway | Azure API Management | Policy engine, rate limiting, auth, developer portal |
| Database | Azure Database for PostgreSQL Flexible Server (per service) | Managed; familiar; per-service isolation |
| Secrets | Azure Key Vault | Centralized secrets; managed identity access |
| CI/CD | GitHub Actions | Existing repo; container build + deploy workflows |
| Feature Flags | Azure App Configuration | Native Azure integration; gradual rollout support |

## Appendix B: Timeline Summary

```
Week  0: ████ Infrastructure setup (Container Apps, Service Bus, APIM, CI/CD)
Weeks 1–3:  ████████████ Phase 1: Notification Service
Weeks 4–6:  ████████████ Phase 2: Video Service
Weeks 6–8:  ████████████ Phase 3: Certificate Service
Weeks 9–11: ████████████ Phase 4: Course Catalog Service
Weeks 11–13:████████████ Phase 5: Assessment Service
Weeks 13–16:████████████████ Phase 6: Payment Service
Weeks 16–18:████████████ Phase 7: Student Progress Service
Weeks 18–20:████████████ Phase 8: Enrollment Service (Saga)
Week 21+:   ████ Monolith decommission & stabilization
```

> **Total:** ~20 weeks of active development + 4 weeks stabilization before full monolith decommission.
