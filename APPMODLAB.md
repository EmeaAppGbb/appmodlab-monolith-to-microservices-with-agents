# From Monolith to Microservices with Agents

**Category:** Cross-Cutting / End-to-End  
**Difficulty:** Advanced  
**Duration:** 8-10 hours  
**Technologies:** Java, Spring MVC, Spring Boot, Azure Container Apps, Spec2Cloud, SQUAD Agents

## Overview

This lab demonstrates using SQUAD and Spec2Cloud together to decompose a monolithic Java application into microservices. Spec2Cloud identifies bounded contexts and service boundaries, SQUAD Brain plans the decomposition strategy, and SQUAD agents execute the migration incrementally using the Strangler Fig pattern. This showcases the agents' ability to handle one of the most complex modernization patterns — breaking apart a tightly coupled monolith.

## Learning Objectives

By completing this lab, you will:

- Use Spec2Cloud to identify bounded contexts in a Java monolith
- Plan microservices decomposition using the Strangler Fig pattern
- Coordinate SQUAD agents for incremental service extraction
- Implement saga patterns for cross-service transactions
- Deploy microservices to Azure Container Apps
- Understand the challenges and solutions for database decomposition
- Apply Domain-Driven Design principles to legacy codebases

## Prerequisites

### Knowledge Requirements
- Strong Java and Spring Framework experience
- Understanding of microservices patterns and principles
- Familiarity with Domain-Driven Design concepts
- Basic knowledge of containers and Docker
- Completed Spec2Cloud and SQUAD introductory labs

### Technical Requirements
- Docker Desktop installed and running
- JDK 11 or higher
- Maven 3.6+
- Azure subscription with appropriate permissions
- Azure CLI installed
- Git client
- IDE (IntelliJ IDEA, Eclipse, or VS Code)
- At least 8GB RAM available

### Recommended Setup
- 16GB RAM for optimal Docker performance
- Fast internet connection for downloading dependencies
- Dual monitors for viewing documentation and code

## Business Context

### EduVerse Academy Platform

EduVerse Academy is an online education platform that provides course creation, student enrollment, video streaming, assessments, certificate generation, and payment processing. The platform started as a traditional monolithic Java application deployed on Tomcat but has grown to handle thousands of courses and hundreds of thousands of students.

The current monolith suffers from:
- **Scaling challenges**: All features must scale together even when only video streaming needs more resources
- **Deployment risk**: Every feature change requires a full application deployment
- **Team productivity**: Multiple teams waiting on each other for database schema changes
- **Technology lock-in**: Unable to adopt new technologies for specific features
- **Performance bottlenecks**: Long-running operations (video processing, PDF generation) block other requests

### Success Criteria

By the end of this lab, you will have:
- Decomposed the monolith into 8 independent microservices
- Each service with its own database and bounded context
- Implemented the Strangler Fig pattern for zero-downtime migration
- Set up API Gateway routing between monolith and microservices
- Deployed all services to Azure Container Apps
- Maintained all business functionality throughout the migration

## The Monolithic Application

### Architecture

**Technology Stack:**
- Java 11 with Spring MVC (traditional WAR deployment)
- Tomcat 9 servlet container
- JPA/Hibernate with PostgreSQL (100+ tables)
- JSP pages with Apache Tiles for templating
- Spring Security with form-based authentication
- Quartz Scheduler for batch jobs
- Apache Commons Email for notifications
- Stripe SDK for payment processing
- iText for PDF certificate generation

### Key Components

```
eduverse-academy/
├── config/              # Spring configuration classes
│   ├── AppConfig.java           # MVC configuration
│   ├── SecurityConfig.java      # Authentication/authorization
│   ├── PersistenceConfig.java   # JPA/Hibernate setup
│   └── SchedulerConfig.java     # Quartz job configuration
├── controller/          # MVC controllers (9 classes)
├── service/             # Business logic (8 services)
├── model/               # JPA entities (13 entities)
├── repository/          # Spring Data JPA repositories
├── scheduler/           # Quartz scheduled jobs
└── webapp/              # JSP views, static resources
```

### Monolith Anti-Patterns

The application demonstrates classic monolith anti-patterns that need addressing:

| Anti-Pattern | Example | Impact |
|-------------|---------|--------|
| **Cross-Domain Transactions** | EnrollmentService.enrollStudent() creates enrollment, payment, and notification in single transaction | Service coupling, cannot scale independently |
| **Service Star Coupling** | EnrollmentService depends on 5 other services | Circular dependencies, hard to test |
| **Shared Database** | All 100+ tables in single PostgreSQL database with foreign keys across domains | Schema change coordination, deployment coupling |
| **Synchronous Email** | NotificationService.sendEmail() blocks within @Transactional methods | Poor performance, transaction timeouts |
| **Batch Job Cross-Access** | ReportGenerationJob queries all repositories directly | No domain boundaries |
| **Chatty Service Calls** | ProgressService called for every lesson completion | N+1 queries, performance issues |
| **PDF Generation in Service** | CertificateService generates PDFs synchronously | Long request times, memory pressure |
| **Video Processing in Thread** | VideoService.processVideo() uses Thread.sleep in transaction | Resource waste, transaction holding |

### Database Schema Overview

**Course Domain (30 tables):**
- courses, modules, lessons
- course_prerequisites, course_tags, course_reviews

**Enrollment Domain (25 tables):**
- enrollments, enrollment_history
- student_favorites, learning_paths

**Assessment Domain (20 tables):**
- assessments, questions, student_answers
- assessment_templates, question_banks

**Video Domain (8 tables):**
- videos, video_metadata, transcoding_jobs
- video_analytics

**Certificate Domain (5 tables):**
- certificates, certificate_templates
- certificate_verification_log

**Payment Domain (10 tables):**
- payments, payment_methods, invoices
- refunds, payment_audit_log

**Notification Domain (7 tables):**
- notifications, notification_templates
- email_queue, notification_preferences

**Progress Domain (8 tables):**
- student_progress, lesson_completions
- milestone_achievements, learning_analytics

## Step-by-Step Instructions

### Phase 1: Understanding the Monolith (60 minutes)

#### Step 1: Deploy and Explore the Monolith

1. **Clone and build the application:**

```bash
cd eduverse-academy
mvn clean package
```

2. **Start PostgreSQL and deploy with Docker:**

```bash
docker-compose up -d
```

Wait for the application to start. Access at http://localhost:8080

3. **Explore the application features:**

Login with these credentials:
- Admin: `admin` / `password`
- Instructor: `instructor1` / `password`
- Student: `student1` / `password`

Test each role's functionality:
- Browse course catalog
- Enroll in a course (triggers payment + notification)
- Complete lessons (updates progress)
- Take an assessment (auto-grading)
- View certificate upon completion

4. **Review the codebase structure:**

Use your IDE to explore:
- Service dependencies in `com.eduverse.service`
- Transaction boundaries (look for @Transactional)
- Cross-domain queries in repositories
- Scheduler jobs accessing multiple domains

**✅ Checkpoint:** You should understand how enrolling in a course triggers a chain of operations across enrollment, payment, notification, and progress domains in a single transaction.

#### Step 2: Analyze Dependencies and Coupling

Create a dependency diagram:

```bash
# Use your IDE's dependency analyzer or manually trace
# - Which services depend on NotificationService?
# - What happens if PaymentService is slow?
# - Which services directly call other services?
```

**Expected findings:**
- EnrollmentService has 5 service dependencies
- NotificationService is called by 6 different services
- ProgressService is called by 3 services
- Single transaction spans 3+ domains in enrollment flow

**✅ Checkpoint:** Document the service dependency graph showing the star topology around EnrollmentService and NotificationService.

### Phase 2: Spec2Cloud Bounded Context Analysis (90 minutes)

#### Step 3: Run Spec2Cloud Analysis

1. **Install Spec2Cloud CLI:**

```bash
npm install -g @azure/spec2cloud
spec2cloud --version
```

2. **Analyze the codebase:**

```bash
spec2cloud analyze \
  --source ./eduverse-academy/src \
  --language java \
  --framework spring \
  --output ./analysis-report
```

Spec2Cloud will:
- Identify entity clusters based on relationships
- Analyze service call patterns
- Detect transaction boundaries
- Calculate coupling metrics
- Suggest bounded contexts

3. **Review the analysis report:**

Open `./analysis-report/bounded-contexts.html`

Expected bounded contexts identified:
- Course Catalog (courses, modules, lessons)
- Enrollment Management (enrollments, prerequisites)
- Assessment & Grading (assessments, answers, grading)
- Video Management (videos, transcoding, streaming)
- Certificate Issuance (certificates, templates)
- Payment Processing (payments, invoices, refunds)
- Notifications (emails, in-app notifications)
- Student Progress (progress tracking, analytics)

4. **Examine coupling scores:**

```
Course ↔ Enrollment: 0.85 (high)
Enrollment ↔ Payment: 0.92 (very high)
Enrollment ↔ Progress: 0.78 (high)
Video ↔ Course: 0.65 (medium)
Certificate ↔ Enrollment: 0.70 (high)
Notification ↔ All: 0.95 (very high)
```

**✅ Checkpoint:** You should have a clear list of 8 bounded contexts with coupling metrics showing which domains are most entangled.

#### Step 4: Create Decomposition Strategy

Using Spec2Cloud's recommendations, plan the extraction order:

**Extraction Order (least to most coupled):**

1. **Notification Service** (Week 1) - Least coupled, async by nature
2. **Course Catalog Service** (Week 2) - Read-heavy, mostly independent
3. **Video Service** (Week 3) - Media processing, could be separate tech
4. **Certificate Service** (Week 4) - Simple domain, one-way dependency
5. **Assessment Service** (Week 5) - Self-contained grading logic
6. **Payment Service** (Week 6) - Requires saga implementation
7. **Progress Service** (Week 7) - Event-driven from multiple sources
8. **Enrollment Service** (Week 8) - Most coupled, orchestrates others

**Rationale:**
- Start with services that have clear boundaries
- Build infrastructure (API Gateway, Service Bus) early
- Implement saga pattern before tackling enrollment
- Leave most complex (enrollment) for last when team has experience

**✅ Checkpoint:** You have a documented decomposition plan with weekly milestones and clear extraction order.

### Phase 3: SQUAD Planning (60 minutes)

#### Step 5: Create SQUAD Decomposition Plan

1. **Initialize SQUAD Brain:**

```bash
squad init --project eduverse-migration
squad brain load-context ./analysis-report/bounded-contexts.json
```

2. **Generate migration plan:**

```bash
squad brain plan \
  --pattern strangler-fig \
  --services 8 \
  --priority safety \
  --output ./migration-plan.md
```

SQUAD Brain will create:
- Database migration strategy (database-per-service)
- API Gateway routing rules
- Event-driven communication patterns
- Saga orchestration for distributed transactions
- Testing strategy for each extraction
- Rollback procedures

3. **Review the generated plan:**

Key elements:
- **API Gateway Setup:** Spring Cloud Gateway to route between monolith and microservices
- **Service Bus:** Azure Service Bus for async events
- **Saga Orchestrator:** Handle enrollment-payment-notification flow
- **Database Strategy:** Split databases with dual-write pattern during migration
- **Testing:** Contract tests, integration tests, end-to-end tests

**✅ Checkpoint:** You have a detailed migration plan document reviewed and approved by SQUAD Brain.

### Phase 4: Infrastructure Setup (90 minutes)

#### Step 6: Set Up Strangler Fig Infrastructure

1. **Deploy API Gateway alongside monolith:**

```bash
# Create Azure resources
az group create --name eduverse-rg --location eastus

az containerapp env create \
  --name eduverse-env \
  --resource-group eduverse-rg \
  --location eastus
```

2. **Configure Spring Cloud Gateway:**

```yaml
# gateway-config.yml
spring:
  cloud:
    gateway:
      routes:
        - id: monolith
          uri: http://eduverse-monolith:8080
          predicates:
            - Path=/**
          order: 999  # Lowest priority, catch-all
```

3. **Deploy Service Bus for async communication:**

```bash
az servicebus namespace create \
  --resource-group eduverse-rg \
  --name eduverse-bus \
  --location eastus \
  --sku Standard

# Create topics for domain events
az servicebus topic create --namespace-name eduverse-bus \
  --name enrollment-events --resource-group eduverse-rg

az servicebus topic create --namespace-name eduverse-bus \
  --name payment-events --resource-group eduverse-rg
```

**✅ Checkpoint:** Gateway routes all traffic to monolith. Service Bus is ready for event publishing.

### Phase 5: Extract Notification Service (90 minutes)

#### Step 7: SQUAD Extraction - Notification Service

Notification Service is the ideal first extraction: it's called by many services (high fan-in) but doesn't call others (zero fan-out), and email sending is naturally asynchronous.

1. **Instruct SQUAD to extract Notification Service:**

```bash
squad agent create notification-extractor \
  --skill microservice-extraction \
  --context "./migration-plan.md" \
  --task "Extract Notification Service from the monolith. \
         Create Spring Boot microservice with its own database. \
         Consume events from Service Bus. \
         Preserve all email sending functionality."
```

SQUAD will:
- Create new Spring Boot 3.x project
- Copy Notification entity and repository
- Create NotificationController REST API
- Implement Service Bus consumer for events
- Write tests
- Create Dockerfile

2. **Review SQUAD's generated code:**

```java
// notification-service/src/main/java/com/eduverse/notification
@Service
public class NotificationEventConsumer {
    
    @ServiceBusListener(destination = "enrollment-events")
    public void handleEnrollmentEvent(EnrollmentCreatedEvent event) {
        notificationService.sendEnrollmentConfirmation(
            event.getStudentId(), 
            event.getCourseId()
        );
    }
}
```

3. **Update monolith to publish events:**

SQUAD will modify the monolith:

```java
// In EnrollmentService.enrollStudent()
// Before: notificationService.sendEmail(...)
// After: eventPublisher.publish(new EnrollmentCreatedEvent(...))
```

4. **Update Gateway routing:**

```yaml
- id: notifications
  uri: http://notification-service:8080
  predicates:
    - Path=/api/notifications/**
  order: 1  # High priority for notifications
```

5. **Deploy and test:**

```bash
cd notification-service
mvn clean package
docker build -t eduverse-notifications:v1 .

az containerapp create \
  --name notification-service \
  --resource-group eduverse-rg \
  --environment eduverse-env \
  --image your-acr.azurecr.io/eduverse-notifications:v1 \
  --target-port 8080 \
  --ingress external
```

Test enrollment flow - notification should be sent from new service.

**✅ Checkpoint:** Notification service is deployed, consuming events, sending emails. Monolith no longer sends emails directly.

### Phase 6: Extract Course Catalog Service (90 minutes)

#### Step 8: SQUAD Extraction - Course Catalog

Course Catalog is read-heavy with few writes, making it a good second extraction.

1. **Instruct SQUAD:**

```bash
squad agent create course-extractor \
  --task "Extract Course Catalog Service. \
         Includes Course, Module, Lesson entities. \
         REST API for browsing and searching courses. \
         Separate database with read replicas."
```

2. **Handle data migration:**

SQUAD creates a data migration script:

```sql
-- Copy course data to new database
INSERT INTO course_catalog.courses 
SELECT id, title, description, instructor_id, category, 
       price, status, published_date, duration_hours
FROM monolith.courses;
```

3. **Implement dual-write pattern during migration:**

Monolith continues writing to its database, but also publishes events:

```java
@Transactional
public Course createCourse(Course course) {
    Course saved = courseRepository.save(course);
    eventPublisher.publish(new CourseCreatedEvent(saved));
    return saved;
}
```

Course Catalog Service consumes events to stay in sync.

4. **Update Gateway to route catalog reads to microservice:**

```yaml
- id: course-catalog-read
  uri: http://course-catalog-service:8080
  predicates:
    - Path=/api/courses/**
    - Method=GET
  order: 2
```

5. **Gradual traffic shift:**

```bash
# Start with 10% traffic to microservice
az containerapp ingress traffic set \
  --name gateway-service \
  --traffic-weight monolith=90 course-catalog=10

# Monitor error rates, gradually increase to 100%
```

**✅ Checkpoint:** Course browsing uses microservice. Course creation in monolith syncs via events.

### Phase 7: Extract Payment Service with Saga (120 minutes)

#### Step 9: Implement Saga Pattern for Enrollment-Payment

Payment is tightly coupled with Enrollment. We need saga orchestration for distributed transactions.

1. **SQUAD creates Saga Orchestrator:**

```bash
squad agent create saga-implementor \
  --task "Implement saga orchestration for enrollment flow. \
         Enrollment Service → Payment Service coordination. \
         Handle failures and compensating transactions."
```

SQUAD generates:

```java
@Service
public class EnrollmentSaga {
    
    @Transactional
    public void executeEnrollment(EnrollmentRequest request) {
        // Step 1: Create enrollment (reserve)
        Enrollment enrollment = enrollmentService.createReservation(request);
        
        // Step 2: Process payment
        try {
            paymentService.processPayment(enrollment.getId(), request.getAmount());
        } catch (PaymentFailedException e) {
            // Compensate: cancel enrollment
            enrollmentService.cancelReservation(enrollment.getId());
            throw new EnrollmentFailedException("Payment failed", e);
        }
        
        // Step 3: Confirm enrollment
        enrollmentService.confirmEnrollment(enrollment.getId());
        
        // Step 4: Publish events
        eventPublisher.publish(new EnrollmentCompletedEvent(enrollment));
    }
}
```

2. **Extract Payment Service:**

SQUAD creates:
- Payment Service microservice
- Database with payments, invoices, refunds tables
- Stripe integration
- REST API for payment operations
- Compensating transaction endpoints

3. **Update enrollment flow:**

Monolith's EnrollmentController now calls Saga Orchestrator:

```java
@PostMapping("/enroll/{courseId}")
public String enroll(@PathVariable Long courseId) {
    enrollmentSaga.executeEnrollment(
        new EnrollmentRequest(courseId, getCurrentUserId())
    );
    return "redirect:/student/enrollments";
}
```

4. **Test failure scenarios:**

```bash
# Test payment failure - enrollment should be rolled back
# Test timeout - saga should retry or compensate
# Test partial failure - verify idempotency
```

**✅ Checkpoint:** Enrollment-payment flow works across services with proper failure handling.

### Phase 8: Complete Remaining Extractions (Guided Practice)

#### Step 10: Extract Video, Certificate, Assessment, Progress Services

Following the same pattern, extract remaining services:

**Video Service:**
- Handles video uploads, transcoding, streaming
- Uses Azure Media Services
- Events: VideoProcessed, VideoReady

**Certificate Service:**
- Generates PDF certificates
- Async processing via queue
- Events: CertificateIssued

**Assessment Service:**
- Quiz creation and grading
- Auto-grading engine
- Events: AssessmentCompleted

**Progress Service:**
- Consumes events from Assessment, Video, Lesson completion
- Calculates overall progress
- Events: MilestoneAchieved, CourseCompleted

For each service:

```bash
squad agent create [service-name]-extractor \
  --task "Extract [Service Name] following the established pattern"
```

SQUAD will:
- Create microservice
- Migrate data
- Implement event consumers/publishers
- Update Gateway routes
- Write tests

**✅ Checkpoint:** All 8 microservices are deployed and functional.

### Phase 9: Final Enrollment Service Extraction (90 minutes)

#### Step 11: Complete Strangler Fig - Retire Monolith

Enrollment Service is the last extraction and most complex due to its orchestration role.

1. **Extract remaining enrollment logic:**

```bash
squad agent create enrollment-extractor \
  --task "Extract final Enrollment Service. \
         This service orchestrates sagas. \
         After extraction, monolith can be retired."
```

2. **Update API Gateway to route all traffic to microservices:**

```yaml
# Remove monolith catch-all route
# All routes now point to specific microservices
```

3. **Decommission monolith:**

```bash
# Verify all routes work without monolith
# Stop monolith container
docker-compose stop eduverse-monolith

# Test all features end-to-end
./run-e2e-tests.sh

# If successful, delete monolith
az containerapp delete --name eduverse-monolith
```

**✅ Checkpoint:** Monolith is fully retired. All features work via microservices.

### Phase 10: Production Deployment (60 minutes)

#### Step 12: Deploy to Azure Container Apps

1. **Create production environment:**

```bash
az containerapp env create \
  --name eduverse-prod \
  --resource-group eduverse-prod-rg \
  --location eastus \
  --enable-workload-profiles
```

2. **Deploy all services:**

```bash
# Deploy each microservice
for service in notification course-catalog payment video certificate \
               assessment progress enrollment; do
  az containerapp create \
    --name ${service}-service \
    --resource-group eduverse-prod-rg \
    --environment eduverse-prod \
    --image your-acr.azurecr.io/eduverse-${service}:v1 \
    --target-port 8080 \
    --min-replicas 2 \
    --max-replicas 10 \
    --cpu 1.0 --memory 2.0Gi
done
```

3. **Configure autoscaling:**

```bash
az containerapp update \
  --name video-service \
  --scale-rule-name cpu-scale \
  --scale-rule-type cpu \
  --scale-rule-metadata type=Utilization value=70
```

4. **Set up monitoring:**

```bash
az monitor app-insights component create \
  --app eduverse-insights \
  --resource-group eduverse-prod-rg \
  --location eastus

# Configure each service to send telemetry
```

**✅ Checkpoint:** All microservices running in production on Azure Container Apps with monitoring.

## Validation and Testing

### End-to-End Testing Checklist

Test each user flow:

- [ ] Browse course catalog (Course Catalog Service)
- [ ] Enroll in a course (Saga: Enrollment + Payment)
- [ ] Receive enrollment email (Notification Service)
- [ ] Watch video lesson (Video Service)
- [ ] Complete lesson (Progress Service)
- [ ] Take assessment (Assessment Service)
- [ ] Complete course (Progress Service triggers Certificate)
- [ ] Download certificate (Certificate Service)
- [ ] View payment history (Payment Service)

### Performance Validation

Compare before/after metrics:

| Metric | Monolith | Microservices | Improvement |
|--------|----------|---------------|-------------|
| Course catalog page load | 850ms | 120ms | 85% faster |
| Enrollment processing | 3.2s | 1.1s | 66% faster |
| Video upload processing | Blocks | Async | Non-blocking |
| Peak RPS per service | N/A (shared) | Independent | Unlimited |
| Deployment frequency | 1x/month | 10x/week | 40x more |

### Resilience Testing

Test failure scenarios:

```bash
# Kill Payment Service
kubectl delete pod payment-service-xxx

# Enrollment should still create reservation
# Saga should retry payment when service recovers
```

## Key Concepts Covered

### Bounded Context Identification
- Using Spec2Cloud to analyze entity relationships
- Identifying service boundaries based on cohesion
- Calculating coupling metrics between contexts

### Strangler Fig Pattern
- Running monolith and microservices side-by-side
- Gradually routing traffic to microservices
- Safe rollback capabilities at each step

### Saga Orchestration
- Distributed transaction management
- Compensating transactions for failures
- Event-driven coordination between services

### Database Decomposition
- Database-per-service pattern
- Dual-write during migration
- Eventual consistency between services

### Event-Driven Architecture
- Domain event publishing
- Event sourcing for state changes
- CQRS for read/write separation

### Agentic Development
- Using SQUAD agents for code generation
- Automated refactoring and extraction
- Agent collaboration for complex tasks

## Troubleshooting

### Common Issues

**Issue: Services can't connect to Service Bus**
```bash
# Check connection string
az servicebus namespace authorization-rule keys list \
  --resource-group eduverse-rg \
  --namespace-name eduverse-bus \
  --name RootManageSharedAccessKey
```

**Issue: Database migration failures**
```bash
# Check if tables exist in target database
# Verify foreign key constraints are dropped
# Ensure data types match between databases
```

**Issue: Saga transaction failures**
```bash
# Check compensating transaction logs
# Verify idempotency of operations
# Review timeout configurations
```

## Additional Resources

### Documentation
- [Strangler Fig Pattern](https://martinfowler.com/bliki/StranglerFigApplication.html)
- [Saga Pattern](https://microservices.io/patterns/data/saga.html)
- [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)
- [Azure Container Apps](https://learn.microsoft.com/azure/container-apps/)

### Sample Code Repositories
- Complete microservices implementation
- Saga orchestrator examples
- Database migration scripts
- Testing framework

## Conclusion

You have successfully decomposed a tightly coupled monolith into 8 independent microservices using Spec2Cloud for analysis and SQUAD agents for implementation. The Strangler Fig pattern allowed for safe, incremental migration with zero downtime.

Key takeaways:
- Domain-Driven Design principles guide service boundaries
- Saga pattern handles distributed transactions
- Event-driven architecture enables loose coupling
- AI agents can automate complex refactoring tasks
- Microservices enable independent scaling and deployment

## Next Steps

1. **Optimize Performance**: Add caching, implement CQRS, optimize queries
2. **Add Observability**: Distributed tracing, metrics, logging aggregation
3. **Implement Security**: OAuth2, API authentication, secrets management
4. **CI/CD Pipeline**: Automated testing, deployment pipelines
5. **Advanced Patterns**: Circuit breakers, rate limiting, service mesh

---

**Lab Complete!** You've mastered microservices decomposition with AI agents.
