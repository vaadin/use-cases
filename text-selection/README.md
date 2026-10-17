# Text Selection API — use cases

A standalone Spring Boot demo of `HasTextSelection` in Vaadin Flow (resolved
from the parent pom's `flow-components.version` property). The interface lets
the server select text and move the cursor in a `TextField`, `TextArea`,
`PasswordField` or `BigDecimalField`. `EmailField`, `IntegerField` and
`NumberField` do not implement it, because browsers do not support a text
selection in `<input type="email">` or `<input type="number">`. Where the API
falls short is recorded in [API-GAPS.md](API-GAPS.md).

| # | View | What it shows |
| - | ---- | ------------- |
| UC1 | Select all on focus | The value is selected on each focus, so typing replaces it. Uses `setAutoselect(true)` and no `HasTextSelection` call; the baseline for UC2. |
| UC2 | Select after formatting | A Format button turns the value into a slug and calls `selectAll()`. The user can Tab to accept, type to replace, or click to place the cursor, which `setAutoselect(true)` would undo. |
| UC3 | Find and highlight | "Find next" selects each match in a text area with `setSelectionRange`, and wraps to the start after the last one. |
| UC4 | Jump to validation error | On a failed submit, the first invalid part of the username is selected, so the user can type over it. |
| UC5 | Insert template at cursor | Snippet buttons insert at the cursor or replace the selected text, and leave a placeholder selected. Reading the cursor needs a workaround (API-GAPS.md #1). |
| UC6 | Live selection info | A panel bound to a signal of the selection shows its range, length, word count and a preview. The signal is a workaround (API-GAPS.md #1). |
| UC7 | Selection toolbar | Buttons are enabled only while text is selected; each replaces the selection and selects the result, so actions chain. Reading the selection is a workaround (API-GAPS.md #1). |

## API surface

```java
public interface HasTextSelection extends HasElement {
    void setSelectionRange(int selectionStart, int selectionEnd);
    void selectAll();
    void setCursorPosition(int position);
}
```

Each call runs in the browser in a `setTimeout`, sets the range on the
native `<input>` or `<textarea>`, and focuses the field, because browsers do
not paint the selection of a field without focus. The `autoselect` property
is ignored for that focus. There is no way to read the selection or the
cursor back on the server; UC5–UC7 use `MissingAPI.selectionSignal` for that.

## Run

```
./mvnw -pl common install -DskipTests
./mvnw -pl text-selection spring-boot:run
```

Open <http://localhost:8080/>.
