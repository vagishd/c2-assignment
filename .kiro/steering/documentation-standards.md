---
name: documentation-standards
description: >
  Generic documentation standards for any project. Apply when writing specs, README files,
  architecture notes, ADRs, or code-level docs. Triggers on "document this", "write the docs",
  "update the README", "explain the design", "write a spec".
alwaysApply: false
---

# Documentation Standards

## Principles

- Document decisions and their rationale, not just the outcome: what was chosen, why, and what was rejected.
- Keep docs close to the code. Specifications live in a `spec/` folder; design decisions that shape the code live with the architecture doc.
- Write for a teammate joining next week, not for the author today.
- Prefer short, scannable prose and tables over long paragraphs. Use diagrams (ASCII/Mermaid) for flows and state.

## Spec-driven development

Create specifications before implementation. A typical spec set:

- requirements — what is being built and its acceptance criteria
- architecture — components, technology choices, and their justification
- data-model — entities, fields, relationships
- api-contract — endpoints, request/response shapes, error contract
- test-strategy — what is verified at each layer

Add further specs as the domain needs them (state machines, data pipelines, UI flows, evaluation strategy). Keep each spec focused on one concern.

## AI / model-backed features

When a project includes AI or model-backed behavior, the following must be documented and justified — not left implicit:

- the strategy for turning source data into retrievable/knowledge units,
- the embedding or model choice, with the cost / latency / quality tradeoff considered,
- retrieval and generation defaults, and confirmation that they are configurable rather than hardcoded,
- the grounding contract that constrains the model to provided context,
- a running log of real AI mistakes caught during development, how each was detected, and how it was fixed.

## Code-level docs

- Public functions with non-obvious behavior get a short doc comment explaining intent and failure modes.
- Non-obvious logic gets a one-line "why" comment; obvious code gets none — rename instead of narrating.
- Remove comments that merely restate the code.

## Secrets

- Never commit credentials. Reference them via environment variables and document the variable names, never the values.
