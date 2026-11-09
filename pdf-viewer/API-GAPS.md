# PDF Viewer — API gaps discovered while building the demos

Places where the experimental PDF viewer of Vaadin 25.4 does not cover a use
case or makes one awkward. Each entry names the use case that surfaced it.
Where the [VCF PDF Viewer](https://vaadin.com/directory/component/pdf-viewer)
add-on (Flow add-on 6.x, `vcf-pdf-viewer` 4.2) already covers a gap, the entry
says how, as a reference for the API.

The Flow component is `com.vaadin.flow.component.pdfviewer.PdfViewer`, behind
the `pdfViewerComponent` feature flag. It loads the `@vaadin/pdf-viewer`
web component 25.4.0-alpha5. Its API: `setSrc(DownloadHandler)` /
`setSrc(String)`, `getPage()` and `pageSignal()`, `getPageCount()` and
`pageCountSignal()`, `setZoom(Zoom)` (`PAGE_WIDTH`, `PAGE_FIT`,
`Zoom.of(double)`), `getToolbarActions()` for the application's own controls,
`setI18n(PdfViewerI18n)`, and listeners for page and zoom changes,
`document-load` (`pageCount`, `title`) and `document-error`
(`DocumentErrorReason`: `NETWORK`, `INVALID`, `PASSWORD`).

## 1. No API to find or highlight text

**Where it bites:** UC7 / `JumpToClauseView.java`, `ClauseFinder.java`
**Symptom:** the viewer has no find bar, and no property or method to search
the document, to jump to a match, or to highlight a passage from code. An
application that wants to open a document at "the clause about termination"
can only set the page, and only through the workaround of #12.
**Workaround used:** `ClauseFinder` extracts the text of each page with PDFBox
on the server and the view sets `page` to the first page with a match. The
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
locale, so an application has to supply all of `PdfViewerI18n`'s twelve texts
for every language. Four of them (`link`, `goToPage`, `externalLink`,
`download`) are for links and a download button that the web component the
Flow component loads does not have yet (#9, #13); an application has to
translate them before it can see where they appear.
**Workaround used:** `ViewerTexts` holds the Finnish and German texts, and the
view sets them with `setI18n(...)` when the language changes.
**Suggested API:** translations shipped with the component for the common
languages, picked from the UI's locale by the Flow component, with
`setI18n(...)` for overrides.

## 5. No browserless tester for the viewer

**Where it bites:** every view test / `src/test/.../PdfViewerTester.java`
**Symptom:** browserless tests can not load a document, so `document-load`,
`document-error` and `page-changed` never fire.
**Workaround used:** `PdfViewerTester` (test sources) fires the DOM events
with the data the Flow component reads, and simulates the user's page change
with `setPropertyAsUser`.
**Suggested API:** a `PdfViewerTester` in browserless-test, with
`load(int pageCount, String title)`, `fail(DocumentErrorReason)` and
`scrollTo(int page)`.

## 6. A page past the end is neither corrected nor reported

**Where it bites:** UC4 / `ManualDeepLinkView.java`
**Symptom:** setting `page` to a number larger than the document (a link to
`uc4?page=99` for a 12-page guide) logs a warning in the browser, does not
scroll, keeps `page` at 99, leaves the page field empty and fires no event.
`getPage()` and `pageSignal()` still say 99.
**Workaround used:** the view's `document-load` listener sets the page to the
last page when it is past the end, and corrects the address.
**Suggested API:** the viewer clamps `page` to `1…pageCount` once the document
has loaded and fires `page-changed`.

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
no print, shown once. The viewer has no download or print button today, but
nothing says that this is a promise. The text layer can be selected and copied,
the viewer does not stop the browser's own print command (Ctrl+P) from
printing the web page, and the file is in the browser's memory. Screenshots
can never be stopped.
**Workaround used:** the server sends the file only once (`OneTimeDocument`),
so a reload, a second tab or the file's address opened elsewhere gets 410
Gone. This works because the viewer fetches the file in a single request
(`disableRange: true` in `pdf-viewer-document-controller.js`), which the
viewer's source states but its documentation does not.
**Suggested API:** a documented "view only" mode that keeps download and print
off, and optionally text selection, with documentation that says what it can
not prevent; and a note in the docs that the viewer fetches a document in one
request, so that a one-time URL can be built safely.

## 9. Links inside the document are not followed

**Where it bites:** UC11 / `ContentsPageView.java`
**Symptom:** the handbook's contents page links to its chapters, but the
viewer draws the pages without their links: a click on a contents entry does
nothing. A links layer was added to the web component after 25.4.0-alpha5,
the version the Flow component loads. Even with links, the application would
not be told that a link was followed, and there would be no way back to where
it was followed from.
**Workaround used:** the view offers the chapters in its own select, which
sets the page (#12), and a "Back to contents" button, which sets the page to
1. The application knows the chapter pages because it generated the handbook;
for a document it did not write, it would have to read the outline or the
links with PDFBox.
**Suggested API:** the links layer in the version the Flow component loads; a
back (and forward) control for links followed inside the document; and a
`link-click` event with the target page or URL that the application can
cancel.

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
the view is open. `setSrc` starts the new document at page 1, so the view sets
the page again on `document-load` (#12).
**Suggested API:** optional rotate buttons and a `rotation` property (0, 90,
180, 270) for the whole document; for a single page, an event with the page
and the rotation, so the application can save it. The VCF add-on has
`setAddRotateClockwiseButton` and `setAddRotateCounterClockwiseButton`, with
tooltip setters; they turn all pages, in the browser only.

## 12. Properties of the web component that Flow does not expose

**Where it bites:** UC3 / `DocumentLibraryView.java`, UC4 /
`ManualDeepLinkView.java`, UC5 / `CheckReceiptView.java`, UC7 /
`JumpToClauseView.java`, UC11 / `ContentsPageView.java`, UC13 /
`TurnScanView.java`
**Symptom:** the web component has a writable `page`, a `headerTitle`, a
`headerHidden` and a `toolbarCollapsed` property. `PdfViewer` has
`getPage()` and `pageSignal()` but no `setPage(int)`, and nothing for the
header or the toolbar. Opening a document at a page (UC4, UC7, UC11, UC13),
showing a library's name for a file instead of its file name (UC3) and giving
a small receipt all the room (UC5) all need one of them.
**Workaround used:** `com.example.MissingAPI` sets the web component's
property on the element: `setPage`, `setHeaderTitle` and
`setToolbarCollapsed`. `getPage()`, `pageSignal()` and the page change
listeners see a page set this way, as they read the same property. `setSrc`
resets the page to 1, so the page has to be set after the source.
**Suggested API:** `setPage(int)` (or a `bindPage(Signal<Integer>)` next to
`pageSignal()`), `setHeaderTitle(String)`, `setHeaderVisible(boolean)` and
`setToolbarCollapsed(boolean)` with a matching signal or event. The VCF
add-on has `setCustomTitle(String)` for the header.

## 13. No download button

**Where it bites:** UC12 / `FillFormView.java`
**Symptom:** the viewer has no download button and `PdfViewer` has no
`download()`. After the server has filled in a form, the user can see it but
not save it from the viewer. A download button, with a property to hide it,
was added to the web component after 25.4.0-alpha5, the version the Flow
component loads; `PdfViewerI18n` already has its `download` text (#4).
**Workaround used:** a "Download the filled form" link under the form, with
the same `DownloadHandler` as the viewer.
**Suggested API:** the download button in the version the Flow component
loads, with `setDownloadButtonVisible(boolean)` and `download()` on the Flow
component.

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
