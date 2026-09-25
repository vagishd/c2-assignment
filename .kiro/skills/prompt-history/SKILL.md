---
name: prompt-history
description: >
  Records every user prompt to the repository so there is a durable, reviewable prompt log.
  Use when asked to save/record/log prompts, set up prompt history, or reconstruct the prompt
  transcript. Writes to docs/prompt-history.md and .specstory/history/<date>.md.
---

# Prompt History

Keeps a permanent record of the prompts given to the AI assistant, mirroring what SpecStory does
for Cursor/VS Code.

## How it works

- A Kiro hook, `record-prompt-history` (`.kiro/hooks/record-prompt-history.json`), fires on the
  `UserPromptSubmit` trigger for every prompt.
- The hook runs `.kiro/scripts/record_prompt.py`, which appends the prompt to:
  - `docs/prompt-history.md` — a single running log for humans to read.
  - `.specstory/history/<YYYY-MM-DD>.md` — a per-day transcript, matching the SpecStory layout.
- The script never blocks a prompt: on any error it exits 0.

## Files

```
.kiro/hooks/record-prompt-history.json   the hook definition
.kiro/scripts/record_prompt.py           the recorder
docs/prompt-history.md                   running log
.specstory/history/                      per-day transcripts
```

## Manual use

If you need to add or backfill an entry by hand, append a section to `docs/prompt-history.md` and the
matching `.specstory/history/<date>.md` in the format:

```
## <timestamp>

> <prompt text>
```

## Notes

- The hook activates on the next session start after it is created.
- Do not commit secrets; prompts are recorded verbatim, so avoid pasting credentials into prompts.
