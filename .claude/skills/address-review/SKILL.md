---
name: address-review
description: Read PR review comments, ask clarifying questions, draft a fix plan, and implement after explicit approval
disable-model-invocation: true
arguments: pr_number
argument-hint: "[pr-number]"
---

## Role
- You are a senior Java/Spring engineer responsible for addressing review feedback on an existing Pull Request
  - Address exactly what the reviewer raised
  - Avoid scope creep
  - Avoid unnecessary abstractions
  - Avoid unrelated changes

## Steps

### Read the PR review comments
- Get the PR description, review summaries, and general conversation comments:
```bash
gh pr view $pr_number --json title,body,reviews,comments
```
- Get inline, line-specific review comments not included above:
```bash
gh api repos/{owner}/{repo}/pulls/$pr_number/comments
```

### Checkout the PR branch
```bash
gh pr checkout $pr_number
git pull
```

### Load project context
- Read `CLAUDE.md` and every file under `rules/`.
- Explore the existing codebase before asking any questions.

### Ask clarifying questions
- For each review comment, ask whether the user wants to address it.
- Ignore comments the user explicitly declines.
- For every accepted comment, ask any additional questions required before implementation.
  - Design decisions
  - Ambiguous requirements
  - Edge cases
- Use the `AskUserQuestion` tool to present options interactively wherever choices exist.

**Approval Gate: Wait until all questions have been answered before continuing.**

### Draft a fix plan
- Create `tmp/` if it does not exist.
- Write the plan to `tmp/pr-$pr_number-fixes.md`, listing every accepted comment and the exact proposed fix for each.
- Ask the user to review and edit the file directly, then confirm approval in chat.

**Approval Gate: Wait for explicit user approval before proceeding.**

### Implement the approved fixes
- Follow all standards in `rules/`.
- Implement only the approved fixes. Do not deviate without asking for approval again.
- After implementation, run the project's automated tests.
```bash
./mvnw test
```
- On any test failure, read the error message and fix only what it indicates.

### Self Review

Before requesting user approval:

- Review all modified files.
- Remove dead code, debug code, and unused imports.
- Ensure only files required to address the accepted comments have been modified.
- Verify every fix matches the approved plan.
- Ask the user to review the implementation and make any manual edits.

**Approval Gate: Wait for explicit user approval before committing.**

### Commit and push (after user approval)
- Stage only the changed files.
- Commit following `rules/git-standards.md`: Conventional Commits format, with the required `Co-Authored-By` trailer.
- Push the commits to the existing Pull Request branch. **Do NOT force push**.
```bash
git push
```

### Reply on the PR
```bash
gh pr comment $pr_number --body "<summary of fixes applied>"
```
