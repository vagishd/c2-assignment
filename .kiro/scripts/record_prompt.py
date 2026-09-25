#!/usr/bin/env python3
"""Record every submitted prompt to the repo's prompt history.

Invoked by the `record-prompt-history` Kiro hook on UserPromptSubmit. Kiro passes session
context as JSON on stdin. We extract the prompt text defensively (field names can vary across
Kiro versions) and append it to:

  - docs/prompt-history.md            a single human-readable running log
  - .specstory/history/<date>.md      a per-day SpecStory-style transcript

The script never fails the prompt submission: any error is swallowed with exit code 0.
"""
import json
import os
import sys
from datetime import datetime, timezone


def project_root() -> str:
    # KIRO_PROJECT_ROOT is set by the hook runtime; fall back to two levels up from this script.
    return os.environ.get(
        "KIRO_PROJECT_ROOT",
        os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..")),
    )


def extract_prompt(payload: dict) -> str:
    """Pull the prompt text out of the hook payload regardless of exact schema."""
    for key in ("prompt", "userPrompt", "message", "text", "input"):
        value = payload.get(key)
        if isinstance(value, str) and value.strip():
            return value.strip()
    # Some versions nest it under a container object.
    for key in ("data", "context", "event"):
        nested = payload.get(key)
        if isinstance(nested, dict):
            found = extract_prompt(nested)
            if found:
                return found
    return ""


def main() -> int:
    try:
        raw = sys.stdin.read()
        payload = json.loads(raw) if raw.strip() else {}
    except Exception:
        payload = {}

    prompt = extract_prompt(payload) if isinstance(payload, dict) else ""
    if not prompt:
        # Nothing usable to record; do not block the prompt.
        return 0

    root = project_root()
    now = datetime.now(timezone.utc)
    stamp = now.strftime("%Y-%m-%d %H:%M:%S UTC")
    day = now.strftime("%Y-%m-%d")

    docs_dir = os.path.join(root, "docs")
    specstory_dir = os.path.join(root, ".specstory", "history")
    os.makedirs(docs_dir, exist_ok=True)
    os.makedirs(specstory_dir, exist_ok=True)

    entry = f"\n## {stamp}\n\n> {prompt}\n"

    running_log = os.path.join(docs_dir, "prompt-history.md")
    if not os.path.exists(running_log):
        with open(running_log, "w", encoding="utf-8") as handle:
            handle.write("# Prompt History\n\n"
                         "Automatically recorded by the `record-prompt-history` hook.\n")
    with open(running_log, "a", encoding="utf-8") as handle:
        handle.write(entry)

    session_log = os.path.join(specstory_dir, f"{day}.md")
    if not os.path.exists(session_log):
        with open(session_log, "w", encoding="utf-8") as handle:
            handle.write(f"# Session transcript — {day}\n")
    with open(session_log, "a", encoding="utf-8") as handle:
        handle.write(entry)

    return 0


if __name__ == "__main__":
    sys.exit(main())
