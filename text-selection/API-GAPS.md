# Text Selection API — API gaps

Where `HasTextSelection` made a use case in this module impossible or
awkward.

## 1. The selection and the cursor can not be read on the server

**Where it bites:** UC5 / `InsertTemplateView.java`, UC6 /
`LiveSelectionInfoView.java`, UC7 / `SelectionToolbarView.java`
**Symptom:** `HasTextSelection` only writes: `setSelectionRange`,
`selectAll` and `setCursorPosition`. A view can not insert a snippet at the
cursor, show details about the selected text, or enable a button only while
text is selected, without listening to the browser itself.
**Workaround used:** `MissingAPI.selectionSignal(field)` adds a debounced DOM
listener for `selectionchange` that sends `selectionStart`, `selectionEnd`
and the selected text to the server, and keeps them in a read-only signal.
**Suggested API:** a read-only signal on the field, e.g.
`Signal<SelectionRange> selectionSignal()` with
`record SelectionRange(int start, int end, String content)`. It was
proposed in vaadin/flow-components#10356, which was closed without merging.

## 2. No browserless simulator for the text selection

**Where it bites:** every test that selects text /
`TextSelectionTestSupport.java`
**Symptom:** browserless-test has no tester method to select text in a field,
nor to read the selection the server asked the browser to make. The
`HasTextSelection` calls are `executeJs` calls that the tests can not see
through the testers.
**Workaround used:** `TextSelectionTestSupport.selectInBrowser` fires the
`selectionchange` DOM event that `MissingAPI` listens to, and
`clickAndReadSelection` reads the pending `setSelectionRange` JavaScript
calls before the response is sent.
**Suggested API:** `select(start, end)` and `getSelection()` on the text
field testers, working with the field's own selection API once it exists.

## 3. The selection can not be cleared without focusing the field

**Where it bites:** UC4 / `ValidationJumpView.java`
**Symptom:** every `HasTextSelection` method also focuses the field. After a
successful submit the view would like to collapse the selection while focus
stays on the Submit button; the closest call, `setCursorPosition`, moves
focus back to the field.
**Workaround used:** none; the view leaves the selection as it is.
**Suggested API:** `deselect()`, which collapses the selection at its end and
does not change focus (also part of vaadin/flow-components#10356).
