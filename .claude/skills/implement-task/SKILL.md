---
name: implement-task
description: Implement a GitHub task issue, read analysis, draft plan, and implement
disable-model-invocation: true
arguments: task_number
argument-hint: "[task-number]"
---

## Role
- You are a senior Java/Spring engineer responsible for implementing the assigned task
  - Implement exactly what the task requires
  - Avoid scope creep
  - Avoid unnecessary abstractions
  - Avoid unrelated changes

## Rules (Non-negotiable)
- **Never search `~/.m2`, unzip jars, or inspect jar contents, under any circumstances.** This includes proactive API discovery, dependency inspection, and failure diagnosis.
- To look up an API, class, or method: use `WebSearch`. Spring Boot and Java APIs evolve, always search for the latest documentation.
- If a dependency appears missing: read `pom.xml` to confirm what is declared.
- On any build or test failure: read the error message, fix only what the error indicates.

## Steps

### Read the task issue and analysis
```bash
gh issue view $task_number --json number,title,body,comments
```
- The analysis comment posted by `/analyze-task` contains key decisions and constraints. Implementation must reflect those decisions.

### Load project context
- Read `CLAUDE.md`, every file under `rules/`, and explore the existing codebase.
- Every decision in the implementation must comply with them.

### Draft implementation plan
- Create `tmp/` if it does not exist.
- Using `plan-template.md` as the structure, write the detailed plan to `tmp/task-$task_number-plan.md`.
- Fill in every section: list every file to be created or modified with the specific changes, and use code blocks to show exact changes (method signatures, class skeletons, SQL snippets) so the user can review precisely what will be implemented.
- Ask the user to review and edit the file directly, then confirm approval in chat.

**Approval Gate: Wait for explicit user approval before proceeding.**

### Checkout feature branch
```bash
git checkout main
git pull origin main
git checkout feature/task-$task_number
```
- If the checkout fails because the branch does not exist, run:
```bash
git checkout -b feature/task-$task_number
```
- Never branch from another feature branch.

### Implement
- Follow all standards in `rules/`.
- Implement the approved plan exactly.
- Do not deviate from the approved implementation plan without asking for approval again.
- All new functionality must be covered by integration tests, following the project's testing standards.
- After implementation, run the project's automated tests.
```bash
./mvnw test
```

### Self Review

Before requesting user approval:

- Review all modified files.
- Remove dead code, debug code, and unused imports.
- Ensure only task-related files have been modified.
- Verify the implementation matches the approved plan.
- Ask the user to review the implementation and make any manual edits.

**Approval Gate: Wait for explicit user approval before committing.**

### Commit and push (after user approval)
- Stage only the changed files.
- Commit following `rules/git-standards.md`: Conventional Commits format, with the required `Co-Authored-By` trailer.
- Push the branch:
```bash
git push -u origin feature/task-$task_number
```
