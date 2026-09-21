---
name: command-check-hallucination
description: Reusable command to audit AI assistant output for hallucination and ungrounded claims. Pull in manually with # when reviewing a generated answer.
inclusion: manual
---

# Command: Check AI Output for Hallucination

Audit an AI-generated answer for grounding. The answer, the question, and the retrieved
context/sources should be in scope. Do not rewrite the answer unless asked — verify it.

For every factual claim in the answer:

1. Trace it to a source — find the specific retrieved document/record that supports it.
   - Supported → OK.
   - Not supported by any provided source → flag as UNGROUNDED (hallucination).
   - Contradicts a source → flag as CONTRADICTION.
2. Check citations — every answer that makes a claim must cite the specific source(s) it used.
   Flag missing, wrong, or fabricated citations.
3. Check abstention — if no source is relevant, the answer must explicitly say so. Flag any
   confident answer produced from no supporting context.
4. Check scope — flag answers that fall back on general/model knowledge for domain-specific
   questions instead of using only the provided context.
5. Check overreach — flag hedged-but-invented specifics (numbers, ids, names, dates) not present
   in the sources.

Report a table: claim → supporting source (or NONE) → verdict (grounded / ungrounded /
contradiction / uncited). End with an overall verdict: grounded, or not-grounded with the list
of offending claims and the corrected, source-backed version (or an explicit no-answer) to use instead.
