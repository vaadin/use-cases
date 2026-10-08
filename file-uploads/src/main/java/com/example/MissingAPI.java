package com.example;

import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.shared.Registration;

/**
 * Shims for upload features that have no Java API yet. Each method names the
 * upstream issue; replace the call with the real API once it exists.
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
     * Missing: {@code Upload#startUpload()} — <a href=
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
     * Missing: a way to read the upload's queue from the server — <a href=
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
}
