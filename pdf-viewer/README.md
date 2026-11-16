# PDF Viewer — use cases

A standalone Spring Boot demo of the experimental PDF viewer of Vaadin 25.4
(`com.vaadin.flow.component.pdfviewer.PdfViewer`). Each view shows one thing a
business application does with PDF documents: invoices, terms, receipts,
manuals, contracts, labels, handbooks and forms, all generated on the fly with
PDFBox.

The viewer is behind the `pdfViewerComponent` feature flag, which
`src/main/resources/vaadin-featureflags.properties` turns on. Without it, the
viewer throws an exception when it is attached.

| # | View | What it shows |
| - | ---- | ------------- |
| UC1 | Check before sending | The generated invoice inside the app; "Send to customer" turns on only once `document-load` reports the PDF has rendered. |
| UC2 | Read before accepting | Terms of service; the accept checkbox turns on once the furthest page reached is the last one, a computed signal over `pageCountSignal()`. |
| UC3 | Document library | A file list with a preview, titled with the library's name for the file (`setHeaderTitle`); a cut-off upload, a deleted file and a password-protected file each get their own explanation from the `document-error` reason. |
| UC4 | Open the manual at a page | Help questions link into a user guide at `uc4?page=N`; scrolling writes the page back into the address, and `scrollToPage` opens it there, and a page past the end is corrected. |
| UC5 | Check against the receipt | An expense claim next to its receipt, fitted with `Zoom.PAGE_FIT` and a collapsed toolbar (`setToolbarCollapsed`); stacks on a phone. |
| UC6 | Print a shipping label | The viewer shows the label of the selected order; as it can not print, "Print label" opens the label in a new tab for the browser's own PDF viewer to print. |
| UC7 | Jump to a clause | A review checklist scrolls a contract to the page with each answer, found with PDFBox text extraction on the server. |
| UC8 | Viewer in your language | The viewer's texts in English, Finnish or German, starting from the browser's language. |
| UC9 | Approve in the toolbar | "Approve" and "Reject" buttons with tooltips in the viewer's toolbar (`getToolbarActions()`); each decision opens the next waiting purchase order. |
| UC10 | Show a document once | A confidential briefing in a viewer with the download button hidden and no print, sent by the server only for the first request; a reload says it was opened already. |
| UC11 | Go to a chapter | A handbook whose contents page links to its chapters; the viewer follows the links, and the view adds its own chapter select, the current chapter and a "Back to contents", as the viewer has no way back. |
| UC12 | Fill in a PDF form | Answers from Vaadin fields are written into the PDF form's fields with PDFBox on the server, flattened, and the filled copy is shown; the viewer's download button, hidden for the empty form, saves it. |
| UC13 | Turn a sideways scan | "Turn left" and "Turn right" in the toolbar turn the current page on the server with PDFBox, turning it in the file itself (kept while the view is open). |
| UC14 | Present the results | Quarterly results as slides: the viewer's header is hidden with `setHeaderVisible(false)` and its toolbar with CSS, and the app's own bar moves between slides (`scrollToPage`), shows the slide number from `pageSignal()` and downloads with `download()`. |

## Run

```
./mvnw -pl common install -DskipTests
./mvnw -pl pdf-viewer spring-boot:run
```

Open <http://localhost:8080/>.

## API gaps

See [API-GAPS.md](API-GAPS.md): no way to search or highlight from code, no
printing, password-protected files, no translations, no browserless tester,
an out-of-range page that is silently ignored, form fields that can not be
filled in, no "view only" mode, no way back after following a link inside the
document, no signatures, no rotation, no outline, and no way to hide the
toolbar.
