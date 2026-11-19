package com.example.uc15;

import java.io.IOException;

import com.example.common.UseCaseDescription;
import com.example.documents.Pdf;
import com.example.documents.SampleDocuments;
import com.example.views.ApiGap;
import com.example.views.MainLayout;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.pdfviewer.PdfViewer;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.server.streams.UploadMetadata;

/**
 * UC15 — Attach a signed contract.
 * <p>
 * The supplier's signed contract has to be attached to a purchase. The user
 * uploads the PDF, sees it in the viewer, and only then submits it, so a wrong
 * or unreadable file is caught before it is stored.
 * <p>
 * The server opens each upload with PDFBox before it shows it: the viewer's
 * {@code document-error} comes from the browser and must not decide what the
 * server keeps. A file that is not a PDF, is damaged or needs a password is
 * rejected with its own message. A file PDFBox reads is shown from memory with
 * {@link PdfViewer#setSrc(com.vaadin.flow.server.streams.DownloadHandler)};
 * "Submit" turns on once {@code document-load} says the viewer could show it.
 * <p>
 * The viewer can only show the file after it has been uploaded: it takes a URL,
 * not the file the user picked in the browser (API-GAPS.md #16).
 */
@Route(value = "uc15", layout = MainLayout.class)
@PageTitle("UC15 — Attach a signed contract")
@UseCaseDescription("Checking an uploaded PDF in the viewer before it is submitted")
@Menu(order = 15, title = "UC15 — Attach a signed contract")
@StyleSheet("uc15.css")
public class AttachContractView extends VerticalLayout {

    static final int MAX_FILE_BYTES = 10 * 1024 * 1024;

    // setMaxFileSize only stops the browser. The server reads an upload into
    // memory, so it refuses one that is too large, or of unknown size, before
    // reading it.
    private final Upload upload = new Upload(UploadHandler
            .inMemory(this::fileReceived).validateMetadata(event -> {
                if (!fitsLimit(event.getFileSize())) {
                    event.reject("The file is larger than 10 MB.");
                }
            }));
    private final Paragraph status = new Paragraph();
    private final PdfViewer viewer = new PdfViewer();
    private final Button submit = new Button("Submit the contract");
    private @Nullable Pdf uploaded;

    public AttachContractView() {
        addClassName("uc15-view");
        add(new H1("UC15 — Attach a signed contract"));
        add(new Paragraph("Upload the contract the supplier signed and check "
                + "it in the viewer: is it the right contract, from the right "
                + "supplier? Submit it only then. A file that is not a PDF, is "
                + "damaged or needs a password is turned away before it is "
                + "shown."));

        upload.setMaxFiles(1);
        upload.setMaxFileSize(MAX_FILE_BYTES);
        upload.setAcceptedMimeTypes("application/pdf");
        upload.setAcceptedFileExtensions(".pdf");
        upload.addFileRejectedListener(
                event -> status.setText(event.getErrorMessage()));
        upload.setWidthFull();

        status.addClassName("uc15-status");
        status.setText("No file yet.");
        submit.addThemeVariants(ButtonVariant.PRIMARY);
        submit.setEnabled(false);
        submit.addClickListener(event -> submit());
        Div side = new Div(upload, status, submit);
        side.addClassName("uc15-side");

        viewer.addClassName("uc15-viewer");
        viewer.setVisible(false);
        viewer.addDocumentLoadListener(event -> {
            if (uploaded != null) {
                submit.setEnabled(true);
                status.setText("Check the contract, then submit it.");
            }
        });
        viewer.addDocumentErrorListener(event -> {
            uploaded = null;
            submit.setEnabled(false);
            viewer.setVisible(false);
            status.setText("The viewer can not show this file. Upload "
                    + "another copy.");
        });

        Div layout = new Div(side, viewer);
        layout.addClassNames("uc15-layout", "side-by-side");
        add(layout);

        Div demo = new Div(new H2("Files to try"),
                new Paragraph("Download one, then upload it above."),
                sample(SampleDocuments.serviceAgreement(),
                        "A contract that opens"),
                sample(SampleDocuments.damagedReport(),
                        "A PDF that was cut off"),
                sample(SampleDocuments.payslip(),
                        "A PDF that needs a password"));
        demo.addClassName("uc15-demo");
        add(demo);

        add(ApiGap.note(16, "the viewer can only show a file after it has "
                + "been uploaded, not the file the user picked."));
    }

    private void fileReceived(UploadMetadata metadata, byte[] bytes) {
        upload.clearFileList();
        submit.setEnabled(false);
        String problem = problemOf(bytes);
        if (problem != null) {
            uploaded = null;
            viewer.setVisible(false);
            status.setText(metadata.fileName() + ": " + problem);
            return;
        }
        uploaded = new Pdf(metadata.fileName(), bytes);
        viewer.setSrc(uploaded.handler());
        viewer.setVisible(true);
        status.setText("Opening " + metadata.fileName() + "…");
    }

    /**
     * Whether an upload of the given size may be read into memory.
     *
     * @param size
     *            the size the request declares, or a negative number if it does
     *            not declare one
     * @return {@code true} if the size is known and within the limit
     */
    static boolean fitsLimit(long size) {
        return size >= 0 && size <= MAX_FILE_BYTES;
    }

    /**
     * Opens an uploaded file with PDFBox, which the server can trust, unlike
     * what the browser reports.
     *
     * @param bytes
     *            the uploaded file
     * @return why the file is turned away, or {@code null} if it is a PDF that
     *         opens without a password
     */
    static @Nullable String problemOf(byte[] bytes) {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            return null;
        } catch (InvalidPasswordException e) {
            return "the PDF is protected with a password. Ask the supplier "
                    + "for a copy without one.";
        } catch (IOException e) {
            return "this is not a PDF, or it is damaged. Ask the supplier "
                    + "for a new copy.";
        }
    }

    private void submit() {
        Pdf pdf = uploaded;
        if (pdf == null) {
            return;
        }
        // A real application would store the file with the purchase here.
        status.setText("Submitted " + pdf.fileName() + ". Upload another "
                + "file to replace it.");
        submit.setEnabled(false);
    }

    private static Anchor sample(Pdf pdf, String text) {
        return new Anchor(pdf.handler(), text);
    }
}
