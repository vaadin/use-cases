# Testing

## Every view has a browserless test

For every `uc<N>/<Name>View.java` add `src/test/java/com/example/uc<N>/<Name>ViewTest.java`
that at minimum:

- navigates to the view with `navigate(<Name>View.class)`,
- asserts that the expected top-level components render (heading, key
  buttons, the primary signal-bound container),
- exercises the view's behaviour with at least one interaction or state change
  when the view has any.

```java
@SpringBootTest
@ViewPackages(classes = UpdateWhenActiveView.class)
class UpdateWhenActiveViewTest extends SpringBrowserlessTest {

    @Test
    void statusBadgeReflectsPageVisibilityChanges() {
        navigate(UpdateWhenActiveView.class);
        runPendingSignalsTasks();
        assertBadgeContains("Visibility unknown");

        setPageVisibility(PageVisibility.HIDDEN);
        assertBadgeContains("tab hidden");
    }
}
```

Use `SpringBrowserlessTest` (from `browserless-test-spring`) with
`@SpringBootTest` and `@ViewPackages(classes = MyView.class)`. A view that needs
no Spring beans can use plain `BrowserlessTest`. Examples to mirror:
`page-visibility/src/test/java/com/example/uc1/` (per-view interaction) and
`signals/src/test/java/com/example/muc01/` (a second user in their own
window).

Logic that does not need a UI — a status derived from timestamps, a parser, an
encoder — gets a plain unit test.

## Tests that can fail

A test earns its place by failing when the behaviour it names breaks. Name the
test after the behaviour (`pickingAnEpisodeSwapsTheSourceAndPlaysIt`), assert
the outcome the reader would see, and check the test fails without the change
it covers. A test that only checks that the view renders its heading is a
start, not the test of a use case.

When the browserless environment can not drive a feature (no simulator, the
behaviour happens in the browser), test what the server can observe, record
the missing simulator in `API-GAPS.md`, and say in the pull request how the
browser half was checked.

## Shared Spring context

`@SpringBootTest` caches one application context and reuses it across test
methods and classes. Singleton beans — a `MeterRegistry`, a service holding a
shared signal, an in-memory store — keep their state between tests, and JUnit
does not run methods in a fixed order. A test that asserts an absolute count
therefore passes or fails depending on which tests ran before it.

Either:

- read the value before the action and assert the difference the action made,
  or
- give the class a fresh context:

```java
// A fresh context per test, so the shared registry starts empty and a count
// can be asserted exactly rather than as a delta.
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
```

Prefer the first: a fresh context per method makes the module's build slower.

## Second session

When a view depends on an application-scoped bean that holds shared state,
also write a test with a second user. Open a multi-user context with session
lock isolation on, let one user act in their own window, and assert what the
other user's window shows:

```java
try (var app = SpringBrowserlessApplicationContext.create(applicationContext,
        b -> b.withViewPackages(MaintenanceBannerView.class)
                .withSessionLockIsolation(true))) {
    var alice = app.newUser().newWindow();
    var bob = app.newUser().newWindow();
    alice.navigate(MaintenanceBannerView.class);
    bob.navigate(MaintenanceBannerView.class);

    alice.test(alice.find(Button.class).withText("Publish to everyone")
            .single()).click();
    alice.runPendingSignalsTasks();
    assertEquals("...", bob.find(Div.class)
            .withClassName("maintenance-banner").single().getText());
}
```

This catches `ListSignal`-vs-`SharedListSignal` mistakes and other
cross-session leaks at test time rather than in the second browser tab. With
lock isolation, a user's session is locked only while one of that user's
windows is active, as in a real request. Code that changes another user's UI
without `UI.access()` therefore fails the test instead of passing silently.
Use `createSecured(...)` and `app.newUser("alice", "USER")` when the view
needs a signed-in user.

## Lifecycle

When a view starts work on attach, test that detaching stops it (navigate
away, then check that no further updates arrive). When a view must leave
session-wide state untouched, assert that state before and after the view.

## Fixtures

Do not commit generated fixtures (source maps, recorded payloads, images
produced by the code under test). Generate them while the tests run, into
`target/test-classes` or another directory under `target/`.

## Running

```bash
./mvnw -pl <module> -am test
./mvnw -pl <module> -am test -Dtest='<TestClass>' -Dsurefire.failIfNoSpecifiedTests=false
```

Before opening a pull request, also check the views in a running app (headless
Chromium is fine), including a narrow (390 px) window.
