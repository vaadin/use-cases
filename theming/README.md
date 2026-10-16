# Theming — use cases

A standalone Spring Boot demo of making a Vaadin Flow application look the way its users and owners need: a brand from a few design tokens, dark mode, a look per customer and per user, compact screens, component variants and one-off styling, accessibility settings, and the choice between Aura and Lumo. Appearance changes apply to every view and every open tab of the session; the user's own choices are remembered in cookies. Each view exercises a single realistic scenario; `API-GAPS.md` records what Flow does not cover yet.

| # | View | What it shows |
| - | ---- | ------------- |
| UC1 | Brand with design tokens | A brand stylesheet of six tokens (accent, background, radius, font) loaded on top of the theme, for Aura and Lumo, in light and dark mode. |
| UC2 | Dark mode | Light, dark or the system's setting, remembered for the next visit; what the system prefers, reported back from the browser; hard-coded colors versus theme tokens. |
| UC3 | Theme per customer | Each customer's colors, corners, font and logo stored as data and served as a generated, validated stylesheet when its user signs in. |
| UC4 | User's own look | An accent color and a text size picked by the user, set as inline token overrides on the root element and applied live everywhere. |
| UC5 | Compact mode | A density switch that shrinks the grid, form and buttons of one work area with Aura's `small` / `large` theme names; Lumo's compact preset can only shrink the whole page. |
| UC6 | Variants or custom CSS | Generic variants that work in every theme, theme-specific ones that don't, and a custom look built from component tokens. |
| UC7 | Styling one component | Grid rows styled from their data through part names, one field through a class and its own part, one button through inline tokens, and a card whose class follows a signal. |
| UC8 | Accessible theming | High contrast, reduced motion and a wider focus ring, taken over from the system settings in one click, and a WCAG contrast check of every accent color in the app. |
| UC9 | Aura or Lumo | The running application switched between the two themes, with the most used tokens mapped between them. |

A picker in the top bar switches between Aura and Lumo from every view, and every use case works in both; where a token exists only in one theme (Aura's base font size and contrast level), the other theme gets an equivalent override. The theme is not loaded with `@StyleSheet`: an annotation-loaded stylesheet cannot be removed, and UC9 switches themes at runtime. `AppearanceSetup` adds the theme, the brand and customer stylesheets and the user's overrides to every UI from the session's `Appearance`.

Each view explains itself: an intro says what to try, the example shows the effect, and a "How it is built" section below it names the technique, the few points that make it work and the files involved, collapsed, each opening to the source packaged with the application.

Where Flow falls short is recorded, with links to the upstream issues, in [API-GAPS.md](API-GAPS.md).

## Run

```
cd theming
mvn spring-boot:run
```

Open <http://localhost:8080/>.
