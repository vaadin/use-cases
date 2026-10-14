# Documenting

## View Javadoc

Every view starts with a class Javadoc that names the use case and explains it
in plain English:

```java
/**
 * UC1 — Update when active.
 * <p>
 * A live server clock updates every second while the tab is visible. The
 * effect of {@link Page#pageVisibilitySignal()} is reactive: as soon as the
 * user hides the tab the periodic task is cancelled, so the websocket stays
 * quiet. On return, ticking resumes.
 */
```

Explain non-obvious choices where they are made — why a call does not trigger
playback, why a value is application-wide — with a short comment. The view's
Javadoc is for developers reading the code; the view's own text is for readers
of the running app. Both must say the same thing.

## Module README

Each module's `README.md` has:

- a title (`# <Feature> — use cases`) and a short paragraph on what the module
  demonstrates and which API it builds on;
- one table row per use case: number, view title, what it shows;
- how to run it, and any setup a use case needs (keys, external services,
  Spring profiles);
- a link to `API-GAPS.md`.

When a view's title, behaviour or route changes, update its row in the same
pull request.

## Keeping text in sync

The same fact often appears in four places: the view text, the class Javadoc,
the README row and `API-GAPS.md`. When one changes, search for the others. A
reviewer will.
