package com.example.uc2;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.server.streams.UploadMetadata;

/**
 * UC2 — Attach documents to a form.
 * <p>
 * An expense claim: what the money was spent on, how much, and the receipts.
 * The receipts are part of the form — at least one is required, they can be
 * removed again before sending, and only PDFs and photos of up to 10 MB are
 * accepted, at most five of them.
 * <p>
 * {@link Upload} is not a form field: it cannot be bound with the
 * {@link Binder} that validates the other two fields, has no required
 * indicator, error message or helper text, and uploads every file the moment it
 * is picked rather than when the form is sent. The view therefore keeps the
 * received receipts in a draft list, removes them again on the upload's
 * file-removed event, shows the constraints and the "required" error in plain
 * text elements next to it, and reports all files rejected by one selection in
 * a single notification instead of one per file. See {@code API-GAPS.md}.
 */
@Route(value = "uc2", layout = MainLayout.class)
@PageTitle("UC2 — Attach documents")
@UseCaseDescription("Sending documents together with the rest of a form")
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
    private final List<Receipt> receipts = new ArrayList<>();
    private final List<String> rejectedInThisRoundTrip = new ArrayList<>();
    private final List<Claim> claims = new ArrayList<>();
    private final Grid<Claim> grid = new Grid<>(Claim.class, false);

    public AttachDocumentsView() {
        add(new H1("UC2 — Attach documents to a form"));
        add(new Paragraph("File an expense claim. The receipts are sent "
                + "together with the rest of the form; you can remove a "
                + "wrong one before sending."));

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

        upload = new Upload(UploadHandler.inMemory(this::receiptReceived));
        upload.setAcceptedMimeTypes("application/pdf", "image/jpeg",
                "image/png");
        upload.setAcceptedFileExtensions(".pdf", ".jpg", ".jpeg", ".png");
        upload.setMaxFiles(MAX_RECEIPTS);
        upload.setMaxFileSize(MAX_RECEIPT_BYTES);
        upload.setWidthFull();
        upload.addFileRemovedListener(e -> receiptRemoved(e.getFileName()));
        upload.addFileRejectedListener(this::receiptRejected);

        NativeLabel receiptsLabel = new NativeLabel("Receipts");
        receiptsLabel.addClassName("field-label");
        Span constraints = new Span("PDF, JPG or PNG · up to " + MAX_RECEIPTS
                + " files · 10 MB each");
        constraints.addClassName("helper-text");
        receiptsError.addClassName("error-text");
        receiptsError.setVisible(false);

        Button send = new Button("Send claim", e -> send());
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

    private void receiptReceived(UploadMetadata metadata, byte[] bytes) {
        receipts.add(new Receipt(metadata.fileName(), metadata.contentType(),
                bytes));
        receiptsError.setVisible(false);
    }

    private void receiptRemoved(String fileName) {
        // The event carries only the file name, so with two receipts of the
        // same name there is no telling which one was removed.
        receipts.stream().filter(r -> r.fileName().equals(fileName)).findFirst()
                .ifPresent(receipts::remove);
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

    private void send() {
        boolean fieldsValid = binder.validate().isOk();
        receiptsError.setVisible(receipts.isEmpty());
        if (!fieldsValid || receipts.isEmpty()) {
            return;
        }
        if (upload.isUploading()) {
            Notification.show("Wait until all receipts have been uploaded.");
            return;
        }
        ClaimForm form = binder.getBean();
        BigDecimal amount = form.getAmount();
        if (amount == null) {
            return;
        }
        claims.add(new Claim(form.getPurpose(), amount,
                receipts.stream().map(Receipt::fileName).toList()));
        grid.getDataProvider().refreshAll();
        Notification.show("Claim sent with " + receipts.size()
                + (receipts.size() == 1 ? " receipt." : " receipts."));

        binder.setBean(new ClaimForm());
        receipts.clear();
        upload.clearFileList();
    }
}
