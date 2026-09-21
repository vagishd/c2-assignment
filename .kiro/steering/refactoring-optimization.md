# Refactoring / Clean Code

## When to Use
Apply this skill whenever asked to: "refactor this", "clean up", "make this more readable", "this is messy", "restructure", "improve code quality", "too much duplication", or when code is correct but hard to maintain.

---

## Core Principles

1. **Refactor without changing behavior.** If observable behavior changes, that's a feature, not a refactor.
2. **One thing at a time.** Rename, then extract, then restructure — not all at once.
3. **Leave tests green.** If there are tests, they should pass before and after.
4. **Name things for what they mean, not what they do mechanically.**

---

## Smell → Fix Catalogue

| Smell | Fix |
|---|---|
| Long function (>30 lines) | Extract sub-functions with intention-revealing names |
| Deep nesting (>3 levels) | Early returns / guard clauses, extract helpers |
| Duplicated logic | Extract shared function or base class |
| Magic numbers / strings | Named constants or enums |
| Boolean flag arguments | Split into two named functions |
| God class (does everything) | Break into focused classes by responsibility |
| Comments explaining *what* code does | Rename so the code speaks for itself |
| Data clumps (same 3+ args passed everywhere) | Introduce a data class / value object |

---

## Python

### Naming
- Functions: `verb_noun` — `calculate_score()`, `fetch_user_by_id()`
- Classes: `PascalCase`, noun-first — `UserProfile`, `RecommendationEngine`
- Constants: `UPPER_SNAKE_CASE`
- Avoid: `data`, `info`, `thing`, `process_it()`

### Structure
- Replace `if/elif` chains on type with polymorphism or `match` (3.10+)
- Use `@dataclass` or `@dataclass(frozen=True)` for plain data holders
- Use `Enum` for named states instead of string literals
- Keep functions under 20–25 lines; if it scrolls, it probably does two things
- Use `Protocol` for duck-typed interfaces instead of loose coupling

### Guard Clauses (flatten nesting)
```python
# Before — arrow-head nesting
def process(user):
    if user:
        if user.is_active:
            if user.has_permission("write"):
                do_work(user)

# After — guard clauses
def process(user):
    if not user:
        return
    if not user.is_active:
        return
    if not user.has_permission("write"):
        return
    do_work(user)
```

### Extract & Name
```python
# Before — magic number, unclear intent
if score > 0.72:
    recommend(item)

# After
RECOMMENDATION_THRESHOLD = 0.72

def is_worth_recommending(score: float) -> bool:
    return score > RECOMMENDATION_THRESHOLD

if is_worth_recommending(score):
    recommend(item)
```

---

## Java / Spring Boot

### Naming
- Methods: `camelCase`, verb-first — `calculateDiscount()`, `findActiveUsers()`
- Classes: `PascalCase`, single responsibility — `OrderValidator`, `PaymentGateway`
- Constants: `UPPER_SNAKE_CASE` in interface or `static final`
- Avoid: `Manager`, `Helper`, `Utils` as the entire name — be specific

### Structure
- Keep methods under 20–25 lines
- Prefer constructor injection over `@Autowired` on fields — easier to test
- Use DTOs at API boundaries, not JPA entities directly
- Introduce a Service layer between Controller and Repository — controllers route, services decide
- Replace `instanceof` chains with polymorphism or the visitor pattern
- Use `Optional<T>` return types instead of `null` for "may not exist" semantics

### Spring Layering
```
Controller  →  Service  →  Repository
   (routes)    (logic)      (data)
```
- Controllers: only request/response handling, no business logic
- Services: all business rules, validation, orchestration
- Repositories: only data access, no business rules

### Guard Clauses
```java
// Before
public void process(Order order) {
    if (order != null) {
        if (order.isValid()) {
            if (order.hasItems()) {
                fulfil(order);
            }
        }
    }
}

// After
public void process(Order order) {
    if (order == null) throw new IllegalArgumentException("Order must not be null");
    if (!order.isValid()) return;
    if (!order.hasItems()) return;
    fulfil(order);
}
```

### Extract Method
```java
// Before — one big method doing validation + saving + notifying
public void submitOrder(Order order) {
    if (order.getItems().isEmpty()) throw new InvalidOrderException("No items");
    if (order.getTotal() <= 0) throw new InvalidOrderException("Invalid total");
    orderRepo.save(order);
    emailService.send(order.getUserEmail(), "Order confirmed");
    auditLog.record(order.getId(), "SUBMITTED");
}

// After
public void submitOrder(Order order) {
    validateOrder(order);
    orderRepo.save(order);
    notifyUser(order);
    auditLog.record(order.getId(), "SUBMITTED");
}

private void validateOrder(Order order) {
    if (order.getItems().isEmpty()) throw new InvalidOrderException("No items");
    if (order.getTotal() <= 0) throw new InvalidOrderException("Invalid total");
}
```

---

## Output Format

When refactoring, provide:
1. **What smells were found** (brief list)
2. **Refactored code** (complete, not just the changed lines)
3. **What changed and why** (one line per meaningful change)
4. **What was intentionally left alone** (to signal you considered it)