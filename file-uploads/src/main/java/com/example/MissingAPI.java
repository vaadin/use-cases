package com.example;

import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.UploadDropZone;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.shared.Registration;

/**
 * Shims for upload features that have no Java API yet. Each method names its
 * {@code API-GAPS.md} entry and the upstream issue; replace the call with the
 * real API once it exists.
 */
public final class MissingAPI {

    /**
     * Name of the DOM event that reports the upload's file list, and the
     * expression that counts the files in it that have not been sent yet.
     */
    static final String FILES_CHANGED = "files-changed";
    static final String QUEUED_COUNT = "event.detail.value.filter(f => !f.complete).length";

    private MissingAPI() {
    }

    /**
     * Starts uploading the files that are queued in an upload with
     * {@code autoUpload = false}, as if the user had pressed each file's start
     * button.
     * <p>
     * Missing ({@code API-GAPS.md} #2): {@code Upload#startUpload()} — <a href=
     * "https://github.com/vaadin/flow-components/issues/1384">vaadin/flow-components#1384</a>.
     * Migrate by calling the real method.
     */
    public static void startUpload(Upload upload) {
        upload.getElement().callJsFunction("uploadFiles");
    }

    /**
     * Tells the server how many files are in the upload's list without having
     * been sent yet, every time a file is added or removed in the browser.
     * <p>
     * Missing ({@code API-GAPS.md} #2): a way to read the upload's queue from
     * the server — <a href=
     * "https://github.com/vaadin/flow-components/issues/6858">vaadin/flow-components#6858</a>.
     * Migrate by reading the queue when it is needed instead of tracking it.
     */
    public static Registration addQueueSizeListener(Upload upload,
            SerializableConsumer<Integer> listener) {
        return upload.getElement()
                .addEventListener(FILES_CHANGED,
                        event -> listener.accept(
                                event.getEventData().get(QUEUED_COUNT).asInt()))
                .addEventData(QUEUED_COUNT);
    }

    /**
     * Cancels the uploads that are running in the browser, as if the user had
     * pressed each file's cancel button in the upload's file list. The server
     * sees the request end early, so the handler completes without success.
     * <p>
     * Missing ({@code API-GAPS.md} #10): {@code Upload#interruptUpload()} that
     * stops an upload handled by an {@code UploadHandler}. Migrate by calling
     * the real method.
     */
    public static void cancelUpload(Upload upload) {
        upload.getElement().executeJs("""
                this.files.filter(file => file.uploading).forEach(file =>
                    this.dispatchEvent(new CustomEvent('file-abort',
                        { detail: { file } })));
                """);
    }

    /**
     * Makes the drop zone the only place on the page where files can be
     * dropped, and makes it refuse drags that carry no files. Without this a
     * file dropped next to the drop zone is opened by the browser in place of
     * the application, and a link or an image dragged from another browser
     * window lights up the drop zone but adds nothing.
     * <p>
     * Missing ({@code API-GAPS.md} #9): {@code UploadDropZone} that accepts
     * only drags carrying files, and a page-wide guard against stray drops.
     * Migrate by deleting the call, and {@code file-drop-guard.ts} with it.
     *
     * @return registration that removes the guard
     */
    public static Registration acceptFileDropsOnlyOn(UploadDropZone dropZone) {
        Element element = dropZone.getElement();
        // Runs again on every attach, in case Flow recreated the element.
        return dropZone.whenAttached(ui -> {
            element.executeJs("window.fileDropGuard.install(this)");
            return () -> element.executeJs("window.fileDropGuard.remove(this)");
        });
    }
}
