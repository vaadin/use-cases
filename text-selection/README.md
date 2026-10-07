# Text Selection API — use cases

A standalone Spring Boot demo of the Text Selection API for Vaadin Flow text
inputs, addressing
[vaadin/flow-components#1377](https://github.com/vaadin/flow-components/issues/1377).
It builds on the official `HasTextSelection` mixin from
[vaadin/flow-components#10266](https://github.com/vaadin/flow-components/pull/10266)
(25.4) plus the `deselect()` / `selectionSignal()` additions on the
[`feature/text-selection` branch](https://github.com/vaadin/flow-components/tree/feature/text-selection),
published as `25.4.text-selection-SNAPSHOT` in the Vaadin prereleases
repository.

| # | View | What it shows |
| - | ---- | ------------- |
| UC1 | Select all on focus | Auto-selects the value on focus so typing overwrites it. Uses the existing `setAutoselect(true)` attribute — baseline for the contrast in UC2. |
| UC2 | Post-transform select-all | A Format button slugifies the value and calls `selectAll()` so the user can Tab to accept, type to replace, or click to position the cursor for a one-character fix — a UX that `setAutoselect(true)` cannot deliver because it would re-select on every subsequent focus. |
| UC3 | Find and highlight in textarea | "Find next" walks the user through matches by selecting each one in place; wraps to the start past the last match. |
| UC4 | Jump to validation error | On a failed submit, the offending substring is selected so the user can immediately retype it. |
| UC5 | Insert template at cursor | Snippet buttons insert at the current cursor position, or replace the selected text; if the snippet has a placeholder it is left selected for the user to type over. |
| UC6 | Live selection info | Side panel reactively shows range, length, word count and a preview of the current selection — bindings are computed from `selectionSignal()`. |
| UC7 | Selection-driven transform toolbar | Toolbar of UPPERCASE / lowercase / "Quote" / Trim, enabled only when there is a selection; transforms replace in place and re-select the result so actions chain. |

## API surface

`TextField`, `TextArea`, `PasswordField`, and `BigDecimalField` implement
`com.vaadin.flow.component.shared.HasTextSelection`. `EmailField`,
`IntegerField` and `NumberField` do not — browsers don't support text
selection for `<input type="email">` / `<input type="number">`.

```java
public interface HasTextSelection extends HasElement {
    void setSelectionRange(int selectionStart, int selectionEnd); // focuses
    void selectAll();                                             // focuses
    void setCursorPosition(int position);                         // focuses
    void deselect();                                              // never focuses
    Signal<SelectionRange> selectionSignal();
}

public record SelectionRange(int start, int end, String content) {
    public int length() { return end - start; }
    public boolean isEmpty() { return start == end; }
    public static SelectionRange empty();
}
```

Names mirror `HTMLInputElement.setSelectionRange()` / `selectionStart` /
`selectionEnd`. The selection-mutating methods always focus the field
(browsers don't paint a selection on a non-focused input); the resulting
focus event reports `isFromClient() == false`, as with `Focusable.focus()`,
and `autoselect` is suppressed for that focus. `deselect()` collapses the
selection at its end and leaves focus alone. All calls are deferred via
`setTimeout(0)` on the client so a pending re-render can't reset the range.

`selectionSignal()` is read-only and pushed from the client on every
selection or cursor change, debounced so typing or drag-selecting results in
a single update. The value carries `content` so views don't have to slice the
field's value manually. Only `setSelectionRange`, `selectAll` and
`setCursorPosition` are in the released 25.4 API so far; UC4 (`deselect()`)
and UC5–UC7 (`selectionSignal()`) depend on the additions.

Clipboard integration (`copyToClipboard()` and friends) is intentionally
out of scope for this round and will be tackled separately.

## Run

```
cd text-selection
mvn spring-boot:run
```

Open <http://localhost:8080/>.
