package com.example.uc7;

import java.util.Base64;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.clipboard.Clipboard;
import com.vaadin.flow.component.clipboard.PasteFile;
import com.vaadin.flow.component.clipboard.PasteFileHandler;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC7 — Paste images and files.
 * <p>
 * {@link Clipboard#onFilePaste(com.vaadin.flow.component.Component, com.vaadin.flow.server.streams.UploadHandler)
 * Clipboard.onFilePaste} registers a per-file upload handler for paste
 * gestures: each file the browser puts on {@code event.clipboardData.files} is
 * POSTed to the URL Flow generates for the handler, and the bytes arrive on the
 * server. {@link PasteFileHandler#batch()} adds paste-aware orchestration on
 * top: {@code onStart} fires once before the first file of a paste,
 * {@code onFile} fires per file with the bytes and metadata, and
 * {@code onComplete} fires once the paste's declared file count has been
 * delivered.
 */
@Route(value = "uc7", layout = MainLayout.class)
@PageTitle("UC7 — Paste files")
@UseCaseDescription("Receiving pasted images and files on the server")
@Menu(order = 7, title = "UC7 — Paste files")
@StyleSheet("paste-drop-zone.css")
@StyleSheet("uc7.css")
public class PasteFilesView extends VerticalLayout {

    public PasteFilesView() {
        addClassName("uc7-view");
        add(new H1("UC7 — Paste images and files"));
        add(new Paragraph(
                "Click into the drop zone, then paste a screenshot or any "
                        + "file with Ctrl+V / Cmd+V. The bytes are uploaded "
                        + "to the server via the standard upload mechanism; "
                        + "image MIME types are rendered as inline previews."));

        Div dropZone = new Div();
        dropZone.addClassName("drop-zone");
        dropZone.setText("Paste a file here (Ctrl+V / Cmd+V)");
        dropZone.setWidthFull();
        // paste fires on the focused element, so the non-editable Div needs
        // to be focusable.
        dropZone.getElement().setAttribute("tabindex", "0");

        Span log = new Span("File log will appear here…");
        log.addClassName("file-log");

        ProgressBar progress = new ProgressBar();
        progress.setVisible(false);
        Span progressLabel = new Span();
        progressLabel.setVisible(false);

        VerticalLayout previews = new VerticalLayout();
        previews.setPadding(false);
        previews.setSpacing(true);

        StringBuilder logText = new StringBuilder();

        Clipboard.onFilePaste(dropZone,
                PasteFileHandler.batch().onStart(start -> {
                    previews.removeAll();
                    logText.setLength(0);
                    log.setText("");
                    progress.setMin(0);
                    progress.setMax(start.totalFiles());
                    progress.setValue(0);
                    progress.setVisible(true);
                    progressLabel.setText(
                            "Uploading 0 / " + start.totalFiles() + "…");
                    progressLabel.setVisible(true);
                }).onFile(file -> {
                    appendLog(logText, file);
                    log.setText(logText.toString());
                    if (file.contentType() != null
                            && file.contentType().startsWith("image/")) {
                        previews.add(imagePreview(file));
                    }
                    progress.setValue(progress.getValue() + 1);
                    progressLabel
                            .setText("Uploading " + (int) progress.getValue()
                                    + " / " + file.totalFiles() + "…");
                }).onComplete(complete -> {
                    progress.setVisible(false);
                    progressLabel.setText("Pasted " + complete.receivedFiles()
                            + (complete.receivedFiles() == 1 ? " file."
                                    : " files."));
                }).build());

        add(dropZone, progressLabel, progress, log, previews);
    }

    private static void appendLog(StringBuilder sb, PasteFile file) {
        sb.append(file.fileName()).append(" — ")
                .append(file.contentType() != null ? file.contentType()
                        : "unknown")
                .append(" (").append(file.size()).append(" bytes)\n");
    }

    private static Image imagePreview(PasteFile file) {
        String mime = file.contentType() != null ? file.contentType()
                : "application/octet-stream";
        String dataUrl = "data:" + mime + ";base64,"
                + Base64.getEncoder().encodeToString(file.bytes());
        Image img = new Image(dataUrl, file.fileName());
        img.setMaxWidth("400px");
        return img;
    }
}
