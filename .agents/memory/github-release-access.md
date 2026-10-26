---
name: GitHub release access
description: Connector visibility failures need not mean GitHub authorization is broken; use the existing authenticated CLI.
---

A GitHub connector can report authorized while the sandbox connection list is empty. In this workspace, the existing GitHub CLI successfully accessed the account and releases despite that failure.

**Why:** Treating the empty connector list as broken OAuth sent the user through unnecessary reconnect instructions. Their UI exposed a Repl-provided-credentials connector with no reconnect option, while Git sync and CLI access worked.

**How to apply:** Before asking the user to change GitHub settings, try a non-secret authenticated CLI read such as `gh api user --jq .login`. Never print tokens or delete/revoke a working connection to fix a connector-path failure. Preserve GitHub-only source changes unless the user explicitly asks to replace them with the Replit version.

Working CLI authentication and release metadata access do not establish binary-upload support. Here, source/tag pushes and draft creation succeeded, but `uploads.github.com` rejected both APK and PDF uploads with HTTP 400 through the normal release uploader and explicit API requests.

**Why:** The separate upload host/transport can fail even when account and repository permissions are sufficient for other operations. Assuming CLI reads proved complete release access led to an overly confident claim.

**How to apply:** Verify the complete asset-upload path before calling release automation working. Keep failed releases unpublished, distinguish upload failures from OAuth failures, and do not repeatedly ask the user to reconnect a functioning account.

Use GitHub-hosted Actions for automated Android build/upload rather than sending binary assets through the failing Replit upload path. This route succeeded end to end, with published downloads and the original signing identity independently verified.

**Why:** Source pushes and workflow dispatch work from Replit even when binary upload requests fail. Running the build/upload on GitHub avoids that transport dependency and does not require a personal access token.

**How to apply:** Retain the original signing identity in encrypted Actions secrets, require new versions for app/guide changes under existing tags, and download-verify artifacts before publishing. A working source push alone is not proof of a successful release.

Explicitly select the restored keystore in Gradle before Android signing configuration is finalized instead of assuming the runner's default keystore directory.

**Why:** A restored key's public certificate matched the pinned identity, but the first runner build selected a different default keystore. Setting the signing path after project evaluation was too late because Android Gradle had finalized it.

**How to apply:** Configure the key when the Android plugin is applied and keep the pinned-certificate check intact. Do not weaken that check or regenerate a key to make CI pass.
