# EduVerse Academy — Monolithic Application

EduVerse Academy is an online education and course delivery platform built as a **traditional monolithic Spring MVC application**. It handles course management, student enrollment, payment processing, progress tracking, certificate generation, and notifications — all within a single deployable WAR file backed by a shared PostgreSQL database.

> **⚠️ This application is intentionally designed as a tightly coupled monolith.** It serves as the starting point for a modernization lab where participants decompose it into microservices.

---

## Tech Stack

| Layer          | Technology                         |
|----------------|------------------------------------|
| Language       | Java 11                            |
| Web Framework  | Spring MVC 5.3                     |
| Security       | Spring Security 5.8                |
| ORM            | Hibernate 5.6 / Spring Data JPA   |
| Database       | PostgreSQL                         |
| Connection Pool| HikariCP                           |
| View Layer     | JSP + JSTL + Apache Tiles          |
| Scheduling     | Quartz Scheduler                   |
| Payments       | Stripe Java SDK                    |
| Email          | Apache Commons Email (SMTP)        |
| PDF Generation | iText 5                            |
| Build Tool     | Apache Maven                       |
| App Server     | Apache Tomcat 9                    |

---

## Prerequisites

- **JDK 11** (OpenJDK or Oracle)
- **Apache Maven 3.6+**
- **PostgreSQL 12+**
- **Apache Tomcat 9**

---

## Setup Instructions

### 1. Create the Database

```sql
CREATE DATABASE eduverse;
CREATE USER eduverse_user WITH PASSWORD 'eduverse_pass';
GRANT ALL PRIVILEGES ON DATABASE eduverse TO eduverse_user;
```

### 2. Configure the Application

Update the database connection settings in `src/main/java/com/eduverse/config/PersistenceConfig.java` or the corresponding properties file to match your local PostgreSQL instance.

### 3. Build

```bash
mvn clean package
```

This produces `target/eduverse-academy.war`.

### 4. Deploy to Tomcat

Copy the WAR file into your Tomcat `webapps/` directory:

```bash
cp target/eduverse-academy.war $CATALINA_HOME/webapps/
```

Start Tomcat and access the application at `http://localhost:8080/eduverse-academy`.

### 5. Run Tests

```bash
mvn test
```

---

## Default Users

| Role       | Username            | Password     |
|------------|---------------------|--------------|
| Admin      | admin@eduverse.com  | admin123     |
| Instructor | instructor@eduverse.com | teach123 |
| Student    | student@eduverse.com | learn123    |

---

## Architecture Notes

### Monolithic Design

EduVerse Academy is a **single-deployment monolith** where all business domains — courses, enrollments, payments, progress, certificates, and notifications — share:

- **One codebase** compiled into a single WAR
- **One database** with all tables in the same schema
- **One transaction manager** spanning multiple domain boundaries
- **One deployment unit** running on a single Tomcat instance

All service classes live in the same Spring `ApplicationContext` and communicate via direct method calls and `@Autowired` injection. There are no API boundaries, message queues, or domain isolation mechanisms.

### Why This Architecture Exists

This monolith was designed **specifically for a decomposition lab**. It deliberately includes common anti-patterns found in real-world legacy applications so that participants can practice identifying domain boundaries, extracting services, and modernizing the architecture.

---

## Intentional Anti-Patterns

The following anti-patterns are embedded throughout the codebase as learning material:

| Anti-Pattern | Where | Description |
|---|---|---|
| **Cross-domain transactions** | `EnrollmentService.enrollStudent()` | A single `@Transactional` method spans enrollment, payment, and notification domains. Payment failure rolls back the enrollment record. |
| **Service coupling (star topology)** | `EnrollmentService` | Directly `@Autowired` to five other services — `PaymentService`, `ProgressService`, `NotificationService`, `CertificateService`, and `CourseRepository`. |
| **Single shared database** | All repositories | Every domain (courses, enrollments, payments, certificates, notifications) reads and writes to the same PostgreSQL database with no schema isolation. |
| **Synchronous email in transactions** | `CourseService.publishCourse()`, `EnrollmentService.enrollStudent()` | Notification emails are sent synchronously inside business transactions. A slow or failing mail server blocks or breaks unrelated operations. |
| **External API calls in transactions** | `PaymentService.processPayment()` | Stripe API calls happen inside a `@Transactional` method. If the charge succeeds but the DB commit fails, the customer is charged without the enrollment being activated. |
| **Pass-through coupling** | `EnrollmentService.updateProgress()` | Acts as a pure proxy to `ProgressService`, adding an unnecessary layer of indirection while maintaining tight coupling. |
| **No state machine enforcement** | `CourseService.updateCourse()` | Published courses can be mutated without status validation — relies on callers to check state. |
| **Hardcoded configuration** | `NotificationService`, `PaymentService` | SMTP credentials and Stripe API keys are hardcoded as static constants instead of externalized configuration. |
| **God service** | `NotificationService` | Called by virtually every other service, creating a central dependency hub that couples all domains. |
| **Silent failure swallowing** | Multiple services | `catch` blocks log errors but swallow exceptions, causing silent data inconsistencies (e.g., enrollment succeeds but confirmation email never sent). |

---

## Project Structure

```
eduverse-academy/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/eduverse/
│   │   │   ├── config/          # Spring, Security, Persistence, Scheduler config
│   │   │   ├── controller/      # Spring MVC controllers
│   │   │   ├── model/           # JPA entities
│   │   │   ├── repository/      # Spring Data JPA repositories
│   │   │   ├── scheduler/       # Quartz jobs
│   │   │   └── service/         # Business logic services
│   │   ├── resources/           # Application properties, logging config
│   │   └── webapp/              # JSP views, static assets, web.xml
│   └── test/
│       └── java/com/eduverse/
│           └── service/         # Unit tests with Mockito
└── target/                      # Build output
```
