# EduVerse Academy — Spec2Cloud Bounded Context Analysis

> **Generated:** 2026-04-16 | **Monolith:** Java 11 / Spring MVC 5.3 / Hibernate 5.6 / PostgreSQL 14  
> **Target Architecture:** Azure Container Apps + Azure Service Bus  
> **Decomposition Pattern:** Strangler Fig  

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Monolith Inventory](#2-monolith-inventory)
3. [Bounded Context Definitions](#3-bounded-context-definitions)
4. [Coupling Matrix](#4-coupling-matrix)
5. [Data Ownership Mapping](#5-data-ownership-mapping)
6. [Recommended Extraction Order (Strangler Fig)](#6-recommended-extraction-order-strangler-fig)
7. [Anti-Patterns and Risks](#7-anti-patterns-and-risks)
8. [Event-Driven Decoupling Plan](#8-event-driven-decoupling-plan)

---

## 1. Executive Summary

The EduVerse Academy monolith consists of **13 JPA entities**, **8 services**, **9 controllers**, **12 repositories**, and **2 Quartz scheduler jobs**, all deployed as a single WAR on Tomcat 9 against a shared PostgreSQL 14 database.

Analysis identifies **8 bounded contexts** with varying degrees of coupling. The **Notification** context is the least coupled (pure sink) and should be extracted first. The **Enrollment** context is the most tightly coupled orchestration hub — it depends on 4 other services — and should be extracted last.

| Metric | Count |
|---|---|
| JPA Entities | 13 |
| Database Tables | 13 |
| Service Classes | 8 |
| Controllers | 9 |
| Repositories | 12 |
| Scheduler Jobs | 2 |
| Cross-service dependencies | 7 direct call paths |
| Cross-domain repository access | 9 instances |

---

## 2. Monolith Inventory

### 2.1 Entity → Table Mapping

| Entity | Table | Primary Key | Key Status/Enum Fields |
|---|---|---|---|
| User | `users` | id (auto) | enabled (boolean) |
| Authority | `authorities` | id (auto) | authority (ROLE_*) |
| Course | `courses` | id (auto) | status: DRAFT, PUBLISHED, ARCHIVED |
| Module | `modules` | id (auto) | sort_order |
| Lesson | `lessons` | id (auto) | content_type: VIDEO, DOCUMENT, QUIZ, ASSIGNMENT |
| Assessment | `assessments` | id (auto) | type: QUIZ, EXAM, ASSIGNMENT, PROJECT |
| StudentAnswer | `student_answers` | id (auto) | score, graded_by |
| Video | `videos` | id (auto) | status: UPLOADING, PROCESSING, READY, FAILED |
| Enrollment | `enrollments` | id (auto) | status: ACTIVE, COMPLETED, DROPPED, EXPIRED |
| Progress | `student_progress` | id (auto) | completed (boolean) |
| Payment | `payments` | id (auto) | status: PENDING, COMPLETED, FAILED, REFUNDED |
| Certificate | `certificates` | id (auto) | certificate_number (unique) |
| Notification | `notifications` | id (auto) | type: EMAIL, IN_APP, SMS; sent (boolean) |

### 2.2 Service → Repository → Controller Mapping

| Service | Own Repositories | Cross-Domain Repos | Called By Controllers |
|---|---|---|---|
| CourseService | Course, Module | — | CourseController, HomeController, AdminController, InstructorController, EnrollmentController |
| EnrollmentService | Enrollment | Course | EnrollmentController, AdminController, InstructorController, PaymentController |
| AssessmentService | Assessment, StudentAnswer | Enrollment | AssessmentController |
| VideoService | Video | Lesson | VideoController |
| CertificateService | Certificate | Enrollment, Course, User | CertificateController |
| PaymentService | Payment | Enrollment | PaymentController |
| NotificationService | Notification | User | *(called by other services, not directly by controllers)* |
| ProgressService | Progress | Enrollment, Module, Lesson | *(called by other services, not directly by controllers)* |

### 2.3 Service-to-Service Call Graph

```
CourseService ──────────────────────→ NotificationService
                                           ▲
EnrollmentService ──→ PaymentService       │
       │                                   │
       ├───────────→ ProgressService       │
       │                                   │
       ├───────────→ CertificateService    │
       │                                   │
       └───────────────────────────────────┘

AssessmentService ──→ ProgressService
```

**EnrollmentService** is the central orchestrator with **4 outgoing service dependencies**.  
**NotificationService** is a pure sink — **0 outgoing**, called by 2 services + 1 scheduler.  
**ProgressService** is a shared utility — called by EnrollmentService + AssessmentService.

---

## 3. Bounded Context Definitions

### BC-1: Course Catalog

| Aspect | Detail |
|---|---|
| **Domain Entities** | Course, Module, Lesson |
| **Services** | CourseService |
| **Controllers** | CourseController, HomeController (partial) |
| **Repositories** | CourseRepository, ModuleRepository, LessonRepository |
| **Database Tables** | `courses`, `modules`, `lessons` |
| **Aggregate Root** | Course (cascade ALL → Module → Lesson) |
| **Outgoing Coupling** | NotificationService (on publish) |
| **Incoming Coupling** | Read by Enrollment, Certificate, Progress, Admin, Instructor contexts |
| **Notes** | Cleanly separated with one-way cascade. The read-heavy nature makes it a good candidate for a read-replica or CQRS read model. The `instructor_id` FK references User but stores only the ID (no JPA relationship), keeping it decoupled. |

### BC-2: Enrollment

| Aspect | Detail |
|---|---|
| **Domain Entities** | Enrollment |
| **Services** | EnrollmentService |
| **Controllers** | EnrollmentController, AdminController (partial), InstructorController (partial) |
| **Repositories** | EnrollmentRepository |
| **Database Tables** | `enrollments` |
| **Aggregate Root** | Enrollment |
| **Outgoing Coupling** | PaymentService, ProgressService, CertificateService, NotificationService, CourseRepository (read) |
| **Incoming Coupling** | PaymentService (writes status), ProgressService (writes progress_percent), Assessment (reads), Certificate (reads), Scheduler jobs |
| **Notes** | **MOST COUPLED CONTEXT.** The `enrollStudent()` method is a distributed transaction spanning payment processing, progress initialization, and notification in a single `@Transactional`. Must be decomposed into a saga. |

### BC-3: Assessment

| Aspect | Detail |
|---|---|
| **Domain Entities** | Assessment, StudentAnswer |
| **Services** | AssessmentService |
| **Controllers** | AssessmentController |
| **Repositories** | AssessmentRepository, StudentAnswerRepository |
| **Database Tables** | `assessments`, `student_answers` |
| **Aggregate Root** | Assessment |
| **Outgoing Coupling** | ProgressService (on pass), EnrollmentRepository (read) |
| **Incoming Coupling** | None (standalone entry point) |
| **Notes** | References Lesson via `lesson_id` FK (no JPA relationship object). The `submitAnswer()` method auto-grades quizzes and triggers progress update — this cross-context call should become an event. The `questions_json` / `answers_json` TEXT columns store quiz data as JSON strings. |

### BC-4: Video

| Aspect | Detail |
|---|---|
| **Domain Entities** | Video |
| **Services** | VideoService |
| **Controllers** | VideoController |
| **Repositories** | VideoRepository |
| **Database Tables** | `videos` |
| **Aggregate Root** | Video |
| **Outgoing Coupling** | LessonRepository (writes video_url to Lesson) |
| **Incoming Coupling** | None |
| **Notes** | Contains a critical anti-pattern: `processVideo()` uses `Thread.sleep(2000)` inside a `@Transactional` method, holding a database connection during simulated transcoding. In microservice form, video processing becomes an async worker with status callbacks. The `transcoded_urls` TEXT field stores multiple quality URLs. |

### BC-5: Certificate

| Aspect | Detail |
|---|---|
| **Domain Entities** | Certificate |
| **Services** | CertificateService |
| **Controllers** | CertificateController |
| **Repositories** | CertificateRepository |
| **Database Tables** | `certificates` |
| **Aggregate Root** | Certificate |
| **Outgoing Coupling** | EnrollmentRepository (read), CourseRepository (read), UserRepository (read) |
| **Incoming Coupling** | Called by EnrollmentService.completeEnrollment() |
| **Notes** | PDF generation via iTextPDF is embedded in the service layer. In microservice form, this becomes an async job triggered by an `EnrollmentCompleted` event. The 1:1 relationship with Enrollment (unique FK) makes data ownership clear. Reads from 3 other contexts are for PDF content population only. |

### BC-6: Payment

| Aspect | Detail |
|---|---|
| **Domain Entities** | Payment |
| **Services** | PaymentService |
| **Controllers** | PaymentController |
| **Repositories** | PaymentRepository |
| **Database Tables** | `payments` |
| **Aggregate Root** | Payment |
| **Outgoing Coupling** | EnrollmentRepository (writes enrollment status on payment/refund) |
| **Incoming Coupling** | Called by EnrollmentService.enrollStudent() |
| **Notes** | **Critical anti-pattern:** Stripe API calls happen inside `@Transactional` boundaries. If Stripe is slow or fails, the database transaction is held open. In microservice form, payment processing should be async with webhook-based confirmation. The `refundPayment()` method also mutates Enrollment status to DROPPED — this cross-domain write must become an event. |

### BC-7: Notification

| Aspect | Detail |
|---|---|
| **Domain Entities** | Notification |
| **Services** | NotificationService |
| **Controllers** | *(none — service-only, called by other services and schedulers)* |
| **Repositories** | NotificationRepository, UserRepository (read) |
| **Database Tables** | `notifications` |
| **Aggregate Root** | Notification |
| **Outgoing Coupling** | UserRepository (read for email address) |
| **Incoming Coupling** | Called by CourseService, EnrollmentService, EnrollmentReminderJob |
| **Notes** | **LEAST COUPLED — PURE EVENT SINK.** Has zero outgoing service calls. Currently uses synchronous SMTP via Apache Commons Email with hardcoded config. Perfect candidate for event-driven architecture: subscribe to domain events and send notifications asynchronously via Azure Service Bus. Supports EMAIL, IN_APP, SMS types. |

### BC-8: Student Progress

| Aspect | Detail |
|---|---|
| **Domain Entities** | Progress |
| **Services** | ProgressService |
| **Controllers** | *(none — service-only, called by EnrollmentService and AssessmentService)* |
| **Repositories** | ProgressRepository |
| **Database Tables** | `student_progress` |
| **Aggregate Root** | Progress |
| **Outgoing Coupling** | EnrollmentRepository (writes progress_percent, last_accessed), ModuleRepository (read), LessonRepository (read) |
| **Incoming Coupling** | Called by EnrollmentService.updateProgress(), AssessmentService.submitAnswer() |
| **Notes** | Progress calculation traverses Course → Module → Lesson structure to compute completion percentage. In microservice form, it should cache course structure locally or receive it via events. The cross-domain write to Enrollment (progress_percent) must become an event (`ProgressUpdated`). |

---

## 4. Coupling Matrix

Directional dependency matrix. **Rows call Columns.** Numbers indicate coupling types:  
- **S** = Service-to-service call  
- **R** = Cross-domain repository read  
- **W** = Cross-domain repository write  
- **E** = Should become async event  

| From ↓ \ To → | Course Catalog | Enrollment | Assessment | Video | Certificate | Payment | Notification | Student Progress |
|---|---|---|---|---|---|---|---|---|
| **Course Catalog** | — | | | | | | S,E | |
| **Enrollment** | R | — | | | S,E | S,E | S,E | S,E |
| **Assessment** | | R | — | | | | | S,E |
| **Video** | W | | | — | | | | |
| **Certificate** | R | R | | | — | | | |
| **Payment** | | W | | | | — | | |
| **Student Progress** | R | W | | | | | | — |

### Coupling Score Summary

| Bounded Context | Outgoing Deps | Incoming Deps | Total Coupling | Coupling Rank |
|---|---|---|---|---|
| **Notification** | 0 (reads User) | 3 callers | 3 | 🟢 Lowest |
| **Video** | 1 (writes Lesson) | 0 callers | 1 | 🟢 Low |
| **Certificate** | 3 (reads only) | 1 caller | 4 | 🟡 Medium-Low |
| **Course Catalog** | 1 (notif on publish) | 4 readers | 5 | 🟡 Medium |
| **Assessment** | 2 (progress + enrollment read) | 0 callers | 2 | 🟡 Medium |
| **Payment** | 1 (writes enrollment) | 1 caller | 2 | 🟡 Medium |
| **Student Progress** | 3 (writes enrollment, reads course) | 2 callers | 5 | 🟠 High |
| **Enrollment** | 5 (4 services + 1 repo) | 3 writers | 8 | 🔴 Highest |

---

## 5. Data Ownership Mapping

Each bounded context **owns** its tables exclusively. Cross-context data access must go through APIs or events.

| Bounded Context | Owned Tables | Shared Read Columns | Cross-Context FK |
|---|---|---|---|
| **Course Catalog** | `courses`, `modules`, `lessons` | `courses.instructor_id` (→ User) | `modules.course_id`, `lessons.module_id` |
| **Enrollment** | `enrollments` | `enrollments.student_id` (→ User), `enrollments.course_id` (→ Course) | — |
| **Assessment** | `assessments`, `student_answers` | `assessments.lesson_id` (→ Lesson), `student_answers.student_id` (→ User) | — |
| **Video** | `videos` | `videos.lesson_id` (→ Lesson) | — |
| **Certificate** | `certificates` | `certificates.enrollment_id` (→ Enrollment) | — |
| **Payment** | `payments` | `payments.enrollment_id` (→ Enrollment) | — |
| **Notification** | `notifications` | `notifications.user_id` (→ User) | — |
| **Student Progress** | `student_progress` | `progress.student_id` (→ User), `progress.lesson_id` (→ Lesson), `progress.enrollment_id` (→ Enrollment) | — |
| **Identity (shared kernel)** | `users`, `authorities` | *(shared across all contexts)* | — |

### Data Ownership Rules for Microservices

1. **Each microservice owns its tables** — no direct cross-service SQL joins.
2. **Cross-context FKs become soft references** — store the ID but no database-level FK constraint.
3. **User/Identity is a shared kernel** — extracted as a shared authentication service (or kept as JWT claims).
4. **Read-heavy cross-context data** (e.g., course title for certificate) should be **denormalized** via events or cached locally.

---

## 6. Recommended Extraction Order (Strangler Fig)

The Strangler Fig pattern extracts services incrementally, routing traffic between monolith and new microservice via an API gateway. **Extract least-coupled contexts first** to minimize risk and build team confidence.

### Phase 1 — Event Infrastructure + Pure Sinks (Weeks 1–3)

| # | Microservice | Rationale | Strangler Approach |
|---|---|---|---|
| **1** | **Notification Service** | Zero outgoing service deps. Pure event consumer. No controllers to re-route — only internal calls. | Replace synchronous `NotificationService` calls with Azure Service Bus events. Monolith publishes events; new service consumes them. No API gateway routing needed. |

### Phase 2 — Isolated Leaf Services (Weeks 4–8)

| # | Microservice | Rationale | Strangler Approach |
|---|---|---|---|
| **2** | **Video Service** | Zero incoming callers. Single outgoing write (Lesson.video_url). Self-contained upload/transcode/stream pipeline. | Route `/video/*` endpoints to new service. Replace synchronous `processVideo()` with async worker. Emit `VideoReady` event to update Lesson in monolith. |
| **3** | **Certificate Service** | One incoming caller (EnrollmentService). All outgoing deps are reads. PDF generation is an isolated concern. | Route `/certificate/*` endpoints to new service. Trigger via `EnrollmentCompleted` event from Service Bus. Read course/user data via API calls or cached snapshots. |

### Phase 3 — Core Domain Services (Weeks 9–14)

| # | Microservice | Rationale | Strangler Approach |
|---|---|---|---|
| **4** | **Course Catalog Service** | Stable, read-heavy domain. Single outgoing dep (notification on publish) already handled by events. Many incoming readers — provide a read API. | Route `/catalog`, `/course/*` endpoints to new service. Other services call Course API instead of direct DB. Emit `CoursePublished` event. |
| **5** | **Assessment Service** | Two outgoing deps (progress + enrollment read). After Progress is event-driven, Assessment can emit `AssessmentPassed` events. | Route `/assessment/*` endpoints to new service. Emit `AssessmentPassed` event (consumed by Progress service). Read enrollment via API. |
| **6** | **Payment Service** | One cross-domain write (enrollment status). Stripe integration benefits from isolation. | Route `/payment/*` endpoints to new service. Emit `PaymentCompleted` / `PaymentRefunded` events. Enrollment service reacts to update status. |

### Phase 4 — Coupled Core (Weeks 15–20)

| # | Microservice | Rationale | Strangler Approach |
|---|---|---|---|
| **7** | **Student Progress Service** | Writes to Enrollment, reads Course structure. After Course and Enrollment are evented, Progress can work with cached data and emit `ProgressUpdated` events. | No direct controller routes (internal only). Expose REST API for progress tracking. Consume `LessonCompleted`, `AssessmentPassed` events. Emit `ProgressUpdated` events consumed by Enrollment. |
| **8** | **Enrollment Service** | **EXTRACT LAST.** Central orchestrator with 5 dependencies. By this phase, all deps are event-driven. The monolithic `enrollStudent()` transaction becomes a saga. | Route `/enroll/*`, `/student/*` endpoints. Implement choreography saga: `EnrollmentCreated` → Payment → `PaymentCompleted` → Enrollment activated. Subscribe to `ProgressUpdated`, `CertificateIssued` events. |

### Extraction Dependency Graph

```
Phase 1:  [Notification] ──────────────────────────────────────────────┐
                                                                       │
Phase 2:  [Video] ──────── [Certificate] ─────────────────────────────│
                                                                       │
Phase 3:  [Course Catalog] ──── [Assessment] ──── [Payment] ─────────│
                                                                       │
Phase 4:  [Student Progress] ──── [Enrollment] ◄──────────────────────┘
                                       ▲              (all events flow here)
                                  LAST - Saga
```

### Scheduler Job Decomposition

| Job | Current Location | Target Location |
|---|---|---|
| EnrollmentReminderJob | Monolith (Quartz) | Enrollment Service (cron job) — emits `ReminderNeeded` events consumed by Notification Service |
| ReportGenerationJob | Monolith (Quartz) | Dedicated Reporting Service or Azure Function — reads from event-sourced projections / read replicas |

---

## 7. Anti-Patterns and Risks

### Critical Anti-Patterns Found

| # | Anti-Pattern | Location | Impact | Remediation |
|---|---|---|---|---|
| 1 | **Distributed Transaction in `enrollStudent()`** | EnrollmentService | Payment + enrollment + notification in one `@Transactional` | Saga pattern (choreography via Service Bus) |
| 2 | **Stripe API inside `@Transactional`** | PaymentService.processPayment() | DB connection held during external HTTP call | Async payment with webhook confirmation |
| 3 | **Thread.sleep inside `@Transactional`** | VideoService.processVideo() | DB connection held for 2+ seconds | Async worker with status polling |
| 4 | **Cross-domain writes** | PaymentService → Enrollment status, ProgressService → Enrollment progress_percent | Dual ownership of `enrollments` table | Event-driven: emit events, let owner update |
| 5 | **N+1 queries** | AdminController, InstructorController | Loops through courses, queries enrollments per course | Batch queries or CQRS read model |
| 6 | **In-memory filtering** | EnrollmentReminderJob, ReportGenerationJob | Loads all rows, filters in Java | Push filtering to SQL queries |
| 7 | **Direct repository access in controllers** | AdminController → UserRepository | Bypasses service layer | Route through a UserService |
| 8 | **Hardcoded SMTP config** | NotificationService | No externalized configuration | Environment variables / Azure Key Vault |

### Risk Matrix for Extraction

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Data inconsistency during saga | High | High | Implement compensating transactions; use Service Bus dead-letter queues |
| Increased latency from API calls | Medium | Medium | Cache course structure locally; use CQRS read models |
| Certificate PDF generation timeout | Medium | Low | Async generation with download-when-ready pattern |
| Video processing backlog | Low | Medium | Auto-scaling worker containers on Azure Container Apps |
| Report job breaks across services | High | Low | Event-sourced reporting via change-data-capture or dedicated read store |

---

## 8. Event-Driven Decoupling Plan

### Domain Events (Azure Service Bus Topics)

| Event | Publisher | Subscribers | Payload |
|---|---|---|---|
| `CoursePublished` | Course Catalog | Notification | courseId, title, instructorId |
| `StudentEnrolled` | Enrollment | Payment, Progress, Notification | enrollmentId, studentId, courseId |
| `PaymentCompleted` | Payment | Enrollment, Notification | paymentId, enrollmentId, amount |
| `PaymentRefunded` | Payment | Enrollment, Notification | paymentId, enrollmentId |
| `PaymentFailed` | Payment | Enrollment, Notification | paymentId, enrollmentId, reason |
| `LessonCompleted` | Student Progress | Enrollment | progressId, enrollmentId, lessonId |
| `ProgressUpdated` | Student Progress | Enrollment | enrollmentId, progressPercent |
| `AssessmentPassed` | Assessment | Student Progress, Notification | assessmentId, studentId, enrollmentId, score |
| `EnrollmentCompleted` | Enrollment | Certificate, Notification | enrollmentId, studentId, courseId |
| `CertificateIssued` | Certificate | Notification | certificateId, enrollmentId, pdfUrl |
| `VideoReady` | Video | Course Catalog | videoId, lessonId, transcodedUrls |
| `ReminderNeeded` | Enrollment (cron) | Notification | enrollmentId, studentId, daysSinceAccess |

### Saga: Student Enrollment Flow (Replaces `enrollStudent()`)

```
   ┌──────────┐    StudentEnrolled     ┌──────────┐
   │Enrollment│ ──────────────────────→│ Payment  │
   │ Service  │                        │ Service  │
   └──────────┘                        └────┬─────┘
        ▲                                   │
        │  PaymentCompleted                 │ PaymentCompleted
        │  (activate enrollment)            │
        ├───────────────────────────────────┘
        │
        │  PaymentFailed
        │  (cancel enrollment — compensating action)
        ├───────────────────────────────────┘
        │
        │  EnrollmentActivated
        ├──────────────────→ Progress Service (initialize tracking)
        ├──────────────────→ Notification Service (send confirmation)
        │
        │  [On 100% completion]
        │  ProgressUpdated(100%)
        ├──────────────────→ Certificate Service (generate PDF)
        └──────────────────→ Notification Service (send certificate)
```

---

## Appendix: File Inventory

### Models (13 files)
`Assessment.java`, `Authority.java`, `Certificate.java`, `Course.java`, `Enrollment.java`, `Lesson.java`, `Module.java`, `Notification.java`, `Payment.java`, `Progress.java`, `StudentAnswer.java`, `User.java`, `Video.java`

### Services (8 files)
`AssessmentService.java`, `CertificateService.java`, `CourseService.java`, `EnrollmentService.java`, `NotificationService.java`, `PaymentService.java`, `ProgressService.java`, `VideoService.java`

### Controllers (9 files)
`AdminController.java`, `AssessmentController.java`, `CertificateController.java`, `CourseController.java`, `EnrollmentController.java`, `HomeController.java`, `InstructorController.java`, `PaymentController.java`, `VideoController.java`

### Repositories (12 files)
`AssessmentRepository.java`, `CertificateRepository.java`, `CourseRepository.java`, `EnrollmentRepository.java`, `LessonRepository.java`, `ModuleRepository.java`, `NotificationRepository.java`, `PaymentRepository.java`, `ProgressRepository.java`, `StudentAnswerRepository.java`, `UserRepository.java`, `VideoRepository.java`

### Schedulers (2 files)
`EnrollmentReminderJob.java`, `ReportGenerationJob.java`

### Config (5 files)
`AppConfig.java`, `PersistenceConfig.java`, `SchedulerConfig.java`, `SecurityConfig.java`, `WebAppInitializer.java`
