# Theming

## Aura only

Every app loads the Aura theme on its app shell:

```java
@StyleSheet(Aura.STYLESHEET)
@StyleSheet("styles.css")
public class Application implements AppShellConfigurator {
```

Lumo is not loaded, so nothing Lumo-specific works:

- `LUMO_*` variants (`ButtonVariant.LUMO_ERROR`, `LUMO_CONTRAST`, …) either
  render unstyled or only work because their theme name happens to match a
  theme-neutral one. Use the theme-neutral variants: `ButtonVariant.PRIMARY`,
  `TERTIARY`, `ERROR`, `SUCCESS`, `WARNING`.
- `--lumo-*` custom properties are not defined. A declaration that uses one
  resolves to nothing, without any error.
- Theme names Aura does not style (`contrast`, for example) look the same as
  no theme at all. Pick one that is styled and fits the meaning — `warning`,
  `error`, `success` — on badges and buttons.

A module that is about theming itself may load both themes and switch between
them; such a module states in each view which theme a variant or property
applies to.

## Custom properties

Use the theme's properties instead of hard-coded values, so the views follow
the theme and its color scheme:

- `--vaadin-*` for the base tokens shared by the themes:
  `--vaadin-text-color`, `--vaadin-text-color-secondary`,
  `--vaadin-background-color`, `--vaadin-background-container`,
  `--vaadin-border-color`, `--vaadin-radius-m`, `--vaadin-padding-s`, …
- `--aura-*` for Aura's palette and type scale: `--aura-accent-color`,
  `--aura-red`, `--aura-green`, `--aura-font-size-s`, …

Check that a property exists before using it: after a build, search for its
definition under the module's `node_modules/@vaadin/`. A near-miss name such as
`--vaadin-background` for `--vaadin-background-color` silently produces a
transparent background.

## Where CSS lives

- `src/main/resources/META-INF/resources/styles.css` — app-wide rules, loaded
  by the app shell.
- `src/main/resources/META-INF/resources/uc<N>.css` — rules for one view,
  loaded with `@StyleSheet("uc<N>.css")` on the view. Prefix the class names
  with the view (`uc1-time`, `uc1-view`) so one view can not restyle another.
- `common` ships its own stylesheet next to the component that needs it (e.g.
  `base-main-layout.css` for `BaseMainLayout`).

Prefer a class name and a stylesheet over inline `getStyle().set(...)` for
anything beyond a single property.
