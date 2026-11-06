---
description: Git commit and PR standards
paths:
  - "**/*"
---

# Git Standards

## Commit Messages
- Follow Conventional Commits format: `<type>: <short description>`
- Types: `feat`, `fix`, `chore`, `refactor`, `test`, `docs`
- Description must be lowercase, imperative tense, no period at end.

Example:
```
feat: add search endpoint
fix: return 404 when resource not found
test: add integration test for search
```

- All commits made by Claude must include a `Co-authored-by` trailer.

Example:
```
feat: add search endpoint

Co-Authored-By: Claude <noreply@anthropic.com>
```

## Branch Naming
- One feature branch per task: `feature/task-{id}` (e.g. `feature/task-123`).
- Before creating the branch, check if it already exists. If yes, check it out without recreating it.

Example:
```bash
# Try to check out — if branch exists, done
git checkout feature/task-123

# If it does not exist, create from main
git checkout main
git checkout -b feature/task-123
```

## Pull Requests
- One PR per task, raised from `feature/task-{id}` to `main` after all tasks are complete.
- Title must follow the same Conventional Commits format as commit messages.
- Body must reference the task issue number.

Example:
```
feat: add search endpoint

Closes #123
```
