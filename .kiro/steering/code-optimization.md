---
name: code-optimization
description: >
  Apply this skill whenever the user asks to optimize, improve performance, make code faster,
  reduce memory or CPU usage, fix high latency, fix slow queries, improve scalability, reduce
  database load, or shares profiling output. Also trigger when the user says "this is slow",
  "latency is high", "too many DB calls", "high memory usage", or pastes a flame graph or
  query plan. Use proactively when reviewing code that has obvious N+1 queries, unbounded
  result sets, or blocking I/O in async contexts.
---

# Code Optimization

---

# MANDATORY: What Must Never Change

Before touching any code, confirm these are unchanged after optimization:

| Constraint | Check |
|---|---|
| Business logic | ✓ |
| API contracts (request/response shape) | ✓ |
| Validation rules | ✓ |
| Exception types and messages | ✓ |
| Transaction boundaries | ✓ |
| Security and authorization behavior | ✓ |
| Result ordering | ✓ |
| Caching behavior (unless explicitly requested) | ✓ |

If an optimization would require changing any of the above — **stop and flag it** instead of proceeding.

---

# Optimization vs Readability: The Golden Rule

**Optimization and code quality are not opposites — they are both required.**

Every optimization must satisfy this hierarchy in order:

```
1. Correct       — behavior unchanged, tests pass
2. Readable      — a colleague can understand it in 30 seconds
3. Maintainable  — can be safely changed 6 months from now
4. Fast          — measurably faster on the actual bottleneck
```

**Never sacrifice 1–3 to achieve 4.** If an optimization makes code harder to understand, it must include a clear comment explaining the trade-off and why it was necessary. If there is a readable alternative within 20% of the performance gain, prefer the readable version.

## Readability Checks During Optimization

Before finalizing any optimized code, verify:

| Check | Standard |
|---|---|
| Naming | Optimized variables/methods still have intention-revealing names |
| Method length | Optimization didn't bloat a method beyond ~40 lines — extract helpers if needed |
| Magic values | Any new constants (thresholds, batch sizes, cache TTLs) are named, not inline |
| Comments | Non-obvious tricks (bitwise ops, reordered loops, pre-allocated buffers) have a 1-line "why" comment |
| Dead code | Unused original code removed, not just commented out |
| Complexity | Cyclomatic complexity not increased — if it was, flag it |

## When to Reject a "Faster" Solution

Refuse an optimization (and say so) if it:
- Uses obscure language tricks that only work in specific JVM/Python versions without documenting why
- Inlines everything into one method to avoid call overhead — unreadable is never worth microseconds
- Removes abstraction layers (service/repo split) for speed — that's an architectural regression
- Introduces shared mutable state to avoid allocations — thread-safety cost is too high

---

# Step 1: Identify the Bottleneck Category

Never optimize without identifying the category first. Ask for profiling data if not provided.

| Category | Symptoms | Primary Tools |
|---|---|---|
| **CPU** | High utilization, slow processing | cProfile, py-spy, JFR, async-profiler |
| **Memory** | High heap, GC pauses, OOM | tracemalloc, memory_profiler, JVisualVM |
| **Database** | Slow queries, high DB CPU, N+1 | EXPLAIN ANALYZE, Hibernate stats, slow query log |
| **Network** | High API latency, slow external calls | Distributed traces, network timing |
| **I/O** | Slow file ops, disk saturation | iostat, buffered reads |
| **Concurrency** | Thread contention, lock waits | Thread dumps, lock profilers |

---

# Python Optimization

## Algorithmic

- Replace `O(n²)` list membership checks with `set` or `dict` lookups → `O(1)`
- Use generators (`yield`) and `itertools` instead of materializing full lists in memory
- Prefer `collections.Counter`, `defaultdict`, `deque` over manual equivalents
- Avoid repeated list traversals — combine into a single pass where possible
- Use `bisect` for sorted-list lookups instead of linear search

```python
# Before — O(n²)
result = [x for x in big_list if x not in other_list]

# After — O(n), set lookup is O(1)
other_set = set(other_list)
result = [x for x in big_list if x not in other_set]
```

## NumPy / Pandas

- Vectorize operations — avoid Python-level `for` loops over arrays
- Use boolean masking instead of filtered list comprehensions
- Avoid `DataFrame.apply()` — prefer built-in vectorized column operations
- Avoid `.copy()` unless mutation safety requires it — use views
- Use `pd.Categorical` for low-cardinality string columns to reduce memory
- Use `chunksize` in `read_csv` / DB reads for large datasets

```python
# Before — row-by-row apply
df["discounted"] = df["price"].apply(lambda p: p * 0.9 if p > 100 else p)

# After — vectorized
df["discounted"] = df["price"].where(df["price"] <= 100, df["price"] * 0.9)
```

## Concurrency

| Workload | Use |
|---|---|
| I/O-bound (API calls, DB, files) | `asyncio` + `aiohttp` / `asyncpg` |
| Mixed I/O with sync libs | `concurrent.futures.ThreadPoolExecutor` |
| CPU-bound | `multiprocessing` / `ProcessPoolExecutor` |
| Parallel ML inference | `ProcessPoolExecutor` or dedicated worker pool |

**Avoid**: calling sync blocking functions inside `async def` — always offload with `loop.run_in_executor`.

## Memory

- Use `__slots__` on classes with thousands of instances
- Prefer `array` or `numpy` arrays over lists of numbers
- Use `lru_cache` / `cache` (functools) for pure functions called repeatedly with the same args
- Profile with `tracemalloc` or `memory_profiler` before assuming memory is the bottleneck
- Stream large files line-by-line instead of loading fully into memory

## Python-Specific Traps to Avoid

| Trap | Fix |
|---|---|
| `+= ` string concat in loop | Use `"".join(parts)` |
| Re-importing inside loops | Import once at module level |
| `len(list) == 0` | Use `if not list:` |
| `dict.keys()` iteration with membership check | Use `key in dict` directly |
| Regex compilation inside loops | Compile once with `re.compile()` |

## Quality Smells to Fix While Optimizing (Python)

If any of these exist in code being optimized, fix them as part of the same pass:

| Smell | Fix |
|---|---|
| Single-letter variable names in optimized paths | Rename: `i`, `x`, `d` → `index`, `item`, `score_dict` |
| Optimized logic with no comment | Add a 1-line `# why` comment for anything non-obvious |
| Magic threshold values | Extract to named constant: `SCORE_THRESHOLD = 0.72` |
| Long optimized function (>30 lines) | Extract named helpers — a fast function can still be readable |
| Chained one-liners that save lines but lose clarity | Unchain; clarity > line count |

---

# Java / Spring Boot Optimization

## Algorithmic & Collections

```java
// Bad — O(n) lookup inside O(n) loop = O(n²)
for (Long id : idsToCheck) {
    if (userList.contains(id)) { ... }  // List.contains = O(n)
}

// Good — O(1) lookup
Set<Long> userSet = new HashSet<>(userList);
for (Long id : idsToCheck) {
    if (userSet.contains(id)) { ... }
}
```

- Prefer `HashMap`, `HashSet`, `EnumMap` over `List` for repeated lookups
- Use `StringBuilder` (not `+=`) for string building in loops
- Avoid excessive intermediate Stream collections — chain operations lazily
- Prefer primitive arrays (`int[]`) over `List<Integer>` in hot paths

## JVM & Memory

- Avoid object creation in hot paths — reuse instances or use object pools
- Use primitive types (`int`, `long`) over boxed types (`Integer`, `Long`) in tight loops
- Profile GC behavior before tuning — don't change heap flags based on intuition
- Use `record` types (Java 16+) for immutable value objects — compact and allocation-friendly

## Spring Boot: Safe Optimizations

### Read-Only Transactions
```java
@Transactional(readOnly = true)  // skips dirty checking, reduces flush overhead
public List<ItemDto> getItems(Long userId) { ... }
```

### DTO Projections (avoid loading full entities)
```java
// Bad — loads entire entity
List<User> users = userRepo.findAll();

// Good — only select needed fields
@Query("SELECT new com.app.dto.UserSummary(u.id, u.name) FROM User u")
List<UserSummary> findUserSummaries();
```

### `@Cacheable` — Use Only When All Are True
- [ ] Operation is expensive (DB, external API, computation)
- [ ] Workload is read-heavy
- [ ] Data staleness is acceptable
- [ ] Cache invalidation strategy is defined

```java
@Cacheable(value = "recommendations", key = "#userId + ':' + #rail")
public List<ItemDto> getRecommendations(Long userId, String rail) { ... }
```

### `@Async` — Use Only When All Are True
- [ ] Execution is independent (no shared mutable state)
- [ ] No transaction dependency required
- [ ] Result ordering is not required
- [ ] Backed by a configured thread pool (not `SimpleAsyncTaskExecutor`)

```java
@Async("recommendationTaskExecutor")
public CompletableFuture<Void> triggerModelRefresh(Long userId) { ... }
```

## JPA / Hibernate

### Eliminate N+1

```java
// Bad — triggers 1 query for users + N queries for each user's orders
List<User> users = userRepo.findAll();
users.forEach(u -> process(u.getOrders()));

// Good — single JOIN FETCH
@Query("SELECT u FROM User u JOIN FETCH u.orders WHERE u.active = true")
List<User> findActiveUsersWithOrders();

// Alternative — EntityGraph (avoids modifying query)
@EntityGraph(attributePaths = {"orders"})
List<User> findByActive(boolean active);
```

### Pagination — Never Load Unbounded Lists

```java
// Bad
List<Item> all = itemRepo.findAll();

// Good
Page<Item> page = itemRepo.findAll(PageRequest.of(0, 50, Sort.by("score").descending()));
```

### Batch Writes

```properties
# application.properties
spring.jpa.properties.hibernate.jdbc.batch_size=50
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
```

```java
// Use saveAll() — not save() in a loop
itemRepo.saveAll(items);
```

### Avoid SELECT * in Native Queries

```java
// Bad
@Query(value = "SELECT * FROM items WHERE rail = :rail", nativeQuery = true)

// Good
@Query(value = "SELECT id, title, score FROM items WHERE rail = :rail", nativeQuery = true)
```

## Spring Boot: Traps to Avoid

| Trap | Fix |
|---|---|
| `FetchType.EAGER` on associations | Switch to `LAZY` + explicit `JOIN FETCH` |
| Transactions on read-only service methods | Add `readOnly = true` |
| `save()` in a loop | Use `saveAll()` with batch config |
| `Optional.get()` without `isPresent()` | Use `orElseThrow()` |
| Unbounded `findAll()` | Add `Pageable` |
| `@Async` with default executor | Configure a named `ThreadPoolTaskExecutor` |

## Quality Smells to Fix While Optimizing (Java)

If any of these exist in code being optimized, fix them in the same pass:

| Smell | Fix |
|---|---|
| Meaningless names on optimized vars | Rename: `map`, `list2`, `tmp` → `userIndexById`, `filteredOrders` |
| Magic numbers (batch size, cache TTL, thresholds) | Extract to `private static final` constant |
| Optimized logic crammed into one method | Extract named private methods — performance doesn't require unreadability |
| Missing `@Transactional(readOnly = true)` on reads | Add it — it's both a correctness and performance fix |
| `// TODO` or stale comments left from original code | Remove them — don't carry dead commentary into optimized code |
| Stream pipeline longer than ~5 operations | Extract intermediate steps into named variables or helper methods |

---

# Output Format

Every optimization response must include all five sections:

## 1. Bottleneck Analysis

- **Category**: CPU / Memory / Database / Network / I/O
- **Root Cause**: What exactly is slow and why
- **Business Logic Impact**: None / [describe if any]

## 2. Optimized Code

Provide the **complete** method or class — not a partial diff.

## 3. Why It's Faster

| Metric | Before | After |
|---|---|---|
| Complexity | O(n²) | O(n) |
| DB queries | N+1 | 1 |
| Memory allocations | High | Low |

Expected improvement (order of magnitude if estimable).

## 4. Trade-Offs

Be explicit about what you gave up:
- Readability cost (if any)
- Memory increase for speed gain
- Cache invalidation complexity added
- Thread-safety considerations introduced

## 5. Code Quality & Readability Report

| Check | Status |
|---|---|
| Variable/method names are intention-revealing | ✓ / ✗ |
| Magic values extracted to named constants | ✓ / ✗ |
| Non-obvious optimizations have a "why" comment | ✓ / ✗ |
| Method length still reasonable (≤40 lines) | ✓ / ✗ |
| No dead/commented-out original code left behind | ✓ / ✗ |
| Readability trade-off (if any) | [describe or "None"] |

## 6. Verification Checklist

```
✓ Business logic unchanged
✓ API contract unchanged
✓ Validation rules unchanged
✓ Exception behavior unchanged
✓ Transaction behavior unchanged
✓ Security behavior unchanged
✓ Result ordering unchanged
✓ Functional output unchanged
```