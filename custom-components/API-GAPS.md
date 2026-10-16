# Custom components — API gaps discovered while building the demos

Places where Flow makes building a component harder than it should be, or where a component works in the browser but trips up in a test or another application. Each entry names the use case that surfaced it.

The short version: **every kind of component can be built, but each has an edge that only shows up later.** A composite or custom field works in the browser but not when its parts change on the server. A web component works once it has rendered, but the server may call it earlier. A React component works but is not a field. An add-on works in the application that built it, but brings assumptions about the consuming application's build with it.

---

## 1. `CustomField` only takes its value from the browser's change event

**Where it bites:** UC2 / MoneyFieldView.java, `MoneyField.java`
**Symptom:** a `CustomField` recomputes its value when the `<vaadin-custom-field>` element fires `change` in the browser. When an inner field changes on the server, or in a browserless test that sets an inner field, the custom field's value stays as it was. Binder rules then run against the old value and the summary bound to it does not move.
**Workaround used:** `MoneyField` calls `updateValue()` from value change listeners on its inner fields.
**Suggested API:** `CustomField` updating its value on its inner fields' value changes by default (or an opt-in `setUpdateOnInnerValueChange(true)`).
Upstream: https://github.com/vaadin/flow-components/issues/10426

## 2. `AbstractCompositeField` does not redraw a value set by the user

**Where it bites:** UC3 / RatingView.java, `RatingStars.java`
**Symptom:** a value that comes from the user is set with `setModelValue(value, true)`, which does not call `setPresentationValue`: it assumes the presentation already shows the value, as an input element would. For a component whose presentation is drawn on the server (lit stars), clicking the third star set the value to 3 but no star lit up.
**Workaround used:** `RatingStars` draws the stars itself before `setModelValue`.
**Suggested API:** a note in the Javadoc of `setModelValue`, or an overload that also updates the presentation.
Upstream: https://github.com/vaadin/flow/issues/11392

## 3. The server can call a web component before it has rendered

**Where it bites:** UC10 / ServiceDeskView.java, `signature-pad.ts`
**Symptom:** `getElement().callJsFunction("clear")` in the same round trip that attaches the element ran before the Lit element had rendered its canvas, and failed in the browser (`Cannot read properties of null`). In UC5 it never showed up, because the user clicked Clear long after rendering; in the service desk, selecting a ticket calls it at once.
**Workaround used:** the element's `clear()` awaits `this.updateComplete` first.
**Suggested API:** `callJsFunction` waiting for the element to be upgraded and rendered (`customElements.whenDefined` plus `updateComplete` when present), or a documented "when ready" variant.
Upstream: https://github.com/vaadin/flow/issues/6007

## 4. Wrapping a web component is all hand-written strings

**Where it bites:** UC5 / SignaturePad.java, UC6 / QrCode.java
**Symptom:** every attribute, property, event and method of a web component becomes a string in Java (`setAttribute("ecclevel", …)`, `@DomEvent("signature-started")`, `callJsFunction("clear")`). Most web components describe their API in a `custom-elements.json` manifest, but Flow cannot use it, so typos and renamed attributes only show up in the browser.
**Workaround used:** hand-written typed setters, an enum for the error correction level, and tests that check the attributes.
**Suggested API:** a generator that turns a `custom-elements.json` manifest into a typed Java wrapper (properties, events, methods), or at least a checked mapping of property names.
Upstream: https://github.com/vaadin/flow/issues/26396

## 5. A React component is not a field

**Where it bites:** UC7 / ColorPickerView.java, `ColorPicker.java`
**Symptom:** `ReactAdapterComponent` shares named state (`getState("color", String.class)`, `addStateChangeListener`), which works, but the result is not a `HasValue`. A Binder cannot bind it, `bindValue` does not exist, and the state change listener does not say whether the change came from the user. The TSX file also imports `Frontend/generated/flow/ReactAdapter`, a path generated in the consuming application.
**Workaround used:** a `ColorPicker` with `getColor` / `setColor` / `addColorChangeListener`; the view keeps the hex field in sync itself.
**Suggested API:** a `ReactAdapterField<T>` base class that maps one state key to a `HasValue<T>`, and a published import path for the adapter.
Upstream: https://github.com/vaadin/flow/issues/26397

## 6. An add-on's TypeScript is compiled with the consuming application's settings

**Where it bites:** UC11 / PackagingView.java, `signature-pad.ts`, `color-picker.tsx`
**Symptom:** the add-on ships TypeScript source, and the application that uses it compiles it. Since vaadin/flow#25241 the copied sources get a `tsconfig.json` of their own, which fixed add-on sources being compiled with no options at all, but it extends the application's configuration, so the application's compiler options still apply. The Lit element relies on `experimentalDecorators` and `useDefineForClassFields: false`, and the React wrapper on `jsx: react-jsx`. Vaadin's generated defaults happen to have them, but an application that changed its `tsconfig.json` breaks the add-on. Nothing checks that the stylesheets and modules a component declares are actually in the jar until it runs.
**Workaround used:** UC11 reads each component's annotations and checks the resources against the classpath in a test.
**Suggested API:** shipping compiled JavaScript from add-ons (or compiling add-on sources with the add-on's own settings), and a build-time check that every `@JsModule` / `@StyleSheet` of an add-on resolves.
Upstream: https://github.com/vaadin/flow/issues/26398

## 7. A component's tester has to be found and imported by every test

**Where it bites:** UC3 / RatingViewTest.java, UC9 / TestingViewTest.java
**Symptom:** a `@Tests` tester is only found when each test class lists its package with `@ComponentTesterPackages`, and `test(component)` returns the generic tester unless the test asks for `test(RatingStarsTester.class, component)`. A tester that needs the component's internals has to live in the component's package, which in practice means a separate tests jar for the add-on.
**Workaround used:** `@ComponentTesterPackages("com.example.components")` on each test, and typed `test(...)` calls.
**Suggested API:** testers discovered through `ServiceLoader`, so an add-on's tests jar registers its own.
Upstream: https://github.com/vaadin/browserless-test/issues/286
