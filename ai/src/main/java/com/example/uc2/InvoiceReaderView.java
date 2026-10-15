package com.example.uc2;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.example.ai.AiProvider;
import com.example.ai.AiProviders;
import com.example.ai.LlmProviderFactory;
import com.example.ai.ProviderNotice;
import com.example.common.UseCaseDescription;
import com.example.uc2.SampleInvoices.Sample;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.ai.common.AIAttachment;
import com.vaadin.flow.component.ai.common.ValueSource;
import com.vaadin.flow.component.ai.form.FormAIController;
import com.vaadin.flow.component.ai.orchestrator.AIOrchestrator;
import com.vaadin.flow.component.ai.orchestrator.ResponseListener.ResponseEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC2 — Read an invoice.
 * <p>
 * An accounts-payable clerk receives invoices as PDF files or photos, each laid
 * out differently. Instead of typing the vendor, dates, amounts and bank
 * account into the form, the clerk attaches the invoice and a
 * {@link FormAIController} fills the form: the model reads the document and
 * writes each value into the matching field. The application stays in charge:
 * the {@link Binder} validates every value the model writes, including the
 * cross-field rule that net amount plus VAT equals the total, and a marker on
 * each filled field lets the clerk check or revert it before approving.
 */
@Route(value = "uc2", layout = MainLayout.class)
@UseCaseDescription("Filling in a form from an uploaded invoice, with the app's validation and a human approving the result")
@Menu(order = 2, title = "UC2 — Read an invoice")
@StyleSheet("uc2.css")
public class InvoiceReaderView extends VerticalLayout {

    static final String SYSTEM_PROMPT = """
            You help an accounts-payable clerk enter supplier invoices. Fill \
            in the form from the attached invoice. Copy every value exactly as \
            printed on the invoice; never calculate or guess a value that is \
            not printed. Use ISO currency codes. If the form rejects a value \
            because the amounts do not add up, keep the printed amounts and \
            tell the clerk in one sentence which amounts disagree. Otherwise \
            reply with one short sentence saying what you filled in.""";

    static final String READ_PROMPT = "Fill in the form from the attached "
            + "invoice.";

    private final Binder<Invoice> binder = new Binder<>(Invoice.class);
    private final ValueSignal<String> status = new ValueSignal<>(
            "Pick a sample invoice or upload your own.");
    private final ValueSignal<Integer> pendingFiles = new ValueSignal<>(0);
    private final List<Invoice> approved = new ArrayList<>();
    private final Grid<Invoice> approvedGrid = new Grid<>();
    private final AIOrchestrator orchestrator;

    public InvoiceReaderView(LlmProviderFactory llmProviders,
            AiProviders providers) {
        addClassName("uc2-view");
        add(new H1("UC2 — Read an invoice"));
        add(new Paragraph("Attach an invoice and let the AI fill in the "
                + "form. Every value it writes goes through the form's normal "
                + "validation, including a check that net amount plus VAT "
                + "equals the total. Filled fields get a marker: open it to "
                + "see where the value came from, or to undo it. Nothing is "
                + "saved until you approve the invoice."));

        ProviderNotice notice = ProviderNotice.forMissing(providers,
                AiProvider.OPENAI);
        if (notice != null) {
            add(notice);
        }

        FormLayout form = createForm();
        FormAIController controller = new FormAIController(form, binder);
        controller.setSourceTrackingEnabled(true);
        // The marker's popover shows the snippet of the invoice each value
        // was read from, so the clerk can check it without opening the PDF.
        controller.setFieldMarkerPopoverContentProvider(change -> change
                .getFieldSource().filter(source -> !source.extracts().isEmpty())
                .map(InvoiceReaderView::sourceText).orElse(null));

        Upload upload = new Upload();
        upload.setAcceptedFileTypes("application/pdf", "image/png",
                "image/jpeg");
        upload.setMaxFiles(1);
        upload.setMaxFileSize(5 * 1024 * 1024);

        orchestrator = AIOrchestrator
                .builder(llmProviders.create(), SYSTEM_PROMPT)
                .withController(controller).withFileReceiver(upload)
                .withResponseListener(this::showResult).build();

        // The orchestrator keeps the uploaded file until the next prompt but
        // does not tell whether it holds one, and the Upload fires no
        // SucceededEvent once the orchestrator owns its handler
        // (API-GAPS.md #5). Count the files from the browser's events to
        // enable the button only when there is a file to read.
        upload.getElement().addEventListener("upload-success",
                event -> pendingFiles.update(n -> n + 1));
        upload.addFileRemovedListener(
                event -> pendingFiles.update(n -> Math.max(0, n - 1)));
        Button readUploaded = new Button("Read uploaded invoice",
                click -> read(null));
        readUploaded.addThemeVariants(ButtonVariant.PRIMARY);

        FlexLayout samples = new FlexLayout();
        samples.addClassName("uc2-samples");
        SampleInvoices.ALL.forEach(sample -> samples.add(sampleCard(sample)));

        Paragraph statusText = new Paragraph();
        statusText.addClassName("uc2-status");
        statusText.bindText(status);

        Button approve = new Button("Approve for payment", click -> approve());
        approve.addThemeVariants(ButtonVariant.SUCCESS);
        Button clear = new Button("Clear form", click -> clearForm());
        clear.addThemeVariants(ButtonVariant.TERTIARY);

        if (notice != null) {
            samples.setEnabled(false);
            upload.setEnabled(false);
        }
        Signal.effect(this, () -> readUploaded
                .setEnabled(notice == null && pendingFiles.get() > 0));

        VerticalLayout source = new VerticalLayout(new H2("1. The invoice"),
                samples, upload, readUploaded);
        source.setPadding(false);
        VerticalLayout target = new VerticalLayout(
                new H2("2. Check and approve"), statusText, form,
                new Div(approve, clear));
        target.setPadding(false);
        Div columns = new Div(source, target);
        columns.addClassName("uc2-columns");
        add(columns);

        approvedGrid.addColumn(Invoice::getVendor).setHeader("Vendor")
                .setAutoWidth(true);
        approvedGrid.addColumn(Invoice::getInvoiceNumber)
                .setHeader("Invoice no.").setAutoWidth(true);
        approvedGrid.addColumn(Invoice::getDueDate).setHeader("Due")
                .setAutoWidth(true);
        approvedGrid.addColumn(i -> i.getCurrency() + " " + i.getTotalAmount())
                .setHeader("Total").setAutoWidth(true);
        approvedGrid.setAllRowsVisible(true);
        approvedGrid.setItems(approved);
        add(new H2("Approved for payment"), approvedGrid);
    }

    private FormLayout createForm() {
        TextField vendor = new TextField("Vendor");
        vendor.setId("vendor");
        TextField invoiceNumber = new TextField("Invoice number");
        invoiceNumber.setId("invoiceNumber");
        DatePicker invoiceDate = new DatePicker("Invoice date");
        invoiceDate.setId("invoiceDate");
        DatePicker dueDate = new DatePicker("Due date");
        dueDate.setId("dueDate");
        ComboBox<String> currency = new ComboBox<>("Currency");
        currency.setId("currency");
        currency.setItems("EUR", "GBP", "USD", "SEK");
        BigDecimalField netAmount = new BigDecimalField("Net amount");
        netAmount.setId("netAmount");
        BigDecimalField vatAmount = new BigDecimalField("VAT amount");
        vatAmount.setId("vatAmount");
        BigDecimalField totalAmount = new BigDecimalField("Total amount");
        totalAmount.setId("totalAmount");
        TextField iban = new TextField("IBAN");
        iban.setId("iban");
        iban.setHelperText("The vendor's bank account, without spaces");

        binder.forField(vendor).asRequired("Enter the vendor").bind("vendor");
        binder.forField(invoiceNumber).asRequired("Enter the invoice number")
                .bind("invoiceNumber");
        binder.forField(invoiceDate).asRequired("Enter the invoice date")
                .bind("invoiceDate");
        binder.forField(dueDate).asRequired("Enter the due date")
                .bind("dueDate");
        binder.forField(currency).asRequired("Pick the currency")
                .bind("currency");
        binder.forField(netAmount).asRequired("Enter the net amount")
                .bind("netAmount");
        binder.forField(vatAmount).asRequired("Enter the VAT amount")
                .bind("vatAmount");
        binder.forField(totalAmount).asRequired("Enter the total amount")
                .bind("totalAmount");
        binder.forField(iban).asRequired("Enter the IBAN")
                .withValidator(
                        value -> value.matches("[A-Z]{2}[0-9A-Z]{13,32}"),
                        "An IBAN is a country code and up to 32 letters and "
                                + "digits, without spaces")
                .bind("iban");
        binder.withValidator(Invoice::amountsAddUp,
                "Net amount plus VAT does not equal the total");
        binder.setBean(new Invoice());

        FormLayout form = new FormLayout(vendor, invoiceNumber, invoiceDate,
                dueDate, currency, netAmount, vatAmount, totalAmount, iban);
        form.setAutoResponsive(true);
        form.setAutoRows(true);
        form.setMaxColumns(2);
        form.setExpandFields(true);
        form.setWidthFull();
        return form;
    }

    private Div sampleCard(Sample sample) {
        Button read = new Button(sample.label(), click -> read(sample));
        Anchor open = new Anchor(
                DownloadHandler
                        .fromInputStream(event -> new DownloadResponse(
                                new ByteArrayInputStream(sample.toPdf()),
                                sample.fileName(), "application/pdf", -1))
                        .inline(),
                "Open the PDF");
        open.setTarget("_blank");
        Div card = new Div(read, open);
        card.addClassName("uc2-sample");
        return card;
    }

    private static Paragraph sourceText(ValueSource source) {
        return new Paragraph("Read from the invoice: " + source.extracts()
                .stream().map(extract -> "“" + extract.text() + "”")
                .collect(Collectors.joining(", ")));
    }

    /**
     * Sends the sample's PDF, or the uploaded file when {@code sample} is
     * {@code null}, to the model with the instruction to fill in the form.
     */
    private void read(@Nullable Sample sample) {
        clearForm();
        status.set("Reading the invoice…");
        if (sample == null) {
            // The prompt takes the uploaded file and clears the upload.
            orchestrator.prompt(READ_PROMPT);
            pendingFiles.set(0);
        } else {
            orchestrator.prompt(READ_PROMPT,
                    List.of(new AIAttachment(sample.fileName(),
                            "application/pdf", sample.toPdf())));
        }
    }

    private void showResult(ResponseEvent event) {
        status.set(event.getError().map(
                error -> "The invoice could not be read: " + error.getMessage())
                .orElseGet(event::getResponse));
    }

    private void approve() {
        BinderValidationStatus<Invoice> validation = binder.validate();
        if (validation.isOk()) {
            approved.add(binder.getBean());
            approvedGrid.getDataProvider().refreshAll();
            Notification.show("Invoice " + binder.getBean().getInvoiceNumber()
                    + " approved for payment");
            clearForm();
            status.set("Pick a sample invoice or upload your own.");
        } else {
            // A cross-field rule belongs to no single field, so it is not
            // highlighted on the form; show its message here.
            status.set(validation.getBeanValidationErrors().stream()
                    .map(ValidationResult::getErrorMessage).findFirst()
                    .orElse("Fix the highlighted fields before approving."));
        }
    }

    private void clearForm() {
        binder.setBean(new Invoice());
    }
}
