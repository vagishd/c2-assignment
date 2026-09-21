---
name: testing-standards
description: >
  Generic testing standards for any project. Apply whenever writing, reviewing, or generating
  tests. Covers the test pyramid, naming, deterministic logic, and testing probabilistic /
  AI output. Triggers on "write a test", "add tests", "test this", "integration test".
globs: "*Test.java,*Tests.java,*IT.java,*.test.ts,*.test.tsx,*.spec.ts,*_test.py,test_*.py"
alwaysApply: true
---

# Testing Standards

## Test pyramid

| Layer | Purpose |
|---|---|
| Unit | Pure logic in isolation: rules, mappers, validators, calculations |
| Slice / component | A single layer wired up (web layer, data layer, one UI component) |
| Integration | End-to-end flows across layers, including persistence |
| Probabilistic / AI eval | Non-deterministic output quality against a fixed dataset |

Most tests are unit tests. Reserve integration tests for flows that cross boundaries. Keep AI/eval tests a small, deliberate set.

## Naming

Use a convention that states the scenario and expected outcome, for example
`unitUnderTest_condition_expectedBehavior`. Test names should read as a specification.

## Deterministic logic

- Cover every branch of business-critical rules: each valid path passes AND each invalid path is rejected with the correct error.
- Test the error contract (code/message/status), not just that an error occurred.
- Use the framework's data-layer slice for query tests and the full-stack test for request→response flows including validation failures.
- Tests must be independent, order-free, and leave no shared state behind.

## Probabilistic / AI output

You cannot assert exact generated text. Assert the properties that must hold:

| Property | How to assert |
|---|---|
| Grounding | Output references only sources that were actually retrieved/provided |
| Attribution | Citations/sources are present when an answer is produced |
| Honest abstention | Out-of-scope input yields an explicit "no answer / not found" result, not a fabrication |
| Retrieval relevance | For a known query, the expected source appears in the top-K results |

- Mock the model in unit tests so grounding and prompt-assembly logic are deterministic.
- Use a small labelled fixture set (input → expected sources) for retrieval tests; assert recall@K and structure, never exact wording.
- Do not make real external model calls in CI. Mock them, or gate live tests behind a profile/tag.

## Rules

- One logical assertion per test where practical.
- No `sleep`-based waits; use proper synchronization/awaiting utilities.
- Clean up state via rollback or explicit teardown.
- Coverage is a floor, not the goal. Assertions must be meaningful.
