# Custom components — use cases

A standalone Spring Boot demo of building your own Vaadin Flow components, in each of the ways Flow supports, and of an application assembled from them: a small service desk. The components live in `com.example.components`, laid out the way an add-on jar would be. `API-GAPS.md` records where Flow made building them harder than it should be.

| # | View | What it shows |
| - | ---- | ------------- |
| UC1 | Composite of existing components | `AddressForm`, a `Composite<FormLayout>` with its own small API, used twice (shipping and billing, "same as shipping"). |
| UC2 | Field with Binder and signals | `MoneyField`, a `CustomField<Money>` of an amount and a currency: Binder rules and messages, `bindValue`, read-only. |
| UC3 | Own API and events | `RatingStars`, a `HasValue<Integer>` with its own `LowRatingEvent`, built as an accessible radio group. |
| UC4 | Server-side DOM | `Sparkline`, an SVG chart built with the Element API in a Grid column, no JavaScript. |
| UC5 | Own web component | `SignaturePad`, a Lit element with a Java API: synchronized value, a DOM event as a server event, a method called from Java. |
| UC6 | npm web component | `QrCode`, a typed wrapper for `<qr-code>` from the `webcomponent-qr-code` package (`@NpmPackage`). |
| UC7 | React component | `ColorPicker`, `react-colorful`'s picker through `ReactAdapterComponent`, with state shared both ways. |
| UC8 | Styling and theming | The components' public styling API: theme variants as Java enums, CSS variables and shadow parts, in light and dark mode. |
| UC9 | Testing the components | A browserless tester (`RatingStarsTester`, `@Tests`), a TestBench element (`RatingStarsElement`) and a Playwright test that draws a signature. |
| UC10 | The composite app | A service desk from those components; `TicketDetail` is itself a component of components, talking to the view through `TicketSavedEvent`. |
| UC11 | Packaging as an add-on | What each component brings (stylesheets, TypeScript, npm packages), read from its annotations and checked against the classpath. |

Each view explains itself without opening the source: below the example, a "How it is built" section names the technique, gives the three points that make the component work, and lists the component's files, collapsed, each opening to its real source (packaged with the application at build time). The views also work on phones; the service desk stacks the ticket list above the detail on narrow screens.

## Add-on layout

```
com/example/components/*.class
META-INF/resources/rating-stars.css, sparkline.css     (@StyleSheet)
META-INF/resources/frontend/signature-pad.ts           (@JsModule, Lit)
META-INF/resources/frontend/color-picker.tsx           (@JsModule, React)
npm packages: declared with @NpmPackage on QrCode and ColorPicker
```

Moved into a jar of their own, the components work in another application unchanged: Vaadin copies the frontend files out of the jar, adds the npm packages to the application's `package.json` and bundles everything when that application builds.

## Run

```
cd custom-components
mvn spring-boot:run
```

Open <http://localhost:8080/>.

Unit and view tests: `./mvnw -pl custom-components -am test`. Browser tests (production build on port 8094, headless Chrome; TestBench needs a Vaadin subscription key): `./mvnw -pl custom-components -am verify -Pit`.
