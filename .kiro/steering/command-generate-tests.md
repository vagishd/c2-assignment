---
name: command-generate-tests
description: Reusable test-generation command. Pull in manually with # to generate tests for code or a feature following the project testing standards.
inclusion: manual
---

# Command: Generate Tests

Generate tests for the code or feature in context. Follow the project testing standards.

Before writing:
1. Identify the unit under test and its dependencies (mock external ones).
2. Enumerate the scenarios: happy path, each validation/error path, boundary values, and — for
   any state machine — every valid transition and every invalid transition.
3. For probabilistic/AI output, test properties (grounding, presence of citations/sources, honest
   abstention on out-of-scope input, retrieval recall@K) — never exact generated text. Mock the model.

When writing:
- Use the naming convention that states scenario and expected outcome.
- One logical assertion per test where practical; assert the error contract, not just the status.
- Keep tests independent and order-free; clean up state; no real external model calls in CI.
- Cover new branches introduced by the change; do not pad with trivial assertions.

After writing, run the suite and report pass/fail. If anything fails, fix the code or the test and
state which, then re-run until green.
