# State Machine

## States
`OPEN, IN_PROGRESS, RESOLVED, CLOSED, CANCELLED`

## Allowed transitions
```
OPEN         → IN_PROGRESS
OPEN         → CANCELLED
IN_PROGRESS  → RESOLVED
IN_PROGRESS  → CANCELLED
RESOLVED     → CLOSED
```

## Diagram
```
        ┌──────────────┐
        │     OPEN     │
        └──────┬───────┘
               │
        ┌──────┴───────┐
        ▼              ▼
  ┌───────────┐   ┌───────────┐
  │IN_PROGRESS│──▶│ CANCELLED │
  └─────┬─────┘   └───────────┘
        │              ▲
        ▼              │
  ┌───────────┐        │
  │ RESOLVED  │        │ (from OPEN too)
  └─────┬─────┘
        ▼
  ┌───────────┐
  │  CLOSED   │
  └───────────┘
```

## Rules
- Any transition not in the allowed list is rejected with `409 INVALID_TRANSITION`; the ticket's
  state is unchanged.
- Terminal states: `CLOSED` and `CANCELLED` have no outgoing transitions.
- `resolutionNotes` may be supplied when moving to `RESOLVED` or `CLOSED`.

## Explicitly rejected examples
| From | To | Result |
|---|---|---|
| CLOSED | OPEN | ❌ 409 |
| RESOLVED | OPEN | ❌ 409 |
| CANCELLED | OPEN | ❌ 409 |
| OPEN | RESOLVED | ❌ 409 (must go through IN_PROGRESS) |
| OPEN | CLOSED | ❌ 409 |
| CLOSED | anything | ❌ 409 (terminal) |
| CANCELLED | anything | ❌ 409 (terminal) |

## Implementation approach
- Allowed transitions defined once as a `Map<TicketStatus, Set<TicketStatus>>` in the domain layer
  (single source of truth).
- `TicketStatus.canTransitionTo(target)` encapsulates the rule; the service calls it before applying
  a change and throws `InvalidTransitionException(from, to)` otherwise.
- This keeps the rule deterministic and exhaustively unit-testable (see test-strategy.md).
- A status change triggers re-ingestion of the ticket into the vector store (see rag-ingestion.md).
