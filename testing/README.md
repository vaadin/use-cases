# Testing — use cases

A standalone Spring Boot demo of testing a Vaadin Flow application at every level. The application is a small order desk; each view is one part of it, and the tests that cover it are the actual content. They go from the cheapest layer to the most expensive: plain unit tests, view tests without a browser, forms, grids, security, background work and browser APIs, then end-to-end, screenshot and accessibility tests in a real browser, and a load test recorded from an end-to-end test. `API-GAPS.md` records where Flow made testing harder than it should be.

| # | View | Tests |
| - | ---- | ----- |
| UC1 | Logic without the UI | `orders/QuoteCalculatorTest`: the pricing rules as plain, parameterized JUnit tests; `uc1/QuoteViewTest` only checks the wiring. |
| UC2 | View test without a browser | `uc2/NewOrderViewTest`: fill in the order form through component testers, click, check the notification, the navigation and the stored order. |
| UC3 | Forms and validation | `uc3/RegistrationViewTest`: each Binder rule through the field's invalid state and message, a cross-field rule, the button enabled only for a valid form, the written bean. |
| UC4 | Grids and lazy data | `uc4/OrdersViewTest`: a lazy Grid over 100,000 orders: size, cells, backend sorting and filtering, selection, a button inside a cell. |
| UC5 | Security | `uc5/StaffDeskViewTest`: the login redirect for visitors, the desk for clerks, the admin-only button, with `@WithAnonymousUser` / `@WithMockUser`. |
| UC6 | Async work and push | `uc6/ReportViewTest`: a background job on an executor bean that the test replaces (`@TestBean`) with one it runs by hand, then the queued `UI.access` commands; no sleeps. |
| UC7 | Browser APIs in tests | `uc7/BrowserApisViewTest`: geolocation through its simulator, the locale set directly, page visibility through a faked browser event, and the time zone that cannot be set. |
| UC8 | End-to-end in a real browser | `uc8/CheckoutIT` (TestBench) and `uc8/CheckoutPlaywrightIT` (Playwright): place an order and find it in the list, in Chrome, against the packaged app. |
| UC9 | Visual regression | `uc9/InvoiceScreenshotIT` (TestBench `compareScreen`) and `uc9/InvoiceScreenshotPlaywrightIT` (masked element screenshot and a small comparator), against `src/test/screenshots`. |
| UC10 | Accessibility checks | `uc10/AccessibilityPlaywrightIT`: axe-core over several views, a deliberately broken variant it must flag, and keyboard-only use of a form. |
| UC11 | Load testing | Vaadin's load testing plugin records `CheckoutPlaywrightIT` into a k6 script and runs it with many virtual users; the view shows sessions, UIs and orders while it runs. |

Demo users for UC5: `clerk` / `clerk` and `admin` / `admin`.

## Run

The application:

```
cd testing
mvn spring-boot:run
```

Unit and view tests (no browser, part of the normal build):

```
./mvnw -pl testing -am test
```

Browser tests (builds the app for production, starts it on port 8090, runs the `*IT` classes in headless Chrome). TestBench needs a Vaadin subscription key; Playwright downloads its own browser on the first run:

```
./mvnw -pl testing -am verify -Pit
```

A failing screenshot test writes the new image to `target/screenshot-errors`; copy it to `src/test/screenshots` to accept it. TestBench's reference names include Chrome's major version, so a Chrome update needs new TestBench references.

Load test (needs [k6](https://k6.io) on the `PATH` or `-Dk6.binary=...`, and a Vaadin subscription key). It records the checkout once, then runs it with `k6.vus` users for `k6.duration`, fails on the plugin's response time thresholds, and writes an HTML report to `target/k6/tests/report`:

```
./mvnw -pl testing -am verify -Pload -Dk6.vus=20 -Dk6.duration=1m
```

Every virtual user iteration starts a new session, which stays in memory until it times out; the server metrics at the end of the run show it.
