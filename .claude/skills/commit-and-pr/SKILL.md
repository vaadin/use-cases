---
name: commit-and-pr
description: How to write a commit message and a pull request description in this repository - the commit subject rules with module scopes, and the PR body (review risk line, summary, what changed, test summary). Use before writing a commit message, before opening a pull request, and when rewriting an existing PR body.
---

# Commits and pull requests

These are the canonical rules for this repository; the Commit & PR Hygiene
section of [`CONVENTIONS.md`](../../../CONVENTIONS.md) points here.

Run `./mvnw spotless:apply` before committing.

## Commit messages

### Subject line

- Format: `<type>(<scope>): <summary>` or `<type>: <summary>` (Conventional Commits)
- Length: under 72 chars
- Allowed types: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `ci`
- Scope: the module the change is in — `feat(multimedia):`, `fix(printing):`,
  `feat(signals):`. `o11y` is the established short scope for
  `observability`. Leave the scope out for changes to `common`, the parent pom
  or several modules.
- Start with an imperative verb — "add", "fix", "remove" — not "added", "adds"
- Name the use case when the change is about one: `fix(multimedia): show one
  subtitle track in UC9`
- Don't repeat the type in the summary: avoid `fix: fix...`

### Body

- Skip when the subject is self-explanatory
- Add a body only for a non-obvious *why*
- Add `Fixes #issuenumber` when the commit resolves an issue in this repository

## What lands in git history

Pull requests are squash-merged, and the squash commit message is the PR title
followed by the PR description. Write the description so it still reads well
in `git log` a year from now: plain Markdown, no HTML, no checkboxes.

## Pull request title

Use the same rules as for commit subject lines.

## Pull request description

Fixed order. Omit a section instead of leaving it empty.

```markdown
**Review risk:** <two-way door | one-way door> — <blast radius>

## Summary

<1-3 sentences in plain words: what a reader of the app gets, or what was
broken and is now fixed.>

## What changed

- <One behaviour per bullet, identifiers in backticks>
- <UC<N>: … when the change touches several use cases>

## Test summary

- <What the new or changed tests cover, in plain words>
- <The module test result, e.g. "observability: 154 tests green">
- <What was checked by hand in a running app, including mobile width>
```

### Review risk

One line, first, so a reviewer knows how careful to be before reading
anything else.

- **two-way door** — easy to revert: a change inside one module's views,
  docs, tests.
- **one-way door** — hard to undo once merged: renaming a module or its Fly
  app (the URL changes), removing a use case others link to, changing a
  workflow that deploys or holds secrets.

The blast radius says who sees the change: "one view in `multimedia`", "every
app, through `BaseMainLayout`", "deploys: every app is redeployed because
`common` changed".

### Summary

- At most 3 sentences, no class or file names. Say what the reader of the app
  sees.
- For a new use case, name it as it appears in the menu (`UC12 — Skeleton that
  fills in`) and say in one sentence what problem it shows.

### What changed

- One behaviour per bullet, not one file per bullet.
- Call out anything that changes for every app (`common`, the parent pom, the
  workflows) and anything a maintainer has to do after merging (a secret, a
  Fly app).
- Mention the `API-GAPS.md` entries added, closed or changed.
- Cap at ~10 bullets. Past that, the pull request probably does more than one
  thing — split it.

### Test summary

- Behaviour covered, not test-class names.
- Always say how the views were checked in a browser, or say that they were
  not.

### Plain English

Most readers are non-native English speakers. Use short sentences and common
words; keep API names and identifiers exact.

## Opening a pull request

- Open it as a draft, self-review the diff, then mark it ready.
- Add the `deploy preview` label when a reviewer should try the views in a
  browser; the preview app URL is posted on the pull request.
- One purpose per pull request. Move unrelated refactorings and cross-module
  sweeps to their own pull request, and say in each which one to merge first.
- Keep the description current. When a pull request it depends on is merged,
  or review changes the scope, update the description.
