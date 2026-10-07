# File uploads — gaps observed while building the use cases

Friction points hit while building the six use cases on Vaadin 25.4 (`Upload`, the modular `UploadManager` / `UploadButton` / `UploadDropZone` / `UploadFileList`, and the `UploadHandler` family).

What works well and is used as is: `UploadButton#setCapture(UploadCapture)` and `Upload#setCapture` (UC1, added in vaadin/flow-components#10332), header and completion validators on `UploadHandler` (`validateHeader`, `validateComplete`) for checking a file's real content before it is kept (UC1, UC3, UC4), `UploadHandler.toTempFile` with `onProgress` for large files (UC5), the `THUMBNAILS` variant of `UploadFileList` for previews while a batch uploads (UC4), and the browserless `UploadTester` / `UploadButtonTester` / `UploadDropZoneTester`, which made every use case testable without a browser, including client-side rejections.

## `Upload` is not a form field

**Where it bit us:** uc2 / `AttachDocumentsView.java`, uc3 / `ProfilePictureView.java`
**Symptom:** Attachments are part of a form, but `Upload` does not implement `HasValue`, `HasValidation`, `HasHelper` or `HasLabel`. It cannot be bound with the `Binder` that validates the rest of the form, cannot show a required indicator or an error message, and has no helper text for its constraints (file types, count, size). UC3 has the same problem from the other side: there is no value, so the upload cannot be told that a picture is already set.
**Workaround used:** The received files are kept in a list next to the `Binder`, the "required" error and the constraints are plain `Span`s under the upload, and the current picture is shown in an `Avatar` beside it.
**Suggested API:** An upload field (`HasValue<…, List<UploadedFile>>` with label, helper text, required indicator and error message) that can be bound with `Binder` and initialised with files that already exist. Tracked in vaadin/flow-components#6629, #2421, #8352 and #7134.

## A form cannot upload its files when it is sent

**Where it bit us:** uc2 / `AttachDocumentsView.java`
**Symptom:** A form whose files are processed together with its fields needs the files to wait in the browser until the user sends the form. `setAutoUpload(false)` makes them wait, but then only the user can start them, file by file: the server has no way to start the queued uploads when "Send claim" is clicked, and no way to ask whether any files are queued, which the "at least one receipt" check needs before starting. Upload's `AllFinishedEvent` cannot tell the form that its files are done either, because it fires whenever no upload happens to be running.
**Workaround used:** `MissingAPI.startUpload(upload)` calls the web component's `uploadFiles()`, and `MissingAPI.addQueueSizeListener` tracks the number of unsent files from the browser's `files-changed` event. That event is not fired when a file completes, so after a failed attempt the view resets the count itself. The view counts arrivals against the number of files queued at Send instead of relying on `AllFinishedEvent`.
**Suggested API:** `Upload#startUpload()` / `UploadManager#startUpload()` returning something that completes once all the started files have finished, with the received and failed files, plus a read-only view of the queue. Tracked in vaadin/flow-components#1384 and #6858.

## Rejected files arrive one event at a time

**Where it bit us:** uc2 / `AttachDocumentsView.java`
**Symptom:** Picking five files of which three are of the wrong type fires three `FileRejectedEvent`s. Showing one notification per event stacks three toasts.
**Workaround used:** The view collects the rejections of a round trip and reports them once from `UI#beforeClientResponse`.
**Suggested API:** One event per selection that carries all rejected files and their reasons. Tracked in vaadin/flow-components#8375.

## Files are identified only by name

**Where it bit us:** uc2 / `AttachDocumentsView.java`, uc4 / `PhotoAlbumView.java`
**Symptom:** `FileRemovedEvent` and the rejection events carry only the file name. Two receipts called `scan.pdf`, or two photos called `IMG_0001.jpg` from different phones, cannot be told apart: when one is removed from the upload's list, the server does not know which.
**Workaround used:** UC2 removes the first receipt with that name. UC4 lets the album own the photos: each gets a UUID, duplicates are renamed `IMG_0001 (2).jpg`, and photos are removed from the album rather than from the upload's list, which is cleared after every batch.
**Suggested API:** A stable per-file id on every upload event and in the upload's file list, and a way to remove a single file from the server. Tracked in vaadin/flow-components#2964, #3631 and #2965.

## Large uploads cannot be resumed

**Where it bit us:** uc5 / `LargeFileView.java`
**Symptom:** A file is sent as a single request. If the connection drops at 90 % of a 400 MB file, the user has to send the whole file again.
**Workaround used:** None. The view tells the user that the upload stopped and that they have to choose the file again.
**Suggested API:** Chunked, resumable uploads (for example the tus protocol, or `Content-Range` requests) handled by `UploadHandler`. Tracked in vaadin/flow-components#7836.

## Upload progress is not announced to screen readers

**Where it bit us:** uc5 / `LargeFileView.java`
**Symptom:** The upload's built-in progress bar updates silently, so a screen-reader user does not hear how a long upload is going or that it has finished.
**Workaround used:** A visually hidden `aria-live="polite"` region that the server updates at 25, 50 and 75 % and when the file is complete or the upload stops.
**Suggested API:** Built-in, throttled announcements of upload progress and completion. Tracked in vaadin/flow-components#783.

## No signal for the upload's state

**Where it bit us:** uc5 / `LargeFileView.java`
**Symptom:** Whether an upload is running, and how far it has got, is available only through `isUploading()` and the `whenStart` / `onProgress` / `whenComplete` callbacks. Showing the cancel button and the progress bar only while an upload runs means setting their visibility by hand in two callbacks.
**Workaround used:** `setVisible` calls in `whenStart` and `whenComplete`.
**Suggested API:** A read-only signal for the upload state (idle, uploading with bytes sent and total, finished, failed), so that `bindVisible` / `bindValue` can be used as elsewhere in Flow.
