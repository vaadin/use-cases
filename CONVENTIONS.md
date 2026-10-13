# Conventions

The canonical list of checkable conventions for this repository. Read it in
full when authoring or reviewing code. Design-level reasoning behind several of
these rules lives in `guidelines/` — see `guidelines/overview.md`.

## Use-Case Content

Derive use cases from what the feature is for — the real things an application
developer would do with it — not from what the current API happens to expose.
A use case the API can not support yet becomes an `API-GAPS.md` entry, not a
reason to drop it.

Every claim the app makes — view text, step instructions, Javadoc, README rows
and `API-GAPS.md` — must be true of the code that runs. Verify a claim about the
feature against its source (Flow, Flow Components, the kit or add-on) before
writing it, and update every place that repeats it when the behaviour changes.

Never ship a broken or half-working example. If a scenario only works in some
browsers, make it work everywhere or say so in the view; do not leave a player
that can not seek or a panel that always shows the same record.

A use case must be understandable from the running app alone, without cloning
the repository or opening "View source". Explain on the page what to do, what to
look at, and how the example is built.

Every view must work on a phone (check at 390 px wide). Stack side-by-side
layouts, let wide content scroll inside its own box, and keep overlays clear of
the navbar title.

Keep each use case independent of the others. A number a view shows must
describe that view only — scope metrics, counters and data to the view (e.g. by
route or tag) so visiting another use case first does not change it, or state
plainly that the value is application-wide.

An empty state must tell "nothing happened yet" apart from "the feature is not
active" (missing license, disabled flag, unsupported browser), so the reader is
never told to retry something that can not work.

Do not promise what the app does not do. User-facing copy must not promise a
response, a guarantee or a capability the feature lacks.

Remove dead code: styling branches that can never apply, methods only a test
calls, and options nobody asked for. Prefer one simple control over a choice
the use case does not need.

## Views

Name each view `<Descriptive>View` in package `com.example.uc<N>`, with
`@Route("uc<N>")`, `layout = MainLayout.class`.

Keep the `@PageTitle` and `@Menu` title short — `UC<N> — <short name>` — so it
fits on one line in the side navigation and the browser tab. Name the problem
the reader has, not the feature ("Slow user actions", not "Interaction
latency").

Put the longer problem statement in `@UseCaseDescription` on the view. It is the
single source for the home card description, the nav tooltip and the link
preview; do not repeat it as a separate string elsewhere.

Every module's home page lists one card per use case through
`BaseHomeView#addMenuCards(...)` or `homeCard(...)`.

Use cases must be browsable without logging in. In a module with Spring
Security, annotate views `@AnonymousAllowed` and permit the static resources
the views load (`/*.css`, `/og-image.jpg`).

Keep per-view CSS in `src/main/resources/META-INF/resources/uc<N>.css`, loaded
with `@StyleSheet("uc<N>.css")` on the view, and prefix its class names with the
view (`uc1-time`) so views can not restyle each other.

When a demo needs a fake problem (latency, failures, load), put the knobs that
create it in a visibly separate area of the view so they are not mistaken for
the feature.

## Lifecycle & State

Start periodic work, polling and listeners in `onAttach` and stop them in
`onDetach` (or keep the `Registration` and remove it on detach). A view must not
keep doing work after the user navigates away.

Set a "scheduled" or "running" flag only once the work is actually scheduled.
Setting it before checking that the view is attached can leave it set forever.

Do not replace session- or application-wide state from a view (the session
error handler, global listeners, static fields) to make a demo work. If a view
must react to something, handle it in the view's own listener.

State shared between users or tabs lives in an application-scoped Spring bean
and uses a shared signal (`SharedValueSignal`, `SharedListSignal`), never a
local one.

Schedule asynchronous UI updates through `UI#access`/`accessLater` and a Spring
`TaskScheduler` or executor; enable `@Push` on the app shell when a view needs
it.

Never build JSON by string concatenation; use Jackson.

## Theming

All apps load the Aura theme (`@StyleSheet(Aura.STYLESHEET)`). Do not use
`LUMO_*` variants or `--lumo-*` custom properties; use theme-neutral variants
(`ButtonVariant.PRIMARY`, `ButtonVariant.ERROR`, …) and `--vaadin-*` / `--aura-*`
properties.

Check that every CSS custom property you use is defined by the theme. An
undefined property silently resolves to nothing.

Do not use a theme name that Aura does not style (e.g. `contrast`); pick one that
is styled and fits the meaning (`warning`, `error`, `success`).

See [Theming](guidelines/theming.md).

## Testing

Every view has a browserless test next to it in `src/test/java/com/example/uc<N>/`
that navigates to the view, asserts the key components render, and exercises at
least one interaction or state change. See [Testing](guidelines/testing.md).

A test must be able to fail when the behaviour it names breaks. Do not keep a
test that only asserts what the view always renders when the use case is about
something else.

Tests must not depend on method order. When a test reads state from a singleton
bean (a `MeterRegistry`, a shared signal, a cache) in a cached Spring context,
either assert the difference its own actions made, or give the class a fresh
context with `@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)`.

When a view depends on application-scoped shared state, add a test that
simulates a second session.

Do not commit generated test fixtures. Generate them while the tests run, into
`target/`.

## Build & Dependencies

Prefer a small, maintained library over a hand-written implementation of a
standard format or algorithm (a parser, an encoder). Prefer the smallest one
that does the job and say in the PR why it was chosen.

Declare an extra Maven repository only in the module that needs it, releases
only, never in the parent pom.

Never commit build output (`target/`, `node_modules/`, `src/main/bundles/`,
`src/main/frontend/generated/`).

`package.json`, `package-lock.json` and `vite.config.ts` are generated by the
Vaadin build but committed. Regenerate them for all modules when the Vaadin
version changes, in the same pull request as the version change. Keep
regenerated copies out of unrelated pull requests, and do not delete them from
a single module.

A new module needs: an entry in the root `pom.xml` `<modules>`, an entry in
`AppCatalog.APPS`, a `Dockerfile`, a `fly.toml`, an `og-image.jpg`, a
`README.md` and an `API-GAPS.md`. See [Repository](guidelines/repository.md).

## Documenting

Every view has a class Javadoc of the form `UC<N> — <short name>.` followed by a
paragraph that explains the scenario in plain English.

Every module's `README.md` has one table row per use case; keep it in sync with
the view titles and behaviour.

Each `API-GAPS.md` entry names where it bites, the symptom, the workaround and
the API that would remove it, and links the upstream issue. Close or remove an
entry as soon as the upstream fix lands, together with every view text that
repeats it. See [API Gaps](guidelines/api-gaps.md).

## Code Style

Run `./mvnw spotless:apply` before committing.

Code is null-marked through the nullability plugin; annotate nullable fields,
parameters and return values with JSpecify `@Nullable`.

Workarounds for missing API live in the module's `MissingAPI` class (or next to
the use case that needs them), with Javadoc naming the upstream issue and how to
migrate when the real API lands.

## Commit & PR Hygiene

Follow the `commit-and-pr` skill
(`.claude/skills/commit-and-pr/SKILL.md`) for commit messages and pull request
descriptions.

Keep a pull request to one purpose. Split an unrelated refactoring or a
cross-module sweep into its own pull request.

Merge `main` into a long-lived branch instead of rebasing a pull request that
is already under review.

Keep the pull request description current: when a dependency it waits for is
merged, or review changes the scope, update the description.
