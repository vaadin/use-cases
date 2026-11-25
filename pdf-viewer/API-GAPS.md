# PDF Viewer — API gaps discovered while building the demos

Places where the experimental PDF viewer of Vaadin 25.4 does not cover a use
case or makes one awkward. Each entry names the use case that surfaced it.
Where the [VCF PDF Viewer](https://vaadin.com/directory/component/pdf-viewer)
add-on (Flow add-on 6.x, `vcf-pdf-viewer` 4.2) already covers a gap, the entry
says how, as a reference for the API.

The Flow component is `com.vaadin.flow.component.pdfviewer.PdfViewer`, behind
the `pdfViewerComponent` feature flag. It loads the `@vaadin/pdf-viewer`
web component 25.4.0-alpha6. Its API: `setSrc(DownloadHandler)` /
`setSrc(String)`, `getPage()`, `pageSignal()` and `scrollToPage(int)`,
`getPageCount()` and `pageCountSignal()`, `setZoom(Zoom)` (`PAGE_WIDTH`,
`PAGE_FIT`, `Zoom.of(double)`), `setHeaderVisible(boolean)`,
`setHeaderTitle(String)` and `setToolbarCollapsed(boolean)`,
`getToolbarActions()` for the application's own controls, a download button
(`setDownloadButtonVisible(boolean)`, `setFileName(String)`, `download()`),
`setI18n(PdfViewerI18n)`, and listeners for page, zoom and toolbar changes,
`document-load` (`pageCount`, `title`) and `document-error`
(`DocumentErrorReason`: `NETWORK`, `INVALID`, `PASSWORD`). The viewer follows
links inside the document.

## 1. No API to find or highlight text

**Where it bites:** UC7 / `JumpToClauseView.java`, `ClauseFinder.java`
**Symptom:** the viewer has no find bar, and no property or method to search
the document, to jump to a match, or to highlight a passage from code. An
application that wants to open a document at "the clause about termination"
can only scroll to the page with `scrollToPage(int)`.
**Workaround used:** `ClauseFinder` extracts the text of each page with PDFBox
on the server and the view scrolls to the first page with a match. The
reader still has to find the sentence on the page.
**Suggested API:** a find bar for the user, and a way to start a search from
code that highlights and scrolls to the first match, e.g. `find(String text)`,
with an event that reports the number of matches; optionally
`highlight(int page, String text)` for a passage the application already
located.

## 2. No way to print

**Where it bites:** UC6 / `PrintLabelView.java`
**Symptom:** the viewer has no print button and `PdfViewer` has no `print()`
method. An application that prints shipping labels has to leave the viewer for
it. It is also not told whether the user printed or cancelled, so it can not
mark a label as printed.
**Workaround used:** each order row has a "Print label" link that opens the
label, served inline, in a new tab, where the browser's own PDF viewer prints
it. That is a second step for the user and a second window.
**Suggested API:** a `print()` method on the Flow component, an optional print
button in the toolbar, and an outcome (`printed`, `cancelled`, `failed`, where
the browser can tell) as a return value or events. `print()` called while a
document loads should wait for it. The VCF add-on has
`setAddPrintButton(boolean)` (print is off by default).

## 3. Password-protected documents can not be opened

**Where it bites:** UC3 / `DocumentLibraryView.java`
**Symptom:** a document that needs a password fires `document-error` with
`DocumentErrorReason.PASSWORD`. There is no `password` property and no prompt.
Payslips, bank statements and medical reports are often protected exactly this
way.
**Workaround used:** the view explains the error and links to the file in a
new tab, where the browser's own PDF viewer asks for the password.
**Suggested API:** a prompt in the viewer, or a `setPassword(String)` with an
event that says whether it was wrong.

## 4. No translations, and the UI locale is not used

**Where it bites:** UC8 / `LocalizedViewerView.java`, `ViewerTexts.java`
**Symptom:** the viewer ships English only and does not follow the UI's
locale, so an application has to supply all of `PdfViewerI18n`'s nineteen texts
for every language.
**Workaround used:** `ViewerTexts` holds the Finnish and German texts, and the
view sets them with `setI18n(...)` when the language changes.
**Suggested API:** translations shipped with the component for the common
languages, picked from the UI's locale by the Flow component, with
`setI18n(...)` for overrides.

## 5. No browserless tester for the viewer

**Where it bites:** every view test / `src/test/.../PdfViewerTester.java`
**Symptom:** browserless tests can not load a document, so `document-load`,
`document-error` and `page-changed` never fire, and `scrollToPage(int)` is a
JavaScript call that changes nothing on the server.
**Workaround used:** `PdfViewerTester` (test sources) fires the DOM events
with the data the Flow component reads, and simulates the user's page change
with `setPropertyAsUser`. The test starts an `ExecutedJavaScript` capture
before it navigates, and the tester reads the pages the view scrolled to from
the calls it holds.
**Suggested API:** a `PdfViewerTester` in browserless-test, with
`load(int pageCount, String title)`, `fail(DocumentErrorReason)` and
`scrollTo(int page)`, that also applies `scrollToPage(int)` to `getPage()`.

## 6. A page past the end is ignored without a word

**Where it bites:** UC4 / `ManualDeepLinkView.java`
**Symptom:** `scrollToPage(int)` with a page past the end (a link to
`uc4?page=99` for a 12-page guide) logs a warning in the browser, does not
scroll and fires no event. The application is not told, so the address still
says page 99 while the viewer shows page 1.
**Workaround used:** the view remembers the page it asked for, and its
`document-load` listener scrolls to the last page when the page is past the
end, and corrects the address.
**Suggested API:** `scrollToPage(int)` clamps the page to `1…pageCount` once
the document has loaded, or reports the page it went to (a return value or a
`page-changed` event, also when the page does not change).

## 7. PDF form fields can not be filled in

**Where it bites:** UC12 / `FillFormView.java`
**Symptom:** the viewer draws each page on a canvas, with a text layer but no
layer for form fields (`pdf-viewer-page.js`). A form's fields are shown with
the values the file holds, but the user can not type into them, tick a
checkbox or pick an option. The application also gets no field values back.
**Workaround used:** the view asks the questions in Vaadin fields, and
`AddressChangeForm` fills in the PDF's fields on the server with PDFBox and
flattens them. The viewer then shows the filled copy. This keeps the
application in charge of the answers: it validates them, and the file that
leaves the app is the one it made.
**Suggested API:** a decision whether the viewer should edit forms at all.
If it does: an editable form layer, the field values as a property with a
change event (so the server sees the answers without parsing the PDF), and a
way to get the filled file back (e.g. `getFilledDocument()`, pdf.js's
`saveDocument()`). Saving brings its own questions: where the filled file goes,
and whether the server should trust it. The VCF add-on renders a form layer
by default (`setRenderInteractiveForms`, pdf.js `AnnotationMode.ENABLE_FORMS`),
so users can type into fields, but the values never reach the server and its
download button fetches the original file again, so the answers are lost. That
is the half to avoid.

## 8. Nothing keeps a document in the viewer

**Where it bites:** UC10 / `ShowOnceView.java`
**Symptom:** some documents may only be read in the application: no download,
no print, shown once. `setDownloadButtonVisible(false)` hides the download
button, and the viewer has no print button today, but nothing says that this
is a promise, and the document's bytes are still in the browser for
`download()` to save. The text layer can be selected and copied,
the viewer does not stop the browser's own print command (Ctrl+P) from
printing the web page, and the file is in the browser's memory. Screenshots
can never be stopped.
**Workaround used:** the view hides the download button, and the server sends
the file only once (`OneTimeDocument`),
so a reload, a second tab or the file's address opened elsewhere gets 410
Gone. This works because the viewer fetches the file in a single request
(`disableRange: true` in `pdf-viewer-document-controller.js`), which the
viewer's source states but its documentation does not.
**Suggested API:** a documented "view only" mode that keeps download (also
`download()`) and print off, and optionally text selection, with documentation that says what it can
not prevent; and a note in the docs that the viewer fetches a document in one
request, so that a one-time URL can be built safely.

## 9. No way back after following a link inside the document

**Where it bites:** UC11 / `ContentsPageView.java`
**Symptom:** the handbook's contents page links to its chapters, and the
viewer follows the links. But it has no back (or forward) control, so after a
link the reader has to find the way to the contents page again, and the
application is not told that a link was followed, so it can not offer the way
back itself or stop a link.
**Workaround used:** a "Back to contents" button that scrolls to page 1. It
works because the contents page of this handbook is page 1; for a link from
another page, the application does not know where the reader came from.
**Suggested API:** a back (and forward) control for links followed inside the
document, and a `link-click` event with the source page and the target page or
URL that the application can cancel.

## 10. No signatures

**Where it bites:** UC12 / `FillFormView.java`
**Symptom:** the form in UC12 ends with a signature line that stays empty.
The viewer can not add a signature, and it does not show whether a
signature in a document is valid. pdf.js tracked signature validation in
https://github.com/mozilla/pdf.js/issues/13351 (closed in June 2026; the web
component uses pdfjs-dist 6.4.299). Whether that can be used outside Firefox
has not been checked. A signature drawn in the browser is only an image. It
does not prove who signed, and signatures with legal weight (e.g. eIDAS
advanced or qualified signatures) are made by a signing service, not by a
viewer.
**Workaround used:** none; the signature line stays empty.
**Suggested API:** keep signing on the server: document a flow where the
application gets the user's consent, signs the file with a signing service,
and shows the signed file. For the viewer, consider showing signature status
(valid, invalid, unknown) once pdf.js can check it, and leave drawn
signatures out unless a use case needs them.

## 11. Pages can not be turned

**Where it bites:** UC13 / `TurnScanView.java`
**Symptom:** a page that was scanned sideways or upside down stays that way:
the viewer has no rotate buttons and no rotation property, and it renders
every page with the rotation stored in the file. Turning the head is the only
way to read the page.
**Workaround used:** "Turn left" and "Turn right" buttons in the toolbar
(`getToolbarActions()`) turn the current page on the server with PDFBox
(`PageRotation`, the page's `/Rotate` entry) and load the document again. The
file itself is turned, so an application that saves it fixes the page for the
next reader, which a viewer-only rotation would not; the demo keeps it while
the view is open. `setSrc` starts the new document at page 1, so the view
calls `scrollToPage(int)` to go back to the turned page.
**Suggested API:** optional rotate buttons and a `rotation` property (0, 90,
180, 270) for the whole document; for a single page, an event with the page
and the rotation, so the application can save it. The VCF add-on has
`setAddRotateClockwiseButton` and `setAddRotateCounterClockwiseButton`, with
tooltip setters; they turn all pages, in the browser only.

## 14. No outline of the document's bookmarks

**Where it bites:** UC4 / `ManualDeepLinkView.java`
**Symptom:** the user guide has bookmarks for its chapters, but the viewer has
no sidebar or outline to show them, and no thumbnails. A reader who opens a
long guide can only page or scroll through it.
**Workaround used:** the view lists help questions that link to the chapter
pages, which the application knows because it generated the guide.
**Suggested API:** an optional sidebar with the outline (and thumbnails), and
an `outline` the server can read after `document-load`, so an application can
build its own navigation for documents it did not write. The VCF add-on has a
sidebar with an outline and thumbnails (`setSidebarOpened`).

## 15. The toolbar can not be hidden

**Where it bites:** UC14 / `PresentResultsView.java`
**Symptom:** an application that has its own controls for the viewer wants it
to show only the pages. `setHeaderVisible(false)` hides the header, but then
the toolbar is always shown: `setToolbarCollapsed(true)` only collapses it
while the header is visible (`vaadin-pdf-viewer-toolbar-mixin.js`). So either
the header or the toolbar stays.
**Workaround used:** the view hides the header with `setHeaderVisible(false)`
and the toolbar with `::part(toolbar) { display: none; }` in `uc14.css`. The
`toolbar` part is documented for styling, but hiding it is not a documented
use; the viewer's keyboard shortcuts for the pages still work.
**Suggested API:** `setToolbarVisible(boolean)` (a `toolbarHidden` property),
independent of the header, so the application can drive the viewer with
`scrollToPage(int)`, `setZoom(Zoom)` and `download()` from its own controls.

## 16. A file can only be shown once it is uploaded

**Where it bites:** UC15 / `AttachContractView.java`
**Symptom:** the viewer takes a URL (`setSrc(String)`) or a
`DownloadHandler`, so it can only show a file the server can serve. A file the
user picks in the `Upload` has to be uploaded in full before the user can see
whether it is the right one, and the server has to keep it, at least in
memory, until the user decides. For a large scan on a slow connection, that is
a long wait for a file that may be the wrong one.
**Workaround used:** the view uploads the file into memory
(`UploadHandler.inMemory`), checks it with PDFBox and serves it back to the
viewer from memory. The bytes go to the server and back to the browser.
**Suggested API:** a way to show the file the user picked before it is
uploaded, e.g. letting the viewer show a file selected in an `Upload` that
does not upload by itself, with the upload started once the user confirms.
