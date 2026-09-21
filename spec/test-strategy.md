# Test Strategy

Follows the testing-standards steering. Deterministic logic is tested exhaustively; probabilistic
AI output is tested by properties (see evaluation-strategy.md).

## Layers

### Unit
- `TicketStatus` state machine: every allowed transition returns true; every disallowed transition
  returns false. Exhaustive over all state pairs.
- Mappers (entity ↔ DTO).
- Knowledge-document builder (correct text + metadata for a ticket).
- RAG assembly logic with a mocked ChatModel (grounding, citation, abstention).

### Slice / data
- `@DataJpaTest` for search (keyword) and filter (status) repository queries against H2.
- `@WebMvcTest` for controller validation → 400 with `VALIDATION_FAILED`.

### Integration (`@SpringBootTest` + MockMvc)
- State machine end-to-end: create → transition through valid path; attempt invalid transitions →
  409 `INVALID_TRANSITION`, state unchanged. (NFR-4 — these MUST pass.)
- Persistence: create then read back; (restart durability is provided by H2 file mode, asserted by
  reading seeded/committed data).
- Full request→response for create, list, view, update, comment.

### AI / RAG evaluation
- Retrieval recall@K on the labelled fixture set.
- Grounding: prompt contains only retrieved context (mocked model).
- Citation: `sources` reflects retrieved ticket ids.
- Abstention: out-of-scope question → `grounded=false`, empty sources, LLM not called.

## Key test cases (state machine — the deterministic core)
| From | To | Expected |
|---|---|---|
| OPEN | IN_PROGRESS | pass |
| OPEN | CANCELLED | pass |
| IN_PROGRESS | RESOLVED | pass |
| IN_PROGRESS | CANCELLED | pass |
| RESOLVED | CLOSED | pass |
| CLOSED | OPEN | reject 409 |
| RESOLVED | OPEN | reject 409 |
| CANCELLED | OPEN | reject 409 |
| OPEN | RESOLVED | reject 409 |
| OPEN | CLOSED | reject 409 |
| CLOSED | * | reject 409 |
| CANCELLED | * | reject 409 |

## CI rules
- No real external model calls in CI (mock or tag-excluded).
- Tests independent and order-free; H2 rolled back per test except durability checks.
