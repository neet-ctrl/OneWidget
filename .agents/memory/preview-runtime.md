---
name: Preview artifact runtime
description: Runtime setup for isolated visual-preview artifacts in this workspace
---

The component-preview artifact workflow expects its declared JavaScript runtime and dependencies to be available when the workflow runs from the artifact directory. In this workspace, package installation may provision the shared runtime at the workspace root, so the artifact must still resolve that runtime locally.

**Why:** The preview workflow can report `vite: not found` even after the package manager successfully restored dependencies when the install location and workflow working directory differ.

**How to apply:** When restoring a component-preview artifact, verify both the workflow command and the artifact-local module resolution before diagnosing the UI code.