# PDF Viewer — API gaps discovered while building the demos

Places where the prototype `<vaadin-pdf-viewer>` web component, or the way it
reaches a Flow application, does not cover a use case or makes one awkward.
Each entry names the use case that surfaced it. Where the
[VCF PDF Viewer](https://vaadin.com/directory/component/pdf-viewer) add-on
(Flow add-on 6.x, `vcf-pdf-viewer` 4.2) already covers a gap, the entry says
how, as a reference for the API.

The web component's API: `src`, `page` (synchronized with `page-changed`),
`zoom` (`page-width`, `page-fit` or a number), `pageCount` (read-only),
`sidebarOpened`, `toolbarCollapsed`, `fileName`, `fileNameVisible`, `i18n`,
`print()`, and the events `document-load` (`pageCount`, `title`) and
`document-error` (`reason`: `network`, `invalid` or `password`). The Flow side
is `com.example.pdfviewer.PdfViewer`, a wrapper this module builds itself
(#1).

## 1. No Flow component for the viewer

**Where it bites:** every use case / `com.example.pdfviewer.PdfViewer`
**Symptom:** Flow Components has no `PdfViewer`, so a Flow application can not
use the web component without writing the Java side itself: the tag and npm
package, the property synchronization for `page`, `zoom`, `sidebarOpened` and
`toolbarCollapsed`, the event classes, serving a document from a
`DownloadHandler`, and the texts.
**Workaround used:** `PdfViewer` (about 400 lines with `PdfViewerI18n`) wraps
the web component. It switches the `DownloadHandler` to inline, keeps a copy
of `pageCount` from the load events because the property is read-only, and
types the zoom and the error reason as enums.
**Suggested API:** a Flow component along the lines of the wrapper:
`setSrc(DownloadHandler)` / `setSrc(String)`, `getPage()` / `setPage(int)`,
`getPageCount()`, `setZoom(Zoom)` / `setZoom(double)`,
`setSidebarOpened(boolean)`, `setToolbarCollapsed(boolean)`,
`setFileName(String)`, `setFileNameVisible(boolean)`, `print()`,
`setI18n(PdfViewerI18n)`, `addPageChangeListener`, `addDocumentLoadListener`,
`addDocumentErrorListener`; plus the signal binding of #10.
Upstream: https://github.com/vaadin/web-components/issues/7669

## 2. The feature flag is unknown to Flow

**Where it bites:** every use case / `PdfViewerFeatureFlagProvider.java`
**Symptom:** the web component only defines `<vaadin-pdf-viewer>` once
`window.Vaadin.featureFlags.pdfViewerComponent` is set. Flow sets the flags it
knows from `vaadin-featureflags.properties`, but it only knows the flags a
`FeatureFlagProvider` lists, and none lists this one. Without a provider, the
server only logs "Unsupported feature flag is present", and in the browser
the viewer stays an empty, undefined element without an error.
**Workaround used:** `PdfViewerFeatureFlagProvider`, registered in
`META-INF/services/com.vaadin.experimental.FeatureFlagProvider`, and
`com.vaadin.experimental.pdfViewerComponent=true` in
`vaadin-featureflags.properties`.
**Suggested API:** the Flow component's module brings the provider along, like
`FlowComponentsFeatureFlagProvider` does for the other experimental
components.

## 3. The preview package has to be pinned by hand

**Where it bites:** every use case / `PdfViewer.PREVIEW_URL`, `pom.xml`
**Symptom:** the module uses the Flow Components pull request snapshot whose
`@NpmPackage` annotations point at a pkg.pr.new preview of the web components
branch. That snapshot has no annotation for `@vaadin/pdf-viewer`, because no
Flow component exists (#1), so the wrapper declares the package itself, with
the preview URL of the same commit. The two must match: every preview package
depends on the others through URLs with its own commit, so a different commit
installs a second copy of `@vaadin/component-base` and the other shared
packages. Nothing checks this; a mismatch only shows in the browser.
**Workaround used:** `PREVIEW_URL` is written out with the full commit hash,
next to a comment that says which snapshot it has to match.
**Suggested API:** the script that points Flow Components at a web components
preview could also report web component packages that have no Flow
component yet, or the Flow component of #1 removes the need.

## 4. No API to find or highlight text

**Where it bites:** UC7 / `JumpToClauseView.java`, `ClauseFinder.java`
**Symptom:** the viewer has a find bar that highlights matches (Ctrl+F or
Cmd+F in the viewer), but no property or method to fill it in, to jump to a
match, or to highlight a passage from code. An application that wants to open
a document at "the clause about termination" can only set the page.
**Workaround used:** `ClauseFinder` extracts the text of each page with PDFBox
on the server and the view sets `page` to the first page with a match. The
reader still has to find the sentence on the page, or type it into the find
bar.
**Suggested API:** a way to start a search from code that highlights and
scrolls to the first match, e.g. `find(String text)`, with an event that
reports the number of matches; optionally `highlight(int page, String text)`
for a passage the application already located.

## 5. `print()` does not report what happened

**Where it bites:** UC6 / `PrintLabelView.java`
**Symptom:** `print()` returns a promise that resolves the same way when the
user printed, cancelled the print dialog, cancelled the viewer's preparation,
or when printing failed. It also returns without doing anything when no
document has loaded yet. The application can not mark a label as printed, and
it can not print a document it has only just set as `src`.
**Workaround used:** the view sets `src` and calls `print()` from its
`document-load` listener. The log says that the print dialog was opened, not
that the label was printed.
**Suggested API:** `print()` resolves with an outcome (`printed`, `cancelled`,
`failed`, where the browser can tell), or `print-start` / `print-end` events;
and `print()` called while a document loads waits for it instead of returning.

## 6. Password-protected documents can not be opened

**Where it bites:** UC3 / `DocumentLibraryView.java`
**Symptom:** a document that needs a password fires `document-error` with the
reason `password` and shows "Password-protected documents are not
supported." There is no `password` property and no prompt. Payslips, bank
statements and medical reports are often protected exactly this way.
**Workaround used:** the view explains the error and links to the file in a
new tab, where the browser's own PDF viewer asks for the password.
**Suggested API:** a prompt in the viewer, or a `password` property with an
event that says whether it was wrong.

## 7. The toolbar's buttons can not be chosen

**Where it bites:** UC6 / `PrintLabelView.java`, UC9 /
`ApproveInToolbarView.java`, UC10 / `ShowOnceView.java`, UC13 /
`TurnScanView.java`
**Symptom:** the find, download and print buttons are always in the toolbar
(`vaadin-pdf-viewer-toolbar-mixin.js`), and the toolbar has no slot for an
application's own controls. In UC6 the application has its own print button
on each row, so the toolbar repeats it. UC9 wants "Approve" and "Reject" next
to the document. UC10 shows a confidential document that must not be
downloaded or printed.
**Workaround used:** the viewer renders its buttons into its light DOM, with
`slot="toolbar-actions"`. `PdfViewer#addToToolbar` gives the application's
buttons the same slot (UC9); a Flow `Button` with `setTooltipText(...)` gets
its tooltip as usual. UC10 hides the download and print buttons with a
stylesheet (`vaadin-pdf-viewer-button[icon='download']`). Neither is API: the
slot names and the buttons' `icon` attribute can change. Controls added
before the viewer first renders come before its own buttons, later ones after
them, and nothing else decides the order.
**Suggested API:** properties to hide built-in actions (e.g.
`downloadVisible`, `printVisible`, `findVisible`), and a documented slot for
application controls (e.g. `toolbar-end`) that is hidden with the toolbar when
it collapses; in Flow, `addToToolbar(Component...)`. The VCF add-on has
`setAddDownloadButton(boolean)` and `setAddPrintButton(boolean)` (print is off
by default) and a default slot in its toolbar: its `PdfViewer` is a `Div`, so
`add(button)` puts a button there, which is also how it adds its own buttons.

## 8. No translations, and the UI locale is not used

**Where it bites:** UC8 / `LocalizedViewerView.java`, `ViewerTexts.java`
**Symptom:** the viewer has 38 texts, most of them screen reader labels and
tooltips. It ships English only and does not follow the UI's locale, so an
application has to supply all 38 for every language. A text left out stays
English in the middle of the translated ones, which is easy to miss because
most texts are not visible.
**Workaround used:** `ViewerTexts` holds the Finnish and German texts, and the
view sets them with `setI18n(...)` when the language changes.
**Suggested API:** translations shipped with the component for the common
languages, picked from the UI's locale by the Flow component, with
`setI18n(...)` for overrides.

## 9. No browserless tester for the viewer

**Where it bites:** every view test / `src/test/.../PdfViewerTester.java`
**Symptom:** browserless tests can not load a document, so `document-load`,
`document-error` and `page-changed` never fire, and there is no stable way to
see that `print()` was called.
**Workaround used:** `PdfViewerTester` (test sources) fires the DOM events with
the data the wrapper reads, simulates the user's page change with
`setPropertyAsUser`, and detects `print()` by reading the pending JavaScript
calls through `UIInternals`.
**Suggested API:** a `PdfViewerTester` in browserless-test, with
`load(int pageCount, String title)`, `fail(reason)`, `scrollTo(int page)` and
a way to assert that printing was requested.

## 10. No signal for the current page

**Where it bites:** UC2 / `ReadBeforeAcceptingView.java`; UC4 /
`ManualDeepLinkView.java`
**Symptom:** the page is only available through a listener, and the page
count only through the load event. UC2 derives three things from them (the
progress text, whether the checkbox is enabled, whether the button is
enabled) and has to update all three from each listener; UC4 keeps the URL in
step by hand.
**Workaround used:** listeners that call one `update()` method.
**Suggested API:** `pageSignal()` and `pageCountSignal()`, or
`bindPage(WritableSignal<Integer>)`, so the furthest page and the enabled
states can be computed signals.

## 11. A page past the end is neither corrected nor reported

**Where it bites:** UC4 / `ManualDeepLinkView.java`
**Symptom:** setting `page` to a number larger than the document (a link to
`uc4?page=99` for a 12-page guide) shows page 1, keeps `page` at 99, leaves
the page field empty and fires no event. The server still believes the user
is on page 99.
**Workaround used:** the view's `document-load` listener sets the page to the
last page when it is past the end, and corrects the address.
**Suggested API:** the viewer clamps `page` to `1…pageCount` once the document
has loaded and fires `page-changed`.

## 12. PDF form fields can not be filled in

**Where it bites:** UC12 / `FillFormView.java`
**Symptom:** the viewer draws each page on a canvas and has no layer for
form fields (`pdf-viewer-page.js` renders with pdf.js's default
`annotationMode`). A form's fields are shown with the values the file holds,
but the user can not type into them, tick a checkbox or pick an option. The
application also gets no field values back.
**Workaround used:** the view asks the questions in Vaadin fields, and
`AddressChangeForm` fills in the PDF's fields on the server with PDFBox and
flattens them. The viewer then shows the filled copy, and its download button
saves it. This keeps the application in charge of the answers: it validates
them, and the file that leaves the app is the one it made.
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

## 13. Nothing keeps a document in the viewer

**Where it bites:** UC10 / `ShowOnceView.java`
**Symptom:** some documents may only be read in the application: no download,
no print, shown once. Apart from the buttons of #7, the viewer offers nothing
for this. The text layer can be selected and copied, the viewer does not stop the
browser's own print command (Ctrl+P) from printing the web page, and the
file is in the browser's memory. Screenshots can never be stopped.
**Workaround used:** the server sends the file only once (`OneTimeDocument`),
so a reload, a second tab or the file's address opened elsewhere gets 410
Gone. This works because pdf.js fetches the file in a single request: the
response of a Flow `DownloadHandler` has no `Accept-Ranges` header, so pdf.js
does not switch to range requests. For a large file from a server that offers
them, it would make several requests, and a one-time link would break.
**Suggested API:** a documented "view only" mode that turns off download and
print, and optionally text selection, with documentation that says what it
can not prevent. A note in the docs on how many requests the viewer makes
for a document, so that a one-time URL can be built safely.

## 14. Following a link inside the document has no way back

**Where it bites:** UC11 / `ContentsPageView.java`
**Symptom:** links to a place in the same document work: a click on a
contents entry scrolls to the chapter (`#followLink` in
`vaadin-pdf-viewer-mixin.js`). But the viewer does not remember where the
link was followed from: there is no "back", and the browser's Back button
leaves the page instead. The application is not told that a link was
followed; it only sees `page-changed`, like for scrolling.
**Workaround used:** the view shows the chapter of the current page and has
its own "Back to contents" button, which sets the page to 1. That works for a
contents page, not for a link in the middle of the text.
**Suggested API:** a back (and forward) control in the viewer for links
followed inside the document, optionally tied to the browser history, and a
`link-click` event with the target page or URL that the application can
cancel.

## 15. No signatures

**Where it bites:** UC12 / `FillFormView.java`
**Symptom:** the form in UC12 ends with a signature line that stays empty.
The viewer can not add a signature, and it does not show whether a
signature in a document is valid. pdf.js tracked signature validation in https://github.com/mozilla/pdf.js/issues/13351
(closed in June 2026; the web component uses pdfjs-dist 6.3.289). Whether
that can be used outside Firefox has not been checked. A signature drawn in
the browser is only an image. It does not prove who signed, and
signatures with legal weight (e.g. eIDAS advanced or qualified signatures)
are made by a signing service, not by a viewer.
**Workaround used:** none; the signature line stays empty.
**Suggested API:** keep signing on the server: document a flow where the
application gets the user's consent, signs the file with a signing service,
and shows the signed file. For the viewer, consider showing signature status
(valid, invalid, unknown) once pdf.js can check it, and leave drawn
signatures out unless a use case needs them.

## 16. Pages can not be turned

**Where it bites:** UC13 / `TurnScanView.java`
**Symptom:** a page that was scanned sideways or upside down stays that way:
the viewer has no rotate buttons and no rotation property, and it renders
every page with the rotation stored in the file. Turning the head is the only
way to read the page.
**Workaround used:** "Turn left" and "Turn right" buttons in the toolbar
(#7) turn the current page on the server with PDFBox (`PageRotation`, the
page's `/Rotate` entry) and load the document again. That also fixes the
file for the next reader, which a viewer-only rotation would not. Flow does
not send a page equal to the one it knows already, so a page set together
with the new `src` is lost and the view sets it again on `document-load`.
**Suggested API:** rotate buttons (optional, like the download and print
buttons of #7) and a `rotation` property (0, 90, 180, 270) for the whole
document; for a single page, an event with the page and the rotation, so the
application can save it. The VCF add-on has `setAddRotateClockwiseButton` and
`setAddRotateCounterClockwiseButton`, with tooltip setters; they turn all
pages, in the browser only.

## 17. The header has no title of its own

**Where it bites:** UC3 / `DocumentLibraryView.java`
**Symptom:** with `fileNameVisible`, the header shows `fileName`, else the
file name from the URL, else the title in the document's metadata
(`vaadin-pdf-viewer-print-mixin.js`). The same `fileName` is the name the file
is downloaded under. A library that calls a file "Travel policy" can not show
that in the header and still download `travel-policy.pdf`.
**Workaround used:** none; the header shows the file name.
**Suggested API:** a `title` property for the header, separate from
`fileName`. The VCF add-on has `setCustomTitle(String)`, which takes
precedence over the file name and the metadata title.
