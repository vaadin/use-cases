# File uploads — gaps observed while building the use cases

Friction points hit while building the six use cases on Vaadin 25.4 (`Upload`, the modular `UploadManager` / `UploadButton` / `UploadDropZone` / `UploadFileList`, and the `UploadHandler` family).

What works well and is used as is: `UploadButton#setCapture(UploadCapture)` and `Upload#setCapture` (UC1, added in vaadin/flow-components#10332), header and completion validators on `UploadHandler` (`validateHeader`, `validateComplete`) for checking a file's real content before it is kept (UC1, UC3, UC4), `UploadHandler.toTempFile` with `onProgress` for large files (UC5), the `THUMBNAILS` variant of `UploadFileList` for previews while a batch uploads (UC4), and the browserless `UploadTester` / `UploadButtonTester` / `UploadDropZoneTester`, which made every use case testable without a browser, including client-side rejections.

## 1. `Upload` is not a form field

**Where it bites:** UC2 / AttachDocumentsView.java, UC3 / ProfilePictureView.java
**Symptom:** Attachments are part of a form, but `Upload` does not implement `HasValue`, `HasValidation`, `HasHelper` or `HasLabel`. It cannot be bound with the `Binder` that validates the rest of the form, cannot show a required indicator or an error message, and has no helper text for its constraints (file types, count, size). UC3 has the same problem from the other side: there is no value, so the upload cannot be told that a picture is already set.
**Workaround used:** The received files are kept in a list next to the `Binder`, the "required" error and the constraints are plain `Span`s under the upload, and the current picture is shown in an `Avatar` beside it.
**Suggested API:** An upload field (`HasValue<…, List<UploadedFile>>` with label, helper text, required indicator and error message) that can be bound with `Binder` and initialised with files that already exist.
Upstream: https://github.com/vaadin/flow-components/issues/6629
Upstream: https://github.com/vaadin/flow-components/issues/2421
Upstream: https://github.com/vaadin/flow-components/issues/8352
Upstream: https://github.com/vaadin/flow-components/issues/7134

## 2. A form cannot upload its files when it is sent

**Where it bites:** UC2 / AttachDocumentsView.java
**Symptom:** A form whose files are processed together with its fields needs the files to wait in the browser until the user sends the form. `setAutoUpload(false)` makes them wait, but then only the user can start them, file by file: the server has no way to start the queued uploads when "Send claim" is clicked, and no way to ask whether any files are queued, which the "at least one receipt" check needs before starting. Upload's `AllFinishedEvent` cannot tell the form that its files are done either, because it fires whenever no upload happens to be running.
**Workaround used:** `MissingAPI.startUpload(upload)` calls the web component's `uploadFiles()`, and `MissingAPI.addQueueSizeListener` tracks the number of unsent files from the browser's `files-changed` event. That event is not fired when a file completes, so after a failed attempt the view resets the count itself. The view counts arrivals against the number of files queued at Send instead of relying on `AllFinishedEvent`, including files the browser reports as cancelled or failed, so that a file that never reaches the server cannot leave the form waiting. Adding receipts is locked while sending by disabling the upload button and drop; disabling the upload itself would also stop the running uploads, because upload requests to a disabled component are refused.
**Suggested API:** `Upload#startUpload()` / `UploadManager#startUpload()` returning something that completes once all the started files have finished, with the received and failed files, plus a read-only view of the queue.
Upstream: https://github.com/vaadin/flow-components/issues/1384
Upstream: https://github.com/vaadin/flow-components/issues/6858

## 3. `whenComplete` does not say why an upload failed

**Where it bites:** UC2 / AttachDocumentsView.java
**Symptom:** A file refused by a validator with `event.reject("dinner.pdf is not a PDF or a photo")` reaches `whenComplete((context, success) -> …)` with `success == false`, but `context.exception()` is `null`, so the form cannot tell a refused file from a broken connection or show the reason it gave.
**Workaround used:** A `TransferProgressListener` passed to `UploadHandler.inMemory(callback, listener)`, whose `onError(context, reason)` receives the `UploadRejectedException` with the message.
**Suggested API:** Fill `TransferContext#exception()` in the context handed to `whenComplete`, or offer `whenFailed((context, reason) -> …)`.

## 4. Rejected files arrive one event at a time

**Where it bites:** UC2 / AttachDocumentsView.java
**Symptom:** Picking five files of which three are of the wrong type fires three `FileRejectedEvent`s. Showing one notification per event stacks three toasts.
**Workaround used:** The view collects the rejections of a round trip and reports them once from `UI#beforeClientResponse`.
**Suggested API:** One event per selection that carries all rejected files and their reasons.
Upstream: https://github.com/vaadin/flow-components/issues/8375

## 5. Files are identified only by name

**Where it bites:** UC2 / AttachDocumentsView.java, UC4 / PhotoAlbumView.java
**Symptom:** `FileRemovedEvent` and the rejection events carry only the file name. Two receipts called `scan.pdf`, or two photos called `IMG_0001.jpg` from different phones, cannot be told apart: when one is removed from the upload's list, the server does not know which.
**Workaround used:** UC2 removes the first receipt with that name. UC4 lets the album own the photos: each gets a UUID, duplicates are renamed `IMG_0001 (2).jpg`, and photos are removed from the album rather than from the upload's list, which is cleared after every batch.
**Suggested API:** A stable per-file id on every upload event and in the upload's file list, and a way to remove a single file from the server.
Upstream: https://github.com/vaadin/flow-components/issues/2964
Upstream: https://github.com/vaadin/flow-components/issues/3631
Upstream: https://github.com/vaadin/flow-components/issues/2965

## 6. Large uploads cannot be resumed

**Where it bites:** UC5 / LargeFileView.java
**Symptom:** A file is sent as a single request. If the connection drops at 90 % of a 400 MB file, the user has to send the whole file again.
**Workaround used:** None. The view tells the user that the upload stopped and that they have to choose the file again.
**Suggested API:** Chunked, resumable uploads (for example the tus protocol, or `Content-Range` requests) handled by `UploadHandler`.
Upstream: https://github.com/vaadin/flow-components/issues/7836

## 7. Upload progress is not announced to screen readers

**Where it bites:** UC5 / LargeFileView.java
**Symptom:** The upload's built-in progress bar updates silently, so a screen-reader user does not hear how a long upload is going or that it has finished.
**Workaround used:** A visually hidden `aria-live="polite"` region that the server updates at 25, 50 and 75 % and when the file is complete or the upload stops.
**Suggested API:** Built-in, throttled announcements of upload progress and completion.
Upstream: https://github.com/vaadin/flow-components/issues/783

## 8. No signal for the upload's state

**Where it bites:** UC5 / LargeFileView.java
**Symptom:** Whether an upload is running, and how far it has got, is available only through `isUploading()` and the `whenStart` / `onProgress` / `whenComplete` callbacks. Showing the cancel button and the progress bar only while an upload runs means setting their visibility by hand in two callbacks.
**Workaround used:** `setVisible` calls in `whenStart` and `whenComplete`.
**Suggested API:** A read-only signal for the upload state (idle, uploading with bytes sent and total, finished, failed), so that `bindVisible` / `bindValue` can be used as elsewhere in Flow.

## 9. The drop zone takes any drag, and the rest of the page takes files

**Where it bites:** UC4 / PhotoAlbumView.java
**Symptom:** `UploadDropZone` accepts every drag: a link or an image dragged from another browser window lights it up with a copy cursor, but the drop carries no file, so nothing is added and the user gets no feedback. Next to the drop zone the opposite happens: a photo dropped a few pixels outside it is not refused but opened by the browser in place of the application.
**Workaround used:** `MissingAPI.acceptFileDropsOnlyOn(dropZone)` installs window-level drag listeners that refuse drags carrying files outside the drop zone and drags carrying no files on it.
**Suggested API:** A drop zone that only reacts to drags that carry files, and an option (on `UploadManager` or the drop zone) to refuse file drops on the rest of the page.

## 10. `Upload` cannot hide its own progress, and cannot be cancelled from the server

**Where it bites:** UC5 / LargeFileView.java
**Symptom:** A view that shows the progress the server measures (with `onProgress`) gets a second progress bar from the file list of `Upload`, which has no option to hide it. Hiding it also hides the list's cancel button, and the server cannot stand in for it: `Upload#interruptUpload()` only sets a flag that nothing reads when the upload is handled by an `UploadHandler`, so the upload keeps running. The modular `UploadManager` with an `UploadButton` and no `UploadFileList` has no second progress bar, but no `interruptUpload()` at all.
**Workaround used:** The file list of `Upload` is hidden with CSS. `MissingAPI.cancelUpload(upload)` aborts the running upload in the browser, as the list's cancel button would. The list is cleared after every upload, failed ones included, so that a failed file does not keep the single file slot taken.
**Suggested API:** `interruptUpload()` that stops uploads handled by an `UploadHandler`, on both `Upload` and `UploadManager`, and an option on `Upload` to hide its file list.
