---
name: api-standards
description: >
  Generic REST API design standards for any project. Apply when creating or changing endpoints,
  request/response DTOs, error responses, or status codes. Triggers on "add an endpoint",
  "design the API", "REST", "controller", "error response".
globs: "*Controller.java,*Request.java,*Response.java,*.route.ts,*.controller.ts"
alwaysApply: true
---

# API Standards

## Structure & versioning

- Group endpoints under a stable base path (e.g. `/api`).
- Version public APIs so contracts can evolve without breaking clients.
- Keep contracts stable: add fields or endpoints; do not change or remove existing ones.

## Resource conventions

- Use nouns for resources and HTTP methods for actions:
  `GET` read, `POST` create, `PUT` replace, `PATCH` partial update, `DELETE` remove.
- Model actions that are not plain CRUD as sub-resources (e.g. a `.../transitions` or `.../actions` collection) rather than verbs in the path.
- List endpoints are always paged and never return unbounded results.
- Filtering and free-text search are query parameters on the list endpoint.

## Status codes

`200` read/update ok · `201` created · `204` no content · `400` malformed/validation · `401` unauthenticated · `403` unauthorized · `404` not found · `409` conflict (including state that conflicts with the request) · `422` semantically invalid · `500` unexpected.

Choose the code that matches the cause. A well-formed request that conflicts with current state is `409`, not `400`.

## Request DTOs

- Validate every field declaratively at the boundary.
- Never accept a persistence entity directly as a request body.
- Reject unknown or invalid enum values with a clear message.

## Error response — one consistent shape

Every error returns the same structure from a single global handler:

- a stable machine-readable `code` (UPPER_SNAKE_CASE),
- a human-readable `message` safe to display,
- a `timestamp`.

Never leak stack traces, SQL, or internal class names to clients.

## Success response conventions

- Expose stable identifiers; avoid leaking internal implementation detail.
- Use ISO-8601 UTC timestamps.
- Include pagination metadata on list responses (content, page, size, total).

## Documentation

- Annotate every endpoint with OpenAPI (summary + response codes).
- Document any non-obvious behavioral contract the endpoint guarantees.
