package com.example.uc5;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.TransferContext;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.server.streams.UploadMetadata;

/**
 * UC5 — Send a large file reliably.
 * <p>
 * Sending a recording or a data export of several hundred megabytes: the file
 * is streamed to a temporary file instead of being held in memory, the user
 * sees how much has arrived and roughly how long the rest will take, and can
 * cancel at any time. When the file is complete the server reports its size and
 * SHA-256 checksum, so the sender can confirm that what arrived is what they
 * sent.
 * <p>
 * Progress is also announced to screen readers at every quarter through a
 * polite live region, because the upload's own progress bar is silent. If the
 * connection drops, the transfer cannot be resumed — the user has to start the
 * whole file again. See {@code API-GAPS.md}.
 */
@Route(value = "uc5", layout = MainLayout.class)
@PageTitle("UC5 — Large file")
@UseCaseDescription("Sending a large file with progress, cancel and a checksum")
@Menu(order = 5, title = "UC5 — Large file")
public class LargeFileView extends VerticalLayout {

    static final int MAX_FILE_BYTES = 500 * 1024 * 1024;
    private static final long PROGRESS_INTERVAL_BYTES = 256 * 1024;

    private final Upload upload;
    private final ProgressBar progressBar = new ProgressBar(0, 1);
    private final Span progressText = new Span();
    private final Span announcement = new Span();
    private final Span result = new Span();
    private final Button cancel = new Button("Cancel upload");

    private long startedAt;
    private int lastAnnouncedQuarter;

    public LargeFileView() {
        add(new H1("UC5 — Send a large file reliably"));
        add(new Paragraph("Send a video, an archive or a data export of up "
                + "to 500 MB. The server stores it in a temporary file, shows "
                + "how far it has got and answers with a checksum once it is "
                + "complete."));

        upload = new Upload(UploadHandler.toTempFile(this::fileReceived)
                .whenStart(this::started)
                .onProgress(this::progressed, PROGRESS_INTERVAL_BYTES)
                .whenComplete(this::completed));
        upload.setMaxFiles(1);
        upload.setMaxFileSize(MAX_FILE_BYTES);
        upload.setWidthFull();

        progressBar.setVisible(false);
        cancel.setVisible(false);
        cancel.addClickListener(e -> upload.interruptUpload());
        announcement.getElement().setAttribute("aria-live", "polite");
        announcement.addClassName("visually-hidden");

        add(upload, progressBar, progressText, cancel, result, announcement);
    }

    private void started(TransferContext context) {
        startedAt = System.nanoTime();
        lastAnnouncedQuarter = 0;
        progressBar.setValue(0);
        progressBar.setVisible(true);
        cancel.setVisible(true);
        result.setText("");
        progressText.setText("Sending " + context.fileName() + "…");
    }

    private void progressed(TransferContext context, long transferred,
            long total) {
        double fraction = total > 0 ? (double) transferred / total : 0;
        progressBar.setValue(fraction);
        progressText.setText(megabytes(transferred) + " of " + megabytes(total)
                + timeLeft(transferred, total));
        int quarter = (int) (fraction * 4);
        if (quarter > lastAnnouncedQuarter && quarter < 4) {
            lastAnnouncedQuarter = quarter;
            announcement.setText(
                    context.fileName() + ": " + quarter * 25 + " percent sent");
        }
    }

    private void completed(TransferContext context, boolean success) {
        progressBar.setVisible(false);
        cancel.setVisible(false);
        if (success) {
            progressText.setText("");
        } else {
            progressText.setText("The upload of " + context.fileName()
                    + " stopped before it was complete. Choose the file "
                    + "again to start over.");
            announcement.setText("Upload stopped");
        }
    }

    private void fileReceived(UploadMetadata metadata, File file) {
        try {
            result.setText("Received " + metadata.fileName() + " ("
                    + megabytes(file.length()) + "). SHA-256: " + sha256(file));
            announcement.setText(metadata.fileName() + " received completely");
        } finally {
            // A real application would move the file to its storage here;
            // the demo only needs the checksum.
            file.delete();
        }
        upload.clearFileList();
    }

    private String timeLeft(long transferred, long total) {
        double seconds = (System.nanoTime() - startedAt) / 1e9;
        if (transferred == 0 || seconds < 1) {
            return "";
        }
        long remaining = Math
                .round((total - transferred) / (transferred / seconds));
        return remaining < 60 ? " · about " + remaining + " s left"
                : " · about " + (remaining + 30) / 60 + " min left";
    }

    private static String megabytes(long bytes) {
        return "%.1f MB".formatted(bytes / (1024.0 * 1024.0));
    }

    private static String sha256(File file) {
        try (InputStream in = Files.newInputStream(file.toPath())) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[64 * 1024];
            for (int read; (read = in.read(buffer)) != -1;) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
