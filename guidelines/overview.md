# Use-Case Guidelines

These guidelines describe how use-case apps in the `use-cases` repository should
be designed and implemented. A use-case app shows what a Vaadin feature is for,
through runnable scenarios a developer can try in the browser, and records where
the current API falls short. Chapters can be read selectively for the topics
your work touches.

Treat these as guidelines, not hard rules. They are best practices that should
be followed by default, but can be deviated from when necessary to make
something work.

For repository-level commands (build, test, run) see [`CLAUDE.md`](../CLAUDE.md).
For commit messages and pull requests see the
[`commit-and-pr`](../.claude/skills/commit-and-pr/SKILL.md) skill.

## Chapters

| Chapter                                 | Topic                                                                       |
| --------------------------------------- | --------------------------------------------------------------------------- |
| [Repository](repository.md)             | Module layout, what a new module needs, CI, dependencies, code style.       |
| [Use-Case Design](use-case-design.md)   | Choosing use cases, telling the story in the UI, naming, mobile.            |
| [Views](views.md)                       | View structure, lifecycle, shared state, async updates, security.           |
| [Theming](theming.md)                   | Aura, theme-neutral variants and custom properties, per-view CSS.           |
| [Testing](testing.md)                   | Browserless tests, shared Spring context, second-session tests.             |
| [API Gaps](api-gaps.md)                 | Writing and maintaining `API-GAPS.md` and `MissingAPI`.                     |
| [Documenting](documenting.md)           | View Javadoc, module README, keeping text in sync with the code.            |
