# PDF Viewer — API gaps discovered while building the demos

Places where the prototype `<vaadin-pdf-viewer>` web component, or the way it
reaches a Flow application, does not cover a use case or makes one awkward.
Each entry names the use case that surfaced it.

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

**Where it bites:** UC6 / `PrintLabelView.java`
**Symptom:** the download and print buttons are always in the toolbar. In
UC6 the application has its own print button on each row, so the toolbar
repeats it. Applications that show confidential documents often do not want
to offer a download at all, and there is no slot for an application's own
toolbar actions (e.g. "Approve" next to the document).
**Workaround used:** none.
**Suggested API:** properties to hide built-in actions (e.g.
`downloadVisible`, `printVisible`) and a slot for custom toolbar content.

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

## 12. No forms, annotations or signatures

**Where it bites:** no view; out of scope for a viewer, but common requests
**Symptom:** the viewer renders pages, text and links. It does not show or
fill in PDF form fields, add comments or highlights, or sign. Applications
that let users fill in a PDF form or annotate a document for review need
another solution.
**Workaround used:** none; no use case was written for this.
**Suggested API:** a decision on whether these belong in the viewer, and if
so, form field values and annotations exposed as properties and events.
