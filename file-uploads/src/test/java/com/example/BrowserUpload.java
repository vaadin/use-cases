package com.example;

import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.flow.component.upload.Upload;

/**
 * Plays parts of the browser that the upload testers leave out: reporting the
 * queue size the way the {@code files-changed} event behind
 * {@link MissingAPI#addQueueSizeListener} would, and reporting uploads that
 * never reach the server, which the browser announces as {@code upload-abort}
 * or {@code upload-error}.
 */
public class BrowserUpload extends ComponentTester<Upload> {

    public BrowserUpload(Upload upload) {
        super(upload);
    }

    public void reportQueued(int count) {
        ObjectNode data = JsonNodeFactory.instance.objectNode();
        data.put(MissingAPI.QUEUED_COUNT, count);
        fireDomEvent(MissingAPI.FILES_CHANGED, data);
    }

    public void reportAborted(String fileName) {
        fireFileEvent("upload-abort", fileName);
    }

    public void reportFailed(String fileName) {
        fireFileEvent("upload-error", fileName);
    }

    private void fireFileEvent(String type, String fileName) {
        ObjectNode data = JsonNodeFactory.instance.objectNode();
        data.put("event.detail.file.name", fileName);
        // Upload's own listener for these events checks whether files are still
        // uploading; the tests report the last one to finish.
        data.put("element.files.some(file => file.uploading)", false);
        fireDomEvent(type, data);
    }
}
