# Codex Subagent Task Templates

This file contains reusable prompt templates for common `ysukampus` tasks. Use these templates when the current task matches one of the patterns below.

Guidelines:
- Reuse the closest template and adapt only the task-specific details.
- Keep repository rules from `AGENTS.md` in force: update `docs/PROJECT_MAP.md` after meaningful changes, keep changes scoped, and do not invent backend behavior.
- Prefer the recommended subagent when the task matches, but keep work in the current agent when delegation would add no value.

## 1. Code Path Mapping

Recommended subagent: `code-mapper`

```text
Map the exact owning path for [feature/bug/task] in ysukampus before making changes. Identify entry point, key composables/ViewModels/repositories/services, state transitions, side effects, and the highest-risk branch points. Keep it concise and evidence-based.
```

## 2. Fast Repository Recon

Recommended subagent: `search-specialist`

```text
Find the highest-signal files and symbols related to [feature/bug/task] in ysukampus. Rank the most relevant hits, explain why they matter, and point to the best next file to inspect.
```

## 3. Admin UI Logic Change

Recommended subagent: `kotlin-specialist`

```text
Implement a minimal logic change for [admin feature/bug] in ysukampus. Preserve the existing admin architecture, keep backend behavior unchanged unless explicitly required, update docs/PROJECT_MAP.md after meaningful edits, and run the appropriate Gradle compile task before handoff.
```

## 4. Compose Layout Fix From Screenshots

Recommended subagent: `ui-fixer`

```text
Fix the Compose UI issue in [screen/component] to better match the design references in design/screenshots and design/*.md. Make the smallest safe patch, preserve the current visual system, keep the change UI-only unless logic must change, and update previews if needed.
```

## 5. UI Decision Before Implementation

Recommended subagent: `ui-designer`

```text
Review the relevant screenshots/specs in design/ and produce implementation-ready guidance for [screen/component]. Focus on layout, spacing, interaction states, component reuse, and any ambiguous details that should be resolved before coding.
```

## 6. ViewModel / State Bug

Recommended subagent: `kotlin-specialist`

```text
Debug and fix the state issue in [ViewModel/state flow/screen] in ysukampus. Preserve existing architecture, identify the exact execution boundary first, implement the smallest safe fix, and validate the changed path plus one failure path.
```

## 7. Registration / Auth / Network Flow Change

Recommended subagent: `kotlin-specialist`

Use `mobile-developer` too when lifecycle, offline, or interruption behavior matters.

```text
Implement or debug the auth/network behavior in [feature/registration|data/auth|data/network|data/session]. Preserve the existing interceptor/token-refresh/session architecture, avoid duplicating refresh logic, keep API assumptions explicit, and validate success plus degraded/error behavior.
```

## 8. Cross-Screen Mobile Flow Issue

Recommended subagent: `mobile-app-developer`

```text
Investigate and fix the mobile flow issue affecting [screens/entry points] in ysukampus. Focus on navigation, lifecycle, state restoration, and user-flow correctness across screen boundaries. Implement the smallest coherent fix and call out any device-level checks still needed.
```

## 9. Gradle / Build / Flavor / Signing Problem

Recommended subagent: `build-engineer`

```text
Diagnose and fix the Gradle/build issue in ysukampus affecting [task/variant/flavor/build type]. Find the root cause, implement the smallest safe fix, preserve current flavor/signing behavior unless the task requires otherwise, and report any remaining CI or environment checks.
```

## 10. Add Regression Tests

Recommended subagent: `test-automator`

```text
Add focused regression coverage for [bug/feature/path] in ysukampus. Keep tests minimal but durable, align them with the actual behavior contract, avoid broad test churn, and explain what risk the new coverage prevents.
```

## 11. Review Current Diff

Recommended subagent: `reviewer`

```text
Review the current ysukampus diff for correctness risks, regressions, security issues, contract mismatches, and missing tests. Prioritize findings by severity with file/line evidence. Keep style commentary out unless it affects maintainability or risk.
```

## 12. Documentation Update

Recommended subagent: `documentation-engineer`

```text
Update the relevant ysukampus documentation for [change/task]. Keep it faithful to the current code and workflows, avoid speculative statements, update docs/PROJECT_MAP.md when required, and keep the resulting docs concise and task-oriented.
```

## 13. Full Admin Task Flow

Recommended subagent order:
1. `code-mapper`
2. `ui-designer` or `kotlin-specialist`
3. `ui-fixer` if needed
4. `test-automator` for coverage
5. `reviewer` before handoff

```text
Handle this admin-panel task in ysukampus end to end: first map the owning path, then implement the smallest safe UI/logic change, update previews/tests/docs as needed, run the relevant Gradle validation, and finish with a risk-focused review.
```

## 14. Safe Default Task Framing

Recommended subagent: choose based on task shape

```text
Work on this ysukampus task conservatively. First identify the target repo line and owning path, then implement the smallest safe change that matches the existing architecture and design system. Update docs/PROJECT_MAP.md after meaningful edits and run the appropriate validation before handoff.
```
