---
name: GitHub release access
description: Connector visibility failures need not mean GitHub authorization is broken; use the existing authenticated CLI.
---

A GitHub connector can report authorized while the sandbox connection list is empty. In this workspace, the existing GitHub CLI successfully accessed the account and releases despite that failure.

**Why:** Treating the empty connector list as broken OAuth sent the user through unnecessary reconnect instructions. Their UI exposed a Repl-provided-credentials connector with no reconnect option, while Git sync and CLI access worked.

**How to apply:** Before asking the user to change GitHub settings, try a non-secret authenticated CLI read such as `gh api user --jq .login`. Never print tokens or delete/revoke a working connection to fix a connector-path failure. Preserve GitHub-only source changes; release tags can identify the exact build source without overwriting the remote default branch.

Working CLI authentication and release metadata access do not establish binary-upload support. Here, source/tag pushes and draft creation succeeded, but `uploads.github.com` rejected both APK and PDF uploads with HTTP 400 through the normal release uploader and explicit API requests.

**Why:** The separate upload host/transport can fail even when account and repository permissions are sufficient for other operations. Assuming CLI reads proved complete release access led to an overly confident claim.

**How to apply:** Verify the complete asset-upload path before calling release automation working. Keep failed releases unpublished, distinguish upload failures from OAuth failures, and do not repeatedly ask the user to reconnect a functioning account.
