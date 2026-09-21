---
name: command-review-spec
description: Reusable spec-review command. Pull in manually with # to review a specification for completeness and testability before implementation.
inclusion: manual
---

# Command: Review Spec

Review the specification in context before any code is written. Do not implement — assess the spec.

Check for:

1. Completeness — are all required features and acceptance criteria captured and unambiguous?
2. Testability — is every requirement stated so it can be verified? Flag anything not measurable.
3. Consistency — do the specs agree with each other (data model, API contract, state rules, UI flow)?
4. Contracts — are request/response shapes, error responses, and status codes fully specified?
5. State & rules — are all valid transitions and all invalid ones explicitly defined?
6. AI/model concerns (if any) — are chunking, embedding choice, retrieval defaults, grounding rules, and evaluation strategy documented and justified?
7. Gaps & risks — missing edge cases, undefined failure modes, unstated assumptions, security/PII handling.

Report findings grouped by severity. For each, name the spec file and section, state the gap, and
propose the specific addition or clarification needed. End with a readiness verdict:
ready-to-implement, or list the blocking items to resolve first.
