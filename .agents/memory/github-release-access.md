---
name: GitHub release access
description: Connector visibility failures need not mean GitHub authorization is broken; use the existing authenticated CLI.
---

A GitHub connector can report authorized while the sandbox connection list is empty. In this workspace, the existing GitHub CLI successfully accessed the account and releases despite that failure.

**Why:** Treating the empty connector list as broken OAuth sent the user through unnecessary reconnect instructions. Their UI exposed a Repl-provided-credentials connector with no reconnect option, while Git sync and CLI access worked.

**How to apply:** Before asking the user to change GitHub settings, try a non-secret authenticated CLI read such as `gh api user --jq .login`. Never print tokens or delete/revoke a working connection to fix a connector-path failure. Preserve GitHub-only source changes; release tags can identify the exact build source without overwriting the remote default branch.
