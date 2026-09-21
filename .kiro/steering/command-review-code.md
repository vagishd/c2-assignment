---
name: command-review-code
description: Reusable code-review command. Pull in manually with # when you want a structured review of a change or file.
inclusion: manual
---

# Command: Review Code

Perform a structured review of the code in context (a diff, a file, or a selection). Do not
change code unless explicitly asked — produce findings.

Review in this order and report findings grouped by severity (Blocker / Major / Minor / Nit):

1. Correctness — logic, edge cases, error handling, off-by-one, null/empty handling, concurrency.
2. Contracts — public API, request/response shape, and behavior preserved unless the change intends otherwise.
3. Design & layering — responsibilities in the right place, no logic leaking across layers, SOLID respected.
4. Readability — intention-revealing names, function length, nesting depth, magic values extracted.
5. Security — input validation, no injection via string-built queries/commands, no secrets in code or logs.
6. Performance — obvious hot-path issues, N+1 access, unbounded result sets, needless allocation.
7. Tests — meaningful coverage for the change; new branches exercised; assertions are real.

For each finding give: file/line, what's wrong, why it matters, and a concrete fix. End with a short
summary and an explicit verdict: approve, approve-with-nits, or request-changes.
