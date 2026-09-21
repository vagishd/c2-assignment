---
name: java-spring-standards
description: >
  Enterprise Java and Spring Boot development standards. Apply whenever writing, reviewing,
  refactoring, or generating Java or Kotlin code — especially in Spring Boot projects.
  Covers naming conventions, SOLID principles, layering, exception handling, logging, Lombok,
  JPA, security, testing, and API design. Also triggers for tasks like "write a service",
  "create a controller", "add an endpoint", "write a repository", or "implement this feature"
  in a Java/Spring context.
globs: "*.java,*.kt"
alwaysApply: true
---

# Enterprise Java & Spring Boot Standards

---

# MANDATORY: Business Logic Preservation

Before writing or changing any code, confirm these are never altered without an explicit request:

| Constraint | Applies To |
|---|---|
| Business logic | All layers |
| API contracts (request/response shape) | Controllers, DTOs |
| Validation rules | Beans, Services |
| Exception types and messages | All layers |
| Transaction boundaries | Services, Repositories |
| Security and authorization behavior | Filters, Controllers, Services |
| Result ordering | Queries, Stream pipelines |
| Backward compatibility | All public APIs |

If a change would violate any of the above — **stop and flag it explicitly** before proceeding.

---

# Project Structure

```
src/main/java/com/company/app/
├── controller/       ← REST endpoints only; no business logic
├── service/          ← All business logic and orchestration
├── repository/       ← Data access only; no business logic
├── domain/           ← JPA entities and domain objects
├── dto/              ← Request/response objects; never expose entities directly
├── mapper/           ← MapStruct mappers between domain and DTO
├── exception/        ← Custom business exceptions and global handler
├── config/           ← Spring configuration beans
└── util/             ← Stateless pure utility functions only
```

**Layering rule:**
```
Request → Controller → Service → Repository → DB
                         ↑
               All business logic lives here
```
Controllers route. Services decide. Repositories fetch. Nothing crosses layers.

---

# Naming Conventions

| Element | Convention | Example |
|---|---|---|
| Class | PascalCase, noun-first, specific | `OrderValidationService`, `UserRepository` |
| Method | camelCase, verb-first, intention-revealing | `findActiveSubscribers()`, `calculateMonthlyFee()` |
| Variable | camelCase, descriptive | `customerId`, `subscriptionPlan` |
| Constant | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT`, `DEFAULT_PAGE_SIZE` |
| Package | lowercase, no underscore | `com.company.app.service` |

**Avoid**: `Manager`, `Helper`, `Utils` as the *entire* name — they communicate nothing.
**Prefer**: `CustomerNotificationService` over `NotificationHelper`.
**Never use**: `data`, `temp`, `obj`, `info`, `thing` as variable names.
**Test methods**: use `methodName_condition_expectedBehavior()` — underscores are allowed and encouraged in test names.

---

# Formatting

- 4 spaces; no tabs
- Max line length: 120 characters
- One class per file
- Organize imports (no wildcards: `import java.util.*` is banned)
- `@Override` on every overriding method
- Blank line between logical blocks inside a method

---

# Object-Oriented Principles

## Encapsulation
- All fields `private`
- Constructor injection only (no field injection via `@Autowired`)
- Prefer immutability — use `final` fields where possible

## Composition over Inheritance
- Avoid deep inheritance chains (max 2 levels beyond framework base)
- Use delegation and composition to share behavior

## Program to Interfaces
```java
// Good
private final UserRepository userRepository;  // interface

// Bad
private final UserRepositoryImpl userRepository;  // concrete class
```

## Single Responsibility
Every class has one reason to change. If you describe a class's job using "and", split it.

---

# SOLID in Practice

| Principle | Spring Boot Application |
|---|---|
| **S** – Single Responsibility | One service per business domain; no "god services" |
| **O** – Open/Closed | Use Strategy pattern and polymorphism; extend by adding, not modifying |
| **L** – Liskov Substitution | Implementations fully satisfy interface contracts |
| **I** – Interface Segregation | Small focused interfaces; no methods forced on unrelated classes |
| **D** – Dependency Inversion | Depend on abstractions (interfaces), not concrete implementations |

---

# Spring Boot Conventions

## Dependency Injection — Constructor Injection Only

```java
// Good — @RequiredArgsConstructor generates constructor from final fields
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;
    private final NotificationService notificationService;
}

// Bad — field injection breaks testability and hides dependencies
@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;
}
```

## Controllers — Thin, Routing Only

```java
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Place a new order")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Order placed"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "409", description = "Conflict — duplicate order")
    })
    public OrderResponse placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        return orderService.placeOrder(request);
    }
}
```

Controllers must not contain: business logic, manual validation, transaction management, or direct repository access.

## Services — Business Logic Lives Here

```java
@Service
@RequiredArgsConstructor
@Transactional  // write operations
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    @Transactional(readOnly = true)  // always mark reads — reduces flush overhead
    public OrderResponse findOrderById(Long orderId) {
        return orderRepository.findById(orderId)
            .map(orderMapper::toResponse)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    public OrderResponse placeOrder(PlaceOrderRequest request) {
        inventoryService.reserveItems(request.getItems());
        Order order = Order.from(request);
        return orderMapper.toResponse(orderRepository.save(order));
    }
}
```

**Transactional pitfalls to avoid:**
- `@Transactional` on `private` methods is silently ignored — Spring AOP doesn't proxy them
- Self-invocation (`this.methodName()`) bypasses the proxy — extract to a separate bean if needed
- Catching and swallowing exceptions inside a `@Transactional` method prevents rollback

## Repositories — Data Access Only

```java
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Derived query for simple cases
    List<Order> findByUserId(Long userId);

    // JPQL with JOIN FETCH to prevent N+1
    @Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.userId = :userId AND o.status = :status")
    List<Order> findByUserIdAndStatus(@Param("userId") Long userId,
                                      @Param("status") OrderStatus status);

    // DTO projections for list/summary views — never load full entities
    @Query("SELECT new com.app.dto.OrderSummary(o.id, o.total, o.status, o.createdAt) " +
           "FROM Order o WHERE o.userId = :userId ORDER BY o.createdAt DESC")
    Page<OrderSummary> findSummariesByUserId(@Param("userId") Long userId, Pageable pageable);
}
```

---

# DTO Standards

```java
// Request DTO — validate every field
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotEmpty(message = "Order must contain at least one item")
    @Valid  // cascade validation into nested objects
    private List<OrderItemRequest> items;

    @NotNull
    @Positive(message = "Total must be positive")
    private BigDecimal total;
}

// Response DTO — immutable
@Value
@Builder
public class OrderResponse {
    Long id;
    Long userId;
    List<OrderItemResponse> items;
    BigDecimal total;
    OrderStatus status;
    LocalDateTime createdAt;
}
```

**Rules:**
- Never expose JPA entities in API responses — always map to DTOs
- Use MapStruct for entity↔DTO mappings; avoid manual get/set chains
- Validate all inputs with Bean Validation annotations — never use `if (x == null)` as input validation

---

# Java 8+ Features

## Streams

```java
// Good — readable, single-pass pipeline
List<OrderSummary> summaries = orders.stream()
    .filter(Order::isActive)
    .map(orderMapper::toSummary)
    .sorted(Comparator.comparing(OrderSummary::getCreatedAt).reversed())
    .toList();

// Bad — unnecessary collect-then-stream
orders.stream()
    .collect(Collectors.toList())  // materializes for no reason
    .stream()
    .filter(...)
```

Never produce side effects inside `.map()` or `.filter()`. Avoid complex nested stream chains — extract to named methods.

## Optional

```java
// Good — safe unwrap with business exception
User user = userRepository.findByEmail(email)
    .orElseThrow(() -> new UserNotFoundException(email));

// Good — transform if present
String name = userOpt.map(User::getName).orElse("Anonymous");

// Never — throws NoSuchElementException, not your business exception
return userOpt.get();
```

## Method References

```java
// Prefer
users.stream().map(User::getName).toList();
users.forEach(this::processUser);

// Over
users.stream().map(u -> u.getName()).toList();
```

## Records (Java 16+)

```java
// Ideal for immutable value/response objects when Lombok isn't in scope
public record UserSummary(Long id, String name, String email) {}
```

---

# Exception Handling

## Custom Exception Hierarchy

```java
// Base — all business exceptions extend this
public class BusinessException extends RuntimeException {
    private final String errorCode;
    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}

// Specific — one per domain concept
public class OrderNotFoundException extends BusinessException {
    public OrderNotFoundException(Long orderId) {
        super("ORDER_NOT_FOUND", "Order not found with id: " + orderId);
    }
}
```

## Global Exception Handler

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(OrderNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ErrorResponse.of(ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .toList();
        return ErrorResponse.of("VALIDATION_FAILED", String.join(", ", errors));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);  // pass exception as arg — includes stack trace
        return ErrorResponse.of("INTERNAL_ERROR", "An unexpected error occurred");
    }
}
```

**Rules:**
- Never swallow exceptions silently: `catch (Exception e) {}` is never acceptable
- Always catch specific exception types — never catch `Throwable` in application code
- Pass the exception as the last logger argument to include the stack trace
- Never use exceptions for control flow
- Use try-with-resources for all `Closeable` resources (streams, connections, sessions)

---

# Logging

```java
@Slf4j
public class OrderService {

    public OrderResponse placeOrder(PlaceOrderRequest request) {
        log.info("Placing order for user {}", request.getUserId());  // parameterized — no concat

        // ... business logic ...

        log.info("Order {} created for user {}", order.getId(), request.getUserId());
        return response;
    }

    private void handlePaymentFailure(Long orderId, Exception ex) {
        log.error("Payment failed for order {}", orderId, ex);  // exception as 3rd arg = stack trace
    }
}
```

## Log Levels

| Level | Use For |
|---|---|
| `ERROR` | Unexpected failures requiring investigation |
| `WARN` | Expected failures (not found, validation errors, retries) |
| `INFO` | Significant business events (order placed, user registered) |
| `DEBUG` | Diagnostic details (intermediate values, method entry/exit) |

## Never Log
- Passwords, tokens, secrets, API keys
- Full credit card numbers or CVVs
- PII (national ID, full DOB, medical data) — mask if needed
- Full request bodies in production unless masked

---

# Lombok Usage

```java
// Entity — never @Data (breaks equals/hashCode with Hibernate lazy loading)
@Entity
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"orders"})           // exclude collections — avoids N+1 in toString
@EqualsAndHashCode(of = "id")             // identity equality only
public class User { ... }

// Mutable DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse { ... }

// Immutable DTO / value object
@Value
@Builder
public class PricingResult { ... }

// Service / component
@Service
@RequiredArgsConstructor                  // generates constructor injection
public class UserService { ... }
```

---

# JPA / Database Standards

## Prevent N+1

```java
// Bad — triggers N queries for associations
List<User> users = userRepository.findAll();
users.forEach(u -> process(u.getOrders()));  // each getOrders() = 1 query

// Good — single JOIN FETCH query
@Query("SELECT u FROM User u JOIN FETCH u.orders WHERE u.active = true")
List<User> findActiveUsersWithOrders();

// Alternative — EntityGraph (avoids modifying existing queries)
@EntityGraph(attributePaths = {"orders", "preferences"})
Optional<User> findWithDetailsByEmail(String email);
```

## Pagination — Always on Unbounded Queries

```java
// Never — loads every row
List<Order> all = orderRepository.findAll();

// Always
Page<OrderSummary> page = orderRepository.findSummariesByUserId(
    userId, PageRequest.of(pageNum, 50, Sort.by("createdAt").descending()));
```

## Batch Writes

```properties
# application.properties — without these, batch_size alone doesn't actually batch
spring.jpa.properties.hibernate.jdbc.batch_size=50
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
```

```java
// Use saveAll(), never save() in a loop
itemRepository.saveAll(items);
```

## Entity Design

```java
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_orders_user_id", columnList = "user_id"),
    @Index(name = "idx_orders_status_created", columnList = "status, created_at")
})
@Getter @Setter @NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class Order {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)   // always STRING — ORDINAL breaks on enum reordering
    private OrderStatus status;

    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp   private LocalDateTime updatedAt;
}
```

**Never use**: `FetchType.EAGER`, `@Data` on entities, `GenerationType.AUTO` in production, `SELECT *` in native queries.

---

# API Design Standards

- REST: `GET` (read), `POST` (create), `PUT` (replace), `PATCH` (partial update), `DELETE`
- Correct HTTP status codes: `200 OK`, `201 Created`, `204 No Content`, `400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found`, `409 Conflict`, `422 Unprocessable Entity`, `500 Internal Server Error`
- Version all APIs: `/api/v1/...`
- Validate all request bodies with `@Valid`
- Document all endpoints with OpenAPI/Swagger (`@Operation`, `@ApiResponse`, `@Parameter`)
- Never break existing contracts — add new fields or endpoints; don't change existing ones
- Return consistent error response shape from `GlobalExceptionHandler`



---

# Clean Code Rules

## Methods
- Max 20–30 lines; if it scrolls, it does two things
- Max 3 levels of nesting — use guard clauses to flatten
- Max 8 parameters — if more needed, introduce a parameter object (`@Builder` DTO)

## Guard Clauses

```java
// Bad — arrow nesting
public void processOrder(Order order) {
    if (order != null) {
        if (order.isValid()) {
            if (inventoryService.hasStock(order)) {
                fulfil(order);
            }
        }
    }
}

// Good — linear, readable
public void processOrder(Order order) {
    Objects.requireNonNull(order, "Order must not be null");
    if (!order.isValid()) throw new InvalidOrderException(order.getId());
    if (!inventoryService.hasStock(order)) throw new InsufficientStockException(order.getId());
    fulfil(order);
}
```

## Constants Over Magic Values

```java
// Bad
if (retryCount > 3) { ... }

// Good
private static final int MAX_RETRY_COUNT = 3;
if (retryCount > MAX_RETRY_COUNT) { ... }
```

## DRY / YAGNI / KISS
- **DRY**: Duplicated logic in two places needs a named home — a private method, utility, or service
- **YAGNI**: Don't add abstraction layers for requirements that don't exist yet
- **KISS**: The simplest correct solution is always the right solution

---

# Design Patterns

| Pattern | Use When |
|---|---|
| **Strategy** | Interchangeable algorithms — pricing rules, scoring models, notification channels |
| **Factory / Factory Method** | Object creation logic varies by type or context |
| **Builder** | Complex construction with many optional parameters |
| **Adapter** | Integrating external APIs or legacy systems with a different interface |
| **Observer / Spring Events** | Decoupled side effects — audit logging, notifications, cache invalidation |
| **Template Method** | Shared workflow with customizable steps — data pipelines, report generation |

Avoid over-engineering. If a pattern adds indirection without solving a real problem, don't use it.

---

# Code Review Checklist

## Architecture
- [ ] Controller → Service → Repository layering followed
- [ ] No business logic in controllers or repositories
- [ ] Entities not exposed at API boundaries (DTOs used)
- [ ] SOLID principles respected

## Correctness
- [ ] `Optional.get()` never called without guard
- [ ] No `==` for String comparison
- [ ] `@Transactional` not on `private` methods
- [ ] `@Transactional` not bypassed via self-invocation
- [ ] No `FetchType.EAGER`
- [ ] No unbounded `findAll()` — pagination present
- [ ] N+1 not introduced (check new associations)
- [ ] try-with-resources on all `Closeable` resources
- [ ] Exceptions not swallowed silently

## Security
- [ ] No hardcoded credentials
- [ ] No PII or secrets in logs
- [ ] All inputs validated with Bean Validation
- [ ] Parameterized queries only — no string concatenation into SQL
- [ ] New endpoints covered by security config


## Performance
- [ ] No N+1 queries
- [ ] Pagination on list endpoints
- [ ] `@Transactional(readOnly = true)` on all read methods
- [ ] No `SELECT *` in native queries
- [ ] `saveAll()` used instead of `save()` in loops

---

# MCP Integration Rules

If the prompt contains any of the following keywords, invoke relevant MCP tools **before** responding:

- `BingeSmart`
- `BingeLearn`