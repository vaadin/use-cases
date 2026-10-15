package com.example.uc12;

import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import com.example.common.UseCaseDescription;
import com.example.documents.Pdf;
import com.example.pdfviewer.PdfViewer;
import com.example.uc12.AddressChangeForm.Field;
import com.example.views.ApiGap;
import com.example.views.MainLayout;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC12 — Fill in a PDF form.
 * <p>
 * A pension fund only accepts its own PDF form, AP-12, for a change of address.
 * The user answers its questions in ordinary Vaadin fields; the server writes
 * the answers into the PDF's form fields with PDFBox, flattens them, and shows
 * the filled copy in the viewer. The viewer's download button then gives the
 * user the filled form.
 * <p>
 * The viewer does not let the user type into a PDF form (API-GAPS.md #12).
 * Filling the form on the server also keeps the application in charge of the
 * answers: it validates them, and the PDF that leaves the app is the one it
 * made. The form's signature line stays empty; the viewer can not sign
 * (API-GAPS.md #15).
 */
@Route(value = "uc12", layout = MainLayout.class)
@PageTitle("UC12 — Fill in a PDF form")
@UseCaseDescription("Filling in an official PDF form and showing the filled copy")
@Menu(order = 12, title = "UC12 — Fill in a PDF form")
@StyleSheet("uc12.css")
public class FillFormView extends VerticalLayout {

    private static final DateTimeFormatter DATE = DateTimeFormatter
            .ofPattern("d MMMM yyyy", Locale.ENGLISH);
    private static final String FILE_NAME = "change-of-address-AP-12.pdf";

    private final TextField name = new TextField(Field.NAME.label());
    private final TextField customerNumber = new TextField(
            Field.CUSTOMER_NUMBER.label());
    private final TextArea oldAddress = new TextArea(Field.OLD_ADDRESS.label());
    private final TextArea newAddress = new TextArea(Field.NEW_ADDRESS.label());
    private final DatePicker movingDate = new DatePicker(
            Field.MOVING_DATE.label());
    private final Paragraph status = new Paragraph();
    private final PdfViewer viewer = new PdfViewer();

    public FillFormView() {
        addClassName("uc12-view");
        add(new H1("UC12 — Fill in a PDF form"));
        add(new Paragraph("The pension fund only accepts its own PDF form for "
                + "a change of address. Answer the questions and choose "
                + " \"Fill in the form\". The server writes your answers "
                + "into the PDF and shows the filled copy. The viewer's "
                + "download button gives you the filled form."));

        FormLayout form = new FormLayout(name, customerNumber, oldAddress,
                newAddress, movingDate);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        for (AbstractField<?, ?> field : fields()) {
            field.setRequiredIndicatorVisible(true);
        }
        customerNumber.setPlaceholder("e.g. AP-448210");
        Button fill = new Button("Fill in the form", event -> fillIn());
        fill.addThemeVariants(ButtonVariant.PRIMARY);
        status.addClassName("uc12-status");
        Div side = new Div(form, fill, status);
        side.addClassName("uc12-form");

        viewer.addClassName("uc12-viewer");
        viewer.setFileName(FILE_NAME);
        viewer.setSrc(new Pdf(FILE_NAME, AddressChangeForm.blank()).handler());
        status.setText("The viewer shows the empty form.");

        Div layout = new Div(side, viewer);
        layout.addClassNames("uc12-layout", "side-by-side");
        add(layout);
        add(ApiGap.note(12, "the viewer shows a PDF form, but you can not "
                + "type into its fields."));
        add(ApiGap.note(15, "the viewer can not add a signature to a PDF, nor "
                + "show whether an existing signature is valid."));
    }

    private void fillIn() {
        boolean complete = true;
        for (AbstractField<?, ?> field : fields()) {
            boolean empty = field.isEmpty()
                    || field.getValue() instanceof String text
                            && text.isBlank();
            ((HasValidation) field).setErrorMessage("Required");
            ((HasValidation) field).setInvalid(empty);
            complete &= !empty;
        }
        if (!complete) {
            status.setText("Answer every question first.");
            return;
        }
        Map<Field, String> values = new EnumMap<>(Field.class);
        values.put(Field.NAME, name.getValue().trim());
        values.put(Field.CUSTOMER_NUMBER, customerNumber.getValue().trim());
        values.put(Field.OLD_ADDRESS, oldAddress.getValue().trim());
        values.put(Field.NEW_ADDRESS, newAddress.getValue().trim());
        values.put(Field.MOVING_DATE, DATE.format(movingDate.getValue()));
        viewer.setSrc(
                new Pdf(FILE_NAME, AddressChangeForm.fill(values)).handler());
        status.setText("The viewer shows the form with your answers. Change "
                + "an answer and fill in the form again to update it.");
    }

    private AbstractField<?, ?>[] fields() {
        return new AbstractField<?, ?>[] { name, customerNumber, oldAddress,
                newAddress, movingDate };
    }
}
