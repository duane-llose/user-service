---
name: create-pr
description: Review, test, and raise a PR for the feature branch
disable-model-invocation: true
arguments: task_number
argument-hint: "[task-number]"
---

## Role
- You are a senior Java/Spring engineer responsible for preparing a high-quality Pull Request for the current feature branch

## Steps

### Confirm the current branch
- Verify the current branch is exactly `feature/task-$task_number`.
- If not, stop and notify the user.

### Check for merge conflicts
```bash
git fetch origin main
git merge-tree origin/main HEAD
```
- If the command reports conflicts, stop and notify the user. Do not attempt to resolve automatically.

### Run tests
```bash
./mvnw clean test
```
- All tests must pass before continuing.
- On any test failure, read the error message and fix only what it indicates.

### Review the implementation
```bash
git diff origin/main...HEAD
```
- Review the implementation against the standards defined in `rules/`.
- Focus on:
  - Correctness
  - Maintainability
- Ignore formatting, personal style preferences, and unnecessary refactoring.
- Fix any issues you identify.
- Re-run tests after fixes:
```bash
./mvnw test
```
- If any fixes were applied, stage and commit them following `rules/git-standards.md`: Conventional Commits format, with the required `Co-Authored-By` trailer.
- Push any fix commits:
```bash
git push
```
- Summarize:
  - Issues identified
  - Fixes applied
  - If no issues were found, state that explicitly.

### Create the Pull Request
- Read `pr-template.md`.
- Populate every placeholder using the implemented changes and task #$task_number.
```bash
gh pr create --title "<conventional-commit-title>" --base main --body "<populated-pr-template>"
```
- Print the PR URL for the user to review and merge.
