# Contributing to EduVerse Academy Monolith

This document provides guidelines for contributing to the EduVerse Academy monolithic application, which is designed as a teaching example for the "Monolith to Microservices with Agents" lab.

## Purpose of This Project

This is an **intentionally flawed monolith** designed to demonstrate:
- Common anti-patterns in monolithic applications
- Tight coupling between services and data
- Cross-domain transactions
- Service dependency trees
- Challenges that necessitate decomposition

**Important:** This code is designed for educational purposes. Many patterns used here are anti-patterns that should be avoided in production systems.

## Anti-Patterns Present (By Design)

1. **Cross-Domain Transactions**: Single transactions spanning enrollment, payment, and notification
2. **Service Star Coupling**: EnrollmentService depends on 5 other services
3. **Synchronous Email**: Blocking email sends within transactions
4. **Direct Repository Access in Jobs**: Scheduler jobs querying all repositories
5. **PDF Generation in Request Thread**: Certificate generation blocks HTTP requests
6. **Shared Database**: 100+ tables with foreign keys across all domains

These are **intentional** to provide a realistic migration scenario.

## Code Structure Guidelines

### Adding New Entities

When adding entities to demonstrate additional coupling:

```java
@Entity
@Table(name = "new_entity")
public class NewEntity {
    // Use Long IDs for cross-domain references (demonstrates coupling)
    @Column(name = "other_domain_id")
    private Long otherDomainId;
    
    // Avoid using @ManyToOne across domains in new entities
    // Use IDs only to show loose entity relationships
}
```

### Adding New Services

Services should demonstrate tight coupling:

```java
@Service
public class NewService {
    
    @Autowired
    private SomeRepository repository;
    
    @Autowired
    private OtherService otherService;  // Demonstrate coupling
    
    @Autowired
    private NotificationService notificationService;  // Common dependency
    
    @Transactional
    public void doSomething() {
        // Mix domain concerns in single transaction
        repository.save(...);
        otherService.doRelatedThing();
        notificationService.sendEmail(...);  // Sync email in transaction
    }
}
```

### Adding New Controllers

Controllers should follow the existing pattern:

```java
@Controller
@RequestMapping("/new-feature")
public class NewFeatureController {
    
    private static final Logger logger = LoggerFactory.getLogger(NewFeatureController.class);
    
    @Autowired
    private NewService newService;
    
    @GetMapping
    public String index(Model model) {
        try {
            // Load data
            // Add to model
            return "new-feature/index";
        } catch (Exception e) {
            logger.error("Error loading feature", e);
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }
}
```

## Testing Guidelines

### Unit Tests

Keep tests simple to demonstrate testability challenges:

```java
@RunWith(MockitoJUnitRunner.class)
public class NewServiceTest {
    
    @Mock
    private SomeRepository repository;
    
    @Mock
    private OtherService otherService;
    
    @Mock
    private NotificationService notificationService;
    
    @InjectMocks
    private NewService newService;
    
    @Test
    public void testDoSomething() {
        // Demonstrate need to mock many dependencies
        when(repository.save(any())).thenReturn(entity);
        when(otherService.doRelatedThing()).thenReturn(true);
        
        newService.doSomething();
        
        verify(notificationService).sendEmail(any());
    }
}
```

### Integration Tests

Not required for this teaching example, but if added:

```java
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class EnrollmentIntegrationTest {
    // Full stack tests showing cross-domain operations
}
```

## Database Changes

### Adding Tables

Add to `data.sql` with seed data:

```sql
-- New domain table (contributes to the 100+ table count)
CREATE TABLE new_domain_entity (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    related_course_id BIGINT,  -- Cross-domain FK
    related_student_id BIGINT, -- Another cross-domain FK
    FOREIGN KEY (related_course_id) REFERENCES courses(id),
    FOREIGN KEY (related_student_id) REFERENCES users(id)
);

-- Seed data
INSERT INTO new_domain_entity (name, related_course_id, related_student_id) VALUES
('Example 1', 1, 4),
('Example 2', 2, 5);
```

## JSP View Guidelines

Keep views simple and functional:

```jsp
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

<div class="container mt-4">
    <h1>${pageTitle}</h1>
    
    <c:forEach items="${items}" var="item">
        <div class="card mb-3">
            <div class="card-body">
                <h5 class="card-title">${item.name}</h5>
                <p class="card-text">${item.description}</p>
            </div>
        </div>
    </c:forEach>
</div>
```

## What NOT to Fix

**Do not "fix" these anti-patterns** — they're intentional:

- ❌ Don't split services into smaller services
- ❌ Don't make email async in the monolith
- ❌ Don't normalize the database
- ❌ Don't add caching layers
- ❌ Don't implement circuit breakers
- ❌ Don't add retry logic
- ❌ Don't split into modules

The point is to show a **realistic legacy monolith** that needs decomposition.

## What TO Contribute

✅ **DO contribute:**

- Additional realistic features that add complexity
- More cross-domain coupling examples
- Additional scheduler jobs demonstrating batch coupling
- More JSP views for completeness
- Enhanced seed data for demos
- Documentation improvements
- Bug fixes that break the demo

## Coding Standards

### Java Style
- Use Java 11 features (no newer Java versions)
- Follow standard Java naming conventions
- Add SLF4J loggers to all services and controllers
- Use Spring annotations (@Service, @Repository, @Controller)

### SQL Style
- Use PostgreSQL-compatible SQL
- Table names: lowercase with underscores
- Column names: lowercase with underscores
- Always provide seed data for new tables

### JSP Style
- Use Bootstrap 4 for styling
- Use JSTL tags, avoid scriptlets
- Include Spring Security tags for role checks
- Keep JavaScript in separate files

## Pull Request Process

1. **Fork** the repository
2. **Create a feature branch** (`git checkout -b feature/add-instructor-reports`)
3. **Add your changes** following the guidelines above
4. **Test locally** with Docker Compose
5. **Commit** with clear messages
6. **Push** to your fork
7. **Open a Pull Request** with description of what coupling/anti-pattern you're demonstrating

### PR Description Template

```markdown
## What does this PR add?

Brief description of the new feature or enhancement.

## What anti-pattern does it demonstrate?

Explain the coupling or anti-pattern this change introduces.

## How to test

Steps to verify the changes work:
1. Start with `docker-compose up`
2. Login as...
3. Navigate to...
4. Observe...

## Related Lab Section

Which part of APPMODLAB.md does this relate to?
```

## Questions?

For questions about:
- **Lab content**: Open an issue with label `documentation`
- **Technical issues**: Open an issue with label `bug`
- **Enhancement ideas**: Open an issue with label `enhancement`

## License

This project is for educational purposes as part of the AppModLabs collection. See LICENSE file for details.
