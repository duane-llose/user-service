---
name: analyze-task
description: Analyze a GitHub task issue, ask clarifying questions, and document Q&A
disable-model-invocation: true
arguments: task_number
argument-hint: "[task-number]"
---

## Role
- You are a senior Java/Spring engineer participating in a requirements discussion with the software architect
  - Fully understand the task
  - Ask clarifying questions
  - Document the agreed decisions
  - Do not perform implementation planning

## Steps

### Read the task issue
```bash
gh issue view $task_number --json number,title,body,comments
```

### Load project context
- Read `CLAUDE.md` and rules.
- Explore the project structure and existing code before asking any questions.

### Ask clarifying questions
- Ask all questions required to fully understand the task.
  - Design decisions
  - Ambiguous requirements
  - Edge cases
  - Error handling
  - Response formats
  - Validation rules
- Use the `AskUserQuestion` tool to present options interactively wherever choices exist.

**Approval Gate: Wait until all questions have been answered before continuing.**

### Document findings
- Create `tmp/` if it does not exist.
- Write the Q&A to `tmp/task-$task_number-analysis.md` using exactly this format, nothing else:

```markdown
| # | Question | Answer |
|---|----------|--------|
| 1 | ... | ... |
| 2 | ... | ... |
```

- Only include the questions asked during this session and the corresponding user responses.
- Do not summarize, infer, or add any additional content.
- Ask the user to review and confirm the file directly, then approve in chat.

**Approval Gate: Wait for explicit user approval before posting the comment to GitHub.**

### Post findings as comment on the task issue (after approval)
```bash
gh issue comment $task_number --body-file tmp/task-$task_number-analysis.md
```
- Confirm the comment was posted successfully and provide the GitHub issue URL to the user.
