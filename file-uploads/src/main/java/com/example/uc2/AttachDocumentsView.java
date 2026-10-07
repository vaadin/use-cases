package com.example.uc2;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.example.Images;
import com.example.MissingAPI;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.FileRejectedEvent;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.TransferContext;
import com.vaadin.flow.server.streams.UploadEvent;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.server.streams.UploadMetadata;

/**
 * UC2 — Attach documents to a form.
 * <p>
 * An expense claim: what the money was spent on, how much, and the receipts.
 * The receipts are part of the form — at least one is required, and only PDFs
 * and photos of up to 10 MB are accepted, at most five of them.
 * <p>
 * Nothing is sent before the user presses "Send claim": the upload has
 * {@code autoUpload} off, so picked receipts only wait in its list, where they
 * can still be removed. Send checks the fields, starts the queued uploads and,
 * once all of them have finished, processes the claim and its receipts as one:
 * every receipt must really be a PDF or a photo, and if one is not, the claim
 * is not saved and the user is told which file to replace. Receipts that did
 * arrive are kept with the draft, so only the replacement is sent next time.
 * <p>
 * Two parts of this need {@link MissingAPI}: starting the queued uploads from
 * the server, and knowing whether any receipts are queued at all, which the
 * "required" check needs. {@link Upload} is not a form field either: it cannot
 * be bound with the {@link Binder} that validates the other fields, and has no
 * required indicator, error message or helper text, so those are plain text
 * elements next to it. Files rejected by one selection are reported in a single
 * notification instead of one per file. See {@code API-GAPS.md}.
 */
@Route(value = "uc2", layout = MainLayout.class)
@PageTitle("UC2 — Attach documents")
@UseCaseDescription("Sending documents and processing them together with the rest of a form")
@Menu(order = 2, title = "UC2 — Attach documents")
public class AttachDocumentsView extends VerticalLayout {

    static final int MAX_RECEIPTS = 5;
    static final int MAX_RECEIPT_BYTES = 10 * 1024 * 1024;

    record Receipt(String fileName, String contentType, byte[] bytes) {
    }

    record Claim(String purpose, BigDecimal amount, List<String> receipts) {
    }

    /** Bean for the fields {@link Binder} can handle. */
    public static class ClaimForm {
        private String purpose = "";
        private @Nullable BigDecimal amount;

        public String getPurpose() {
            return purpose;
        }

        public void setPurpose(String purpose) {
            this.purpose = purpose;
        }

        public @Nullable BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(@Nullable BigDecimal amount) {
            this.amount = amount;
        }
    }

    private final Binder<ClaimForm> binder = new Binder<>();
    private final Upload upload;
    private final Span receiptsError = new Span("Attach at least one receipt.");
    private final Button send = new Button("Send claim", e -> send());
    private final List<Receipt> received = new ArrayList<>();
    private final List<String> failed = new ArrayList<>();
    private final List<String> rejectedInThisRoundTrip = new ArrayList<>();
    private final List<Claim> claims = new ArrayList<>();
    private final Grid<Claim> grid = new Grid<>(Claim.class, false);
    private int queued;
    private boolean sending;
    private int expected;
    private int finished;

    public AttachDocumentsView() {
        add(new H1("UC2 — Attach documents to a form"));
        add(new Paragraph("File an expense claim. Nothing is sent until you "
                + "press \"Send claim\": then the receipts are uploaded and "
                + "checked together with the rest of the form, and the claim "
                + "is only saved if all of them can be used."));

        TextField purpose = new TextField("What was it for?");
        purpose.setWidthFull();
        BigDecimalField amount = new BigDecimalField("Amount (EUR)");
        binder.forField(purpose).asRequired("Say what the expense was for")
                .bind(ClaimForm::getPurpose, ClaimForm::setPurpose);
        binder.forField(amount).asRequired("Enter the amount")
                .withValidator(a -> a.signum() > 0,
                        "The amount must be positive")
                .bind(ClaimForm::getAmount, ClaimForm::setAmount);
        binder.setBean(new ClaimForm());

        upload = new Upload(UploadHandler.inMemory(this::receiptReceived)
                .validateHeader(Images.HEADER_SIZE,
                        AttachDocumentsView::rejectUnlessReceipt)
                .whenComplete(this::receiptFinished));
        upload.setAutoUpload(false);
        upload.setAcceptedMimeTypes("application/pdf", "image/jpeg",
                "image/png");
        upload.setAcceptedFileExtensions(".pdf", ".jpg", ".jpeg", ".png");
        upload.setMaxFiles(MAX_RECEIPTS);
        upload.setMaxFileSize(MAX_RECEIPT_BYTES);
        upload.setWidthFull();
        upload.addFileRejectedListener(this::receiptRejected);
        upload.addFileRemovedListener(e -> receiptRemoved(e.getFileName()));
        MissingAPI.addQueueSizeListener(upload, count -> {
            queued = count;
            if (count > 0) {
                receiptsError.setVisible(false);
            }
        });

        NativeLabel receiptsLabel = new NativeLabel("Receipts");
        receiptsLabel.addClassName("field-label");
        Span constraints = new Span("PDF, JPG or PNG · up to " + MAX_RECEIPTS
                + " files · 10 MB each");
        constraints.addClassName("helper-text");
        receiptsError.addClassName("error-text");
        receiptsError.setVisible(false);
        send.addThemeVariants(ButtonVariant.PRIMARY);

        add(purpose, amount, receiptsLabel, upload, constraints, receiptsError,
                send);

        add(new H2("Sent claims"));
        grid.addColumn(Claim::purpose).setHeader("Purpose");
        grid.addColumn(c -> c.amount() + " €").setHeader("Amount")
                .setAutoWidth(true);
        grid.addColumn(c -> String.join(", ", c.receipts()))
                .setHeader("Receipts");
        grid.setAllRowsVisible(true);
        grid.setItems(claims);
        add(grid);
    }

    private static void rejectUnlessReceipt(UploadEvent event,
            ByteBuffer header) {
        byte[] start = new byte[Math.min(4, header.remaining())];
        header.duplicate().get(start);
        boolean pdf = "%PDF"
                .equals(new String(start, StandardCharsets.US_ASCII));
        if (!pdf && !Images.isImage(header)) {
            event.reject(event.getFileName() + " is not a PDF or a photo");
        }
    }

    private void send() {
        boolean fieldsValid = binder.validate().isOk();
        boolean hasReceipts = queued > 0 || !received.isEmpty();
        receiptsError.setVisible(!hasReceipts);
        if (!fieldsValid || !hasReceipts) {
            return;
        }
        if (queued == 0) {
            // Everything already arrived on an earlier attempt.
            process();
            return;
        }
        sending = true;
        expected = queued;
        finished = 0;
        binder.setReadOnly(true);
        send.setEnabled(false);
        send.setText("Sending receipts…");
        MissingAPI.startUpload(upload);
    }

    private void receiptReceived(UploadMetadata metadata, byte[] bytes) {
        received.add(new Receipt(metadata.fileName(), metadata.contentType(),
                bytes));
        arrived();
    }

    private void receiptFinished(TransferContext context, boolean success) {
        if (!success) {
            failed.add(context.fileName());
            arrived();
        }
    }

    private void arrived() {
        // Counted per file rather than with the all-finished event, which can
        // fire whenever no upload happens to be running.
        if (sending && ++finished >= expected) {
            process();
        }
    }

    private void receiptRemoved(String fileName) {
        // Removing a receipt that already arrived drops it from the draft. The
        // event carries only the file name, so with two receipts of the same
        // name there is no telling which one was removed.
        received.stream().filter(r -> r.fileName().equals(fileName)).findFirst()
                .ifPresent(received::remove);
    }

    /** Processes the claim and all of its receipts as one unit. */
    private void process() {
        sending = false;
        binder.setReadOnly(false);
        send.setEnabled(true);
        send.setText("Send claim");

        if (!failed.isEmpty()) {
            Notification
                    .show("The claim was not sent. " + String.join(", ", failed)
                            + (failed.size() == 1
                                    ? " is not a PDF or a photo; remove it"
                                    : " are not PDFs or photos; remove them")
                            + " and send again.");
            // The browser reports the queue only when files are added or
            // removed;
            // right now only the failed files are still waiting.
            queued = failed.size();
            failed.clear();
            return;
        }
        ClaimForm form = binder.getBean();
        BigDecimal amount = form.getAmount();
        if (amount == null) {
            return;
        }
        claims.add(new Claim(form.getPurpose(), amount,
                received.stream().map(Receipt::fileName).toList()));
        grid.getDataProvider().refreshAll();
        Notification.show("Claim sent with " + received.size()
                + (received.size() == 1 ? " receipt." : " receipts."));

        binder.setBean(new ClaimForm());
        received.clear();
        queued = 0;
        upload.clearFileList();
    }

    private void receiptRejected(FileRejectedEvent event) {
        // One event arrives per file; collect the ones of this round trip and
        // tell the user once.
        if (rejectedInThisRoundTrip.isEmpty()) {
            getUI().ifPresent(ui -> ui.beforeClientResponse(this,
                    context -> reportRejected()));
        }
        rejectedInThisRoundTrip.add(
                event.getFileName() + " (" + event.getErrorMessage() + ")");
    }

    private void reportRejected() {
        Notification.show((rejectedInThisRoundTrip.size() == 1
                ? "This file was not attached: "
                : rejectedInThisRoundTrip.size() + " files were not attached: ")
                + String.join(", ", rejectedInThisRoundTrip));
        rejectedInThisRoundTrip.clear();
    }
}
