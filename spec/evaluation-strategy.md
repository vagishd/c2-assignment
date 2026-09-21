# Evaluation Strategy — AI / RAG

Deterministic logic is covered in test-strategy.md. This document covers how the probabilistic AI
feature is evaluated for retrieval quality and grounding.

## What we evaluate

| Dimension | Question | How |
|---|---|---|
| Retrieval relevance | Does the expected ticket appear in top-K for a known question? | Labelled fixture set, assert recall@K |
| Grounding | Does the answer use only retrieved tickets? | Mocked ChatModel; assert prompt carries only retrieved context |
| Citation | Are the used ticket IDs returned in `sources`? | Assert `sources` non-empty and match retrieved ids |
| Honest abstention | Does an out-of-scope question return no-match, not a fabrication? | Query with no matching data; assert `grounded=false`, empty sources, no LLM call |
| Threshold behavior | Does a weak match get filtered out? | Query just below threshold; assert abstention |

## Fixture set
A small labelled dataset (question → expected ticketId(s)) built from the seeded sample tickets,
covering the in-scope example questions (payment failures, resolution of a specific ticket, shipment
tracking causes, similar resolved tickets, high-priority payment tickets).

## Method
- Retrieval tests run against the real embedding model + in-memory store using seeded data, asserting
  the expected ticket is retrieved — not exact answer wording.
- Grounding/citation/abstention tests mock the `ChatModel`/`ChatClient` so the assembly and guardrail
  logic is deterministic, and assert on structure (context contents, sources, grounded flag).
- No live external model calls in CI; live tests (if any) are tagged and excluded by default.

## Metrics
- recall@K on the fixture set (target: expected ticket present in top-K for every in-scope question).
- abstention correctness: 100% of out-of-scope questions must abstain.

## AI mistakes log (NFR-5)
Maintain a running list of real AI mistakes caught during development. Each entry:
- what the AI produced (code or RAG answer),
- how it was detected (test, review, manual check),
- the root cause,
- the fix.

### Entry 1 — Missing vector-store dependency (AI-generated code mistake)
- **What the AI produced:** The initial `build.gradle` and `VectorStoreConfig` assumed the Spring AI
  model starters (`spring-ai-starter-model-ollama` / `-openai`) would transitively provide the
  `org.springframework.ai.vectorstore` package (`VectorStore`, `SimpleVectorStore`, `Document`).
- **How it was detected:** `./gradlew compileJava` failed with "package org.springframework.ai.vectorstore
  does not exist". A dependency tree check confirmed the model starters pull `spring-ai-commons`,
  `spring-ai-model`, and `spring-ai-client-chat`, but NOT the vector-store module.
- **Root cause:** Over-generalized assumption about transitive dependencies in Spring AI 1.0's
  modular layout.
- **Fix:** Added `implementation 'org.springframework.ai:spring-ai-vector-store'` (version managed by
  the BOM). Recompiled clean.

### Entry 2 — Grounding / hallucination guardrail (RAG answer risk)
- **Risk observed:** With a permissive prompt, an LLM will answer support questions from general
  knowledge when retrieval returns weak or no context, producing plausible but ungrounded answers.
- **How it was detected/anticipated:** Reviewing the ask flow against FR-15/FR-17 and the
  command-check-hallucination steering.
- **Fix implemented:** `AskService` short-circuits to the honest no-match response and never calls the
  LLM when similarity search returns nothing above the configured threshold; and the system prompt
  restricts the model to the provided ticket context only, instructing it to return the exact
  no-match sentence otherwise. Sources are always the retrieved tickets, so citations cannot be
  fabricated.

### Entry 3 — ticket_ref NOT NULL violation (AI-generated code mistake)
- **What the AI produced:** `TicketService.create` and `SampleDataSeeder` saved a ticket, then set
  `ticketRef` from the generated id and saved again. With `ticket_ref` declared NOT NULL, the first
  insert violated the constraint under strict DDL (surfaced as a `DataIntegrityViolationException`
  when running the integration tests on H2 `create-drop`).
- **How it was detected:** State-machine integration tests failed to load context / persist.
- **Root cause:** Deriving the reference from the id forced a two-phase save with a null value in
  a non-null column.
- **Fix:** Switched id generation to a sequence and assigned `ticketRef` in a `@PrePersist` hook, so
  the reference is present on the first (single) insert. Removed the second save.

### Entry 4 — LazyInitializationException on read (AI-generated code mistake)
- **What the AI produced:** `GET /api/tickets/{id}` returned 500. The mapper accessed the lazy
  `comments` collection after the read transaction had closed (`open-in-view: false`).
- **How it was detected:** Integration test asserted `$.status` on the GET response and got a 500
  with `INTERNAL_ERROR`; logs showed the lazy-init failure.
- **Root cause:** Mapping a lazily-loaded association outside an open session.
- **Fix:** Added `findWithCommentsById` using an `@EntityGraph(attributePaths = "comments")` and
  routed the service read path through it, so comments are fetched before mapping.

> Add further entries as real cases are observed while running against a live model.
