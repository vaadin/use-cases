package com.example.uc12;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;

/**
 * The change of address form of UC12: a PDF form with fields, as an outside
 * organisation would publish it, and a way to fill it in on the server.
 * <p>
 * {@link #blank()} builds the form with PDFBox, so that nothing binary is
 * committed; a real application would load the organisation's file instead.
 * {@link #fill(Map)} writes the answers into the form fields and flattens them,
 * so the answers become part of the page and can no longer be changed in the
 * file the user downloads and sends.
 */
public final class AddressChangeForm {

    /** The fields of the form, with their names in the PDF. */
    public enum Field {
        NAME("name", "Full name", false),
        CUSTOMER_NUMBER("customerNumber", "Customer number", false),
        OLD_ADDRESS("oldAddress", "Old address", true),
        NEW_ADDRESS("newAddress", "New address", true),
        MOVING_DATE("movingDate", "Moving date", false);

        private final String name;
        private final String label;
        private final boolean multiline;

        Field(String name, String label, boolean multiline) {
            this.name = name;
            this.label = label;
            this.multiline = multiline;
        }

        /** The label printed next to the field, also used in the view. */
        public String label() {
            return label;
        }
    }

    static final String TITLE = "Notification of Change of Address";

    private static final float MARGIN = 56;
    private static final float WIDTH = PDRectangle.A4.getWidth() - 2 * MARGIN;

    private AddressChangeForm() {
    }

    /**
     * Builds the empty form.
     *
     * @return the form as a PDF document with a text field for each
     *         {@link Field}
     */
    public static byte[] blank() {
        try (PDDocument document = new PDDocument()) {
            PDDocumentInformation info = new PDDocumentInformation();
            info.setTitle(TITLE);
            document.setDocumentInformation(info);
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDAcroForm form = new PDAcroForm(document);
            document.getDocumentCatalog().setAcroForm(form);
            PDResources resources = new PDResources();
            resources.put(COSName.HELV, new PDType1Font(FontName.HELVETICA));
            form.setDefaultResources(resources);
            form.setDefaultAppearance("/Helv 11 Tf 0 g");

            try (PDPageContentStream content = new PDPageContentStream(document,
                    page)) {
                float y = PDRectangle.A4.getHeight() - MARGIN;
                y -= 20;
                text(content, "Acme Pension Fund — form AP-12", MARGIN, y,
                        FontName.HELVETICA, 10);
                y -= 28;
                text(content, TITLE, MARGIN, y, FontName.HELVETICA_BOLD, 18);
                y -= 24;
                text(content,
                        "Tell us your new address at least two weeks "
                                + "before you move.",
                        MARGIN, y, FontName.HELVETICA, 11);
                y -= 16;
                for (Field field : Field.values()) {
                    float height = field.multiline ? 48 : 22;
                    y -= 22;
                    text(content, field.label, MARGIN, y, FontName.HELVETICA,
                            10);
                    y -= height + 4;
                    PDRectangle box = new PDRectangle(MARGIN, y, WIDTH, height);
                    content.addRect(box.getLowerLeftX(), box.getLowerLeftY(),
                            box.getWidth(), box.getHeight());
                    content.stroke();
                    addField(form, page, field, box);
                }
                y -= 48;
                content.moveTo(MARGIN, y);
                content.lineTo(MARGIN + WIDTH / 2, y);
                content.stroke();
                text(content, "Signature", MARGIN, y - 14, FontName.HELVETICA,
                        10);
            }
            return save(document);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not build the form", e);
        }
    }

    /**
     * Fills in the form and flattens it.
     *
     * @param values
     *            the answers; fields left out stay empty
     * @return the filled form as a PDF document without form fields
     */
    public static byte[] fill(Map<Field, String> values) {
        try (PDDocument document = Loader.loadPDF(blank())) {
            PDAcroForm form = document.getDocumentCatalog().getAcroForm();
            for (Map.Entry<Field, String> value : values.entrySet()) {
                form.getField(value.getKey().name).setValue(value.getValue());
            }
            form.flatten();
            return save(document);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not fill in the form", e);
        }
    }

    private static void addField(PDAcroForm form, PDPage page, Field field,
            PDRectangle box) throws IOException {
        PDTextField textField = new PDTextField(form);
        textField.setPartialName(field.name);
        textField.setMultiline(field.multiline);
        textField.setDefaultAppearance("/Helv 11 Tf 0 g");
        form.getFields().add(textField);
        PDAnnotationWidget widget = textField.getWidgets().getFirst();
        widget.setRectangle(box);
        widget.setPage(page);
        widget.setPrinted(true);
        page.getAnnotations().add(widget);
    }

    private static void text(PDPageContentStream content, String text, float x,
            float y, FontName font, float size) throws IOException {
        content.beginText();
        content.setFont(new PDType1Font(font), size);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
    }

    private static byte[] save(PDDocument document) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        document.save(out);
        return out.toByteArray();
    }
}
