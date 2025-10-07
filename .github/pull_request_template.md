---
name: PR Checklist
about: Ensure code is polished, tested, and ready for public consumption
title: "[PR] "
labels: ""
assignees: "@minademian"

---

## PR Purpose
<!-- Brief description of what this PR does -->

---

## Checklist Before Merging ✅

### 1️⃣ Code Quality
- [ ] Consistent code style (indentation, spacing, naming)
- [ ] Method-level comments / Javadoc for public APIs
- [ ] No unused imports or debug prints

### 2️⃣ Tests & CI
- [ ] Unit tests for core logic (happy path + edge cases)
- [ ] Integration tests for CLI / file conversion / output correctness
- [ ] CI runs successfully on at least one Linux/macOS environment

### 3️⃣ Documentation & UX
- [ ] README includes quickstart example (3–5 commands)
- [ ] Demo snippet, screenshot, or GIF showing input → output
- [ ] Error messages are clear and actionable
- [ ] Exit codes defined for automation-friendly usage

### 4️⃣ Packaging & Distribution
- [ ] Prebuilt binaries / artifacts available (GitHub Releases optional)
- [ ] Docker / container instructions included (if applicable)
- [ ] Dependencies documented (minimum JDK, libraries, etc.)

### 5️⃣ OSS Signals
- [ ] LICENSE present (MIT, Apache 2.0, or similar)
- [ ] `Contributing.md` exists with instructions for PRs / issues
- [ ] CHANGELOG or version notes updated for this release

---

## Screenshots / Logs
<!-- Include any demo output, screenshots, or relevant logs -->

---

## Notes / Next Steps
<!-- Any additional context or follow-ups -->
