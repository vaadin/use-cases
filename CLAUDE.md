# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

This repository holds runnable use-case apps for Vaadin features. Each
top-level directory (except `common`) is a standalone Spring Boot + Vaadin Flow
application that demonstrates one feature area through a set of numbered use
cases (`UC1`, `UC2`, …), and records where the current API falls short in an
`API-GAPS.md`. Every app is deployed to Fly.io and linked from the application
selector of every other app.

### Technologies

- Java 25, Maven (use the `./mvnw` wrapper)
- Spring Boot 4, Vaadin Flow and Flow Components on the current snapshot
- The Aura theme
- Browserless tests (`browserless-test-spring`) with JUnit

See `guidelines/repository.md` for the module layout and what a new module
needs.

## Guidelines & Conventions

Always read `CONVENTIONS.md` in full when **authoring** or **reviewing** code, and before **committing** or **opening a pull request** — it is the canonical list of checkable conventions.

Design and implementation guidelines live in `guidelines/`. Read the chapters mapped in `guidelines/overview.md` selectively for the topics your work touches.

The commit message format, the PR description format and the expectations for opening a pull request are in the `commit-and-pr` skill (`.claude/skills/commit-and-pr/SKILL.md`), linked from the Commit & PR Hygiene section of `CONVENTIONS.md`.

## Development Commands

```bash
# Build and test one module (and the shared common module it depends on)
./mvnw -pl <module> -am install

# Run the tests of one module
./mvnw -pl <module> -am test

# Run a single test class
./mvnw -pl <module> -am test -Dtest='<TestClass>' -Dsurefire.failIfNoSpecifiedTests=false

# Run an app at http://localhost:8080/ (install common first)
./mvnw -pl common install -DskipTests
./mvnw -pl <module> spring-boot:run

# Format Java sources (Eclipse formatter + import order)
./mvnw spotless:apply
```

CI builds every module separately with `./mvnw -pl <module> -am install`, so a
change to `common` must keep every module building.
