---
name: Video scaffold validation
description: Browser-library omission in generated video TypeScript settings can produce misleading animation type errors.
---

Check browser TypeScript libraries before changing otherwise valid video code when a generated artifact reports missing `window`/`document` alongside Framer Motion type errors.

**Why:** The new video scaffold inherited backend-only libraries. Adding `es2022,dom,dom.iterable` as compiler flags made the unchanged scaffold and film pass type-checking; the production build and real browser playback also passed. Scene rewrites would have addressed the wrong problem.

**How to apply:** Preserve protected playback code. When scaffold configuration is read-only, validate with explicit browser-library flags. A separately authorized configuration fix should be scoped to the video artifact, not global backend TypeScript settings.
