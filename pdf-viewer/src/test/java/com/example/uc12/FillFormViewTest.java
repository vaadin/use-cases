package com.example.uc12;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.pdfviewer.PdfViewer;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = FillFormView.class)
class FillFormViewTest extends SpringBrowserlessTest {

    @Test
    void fillingInShowsTheFilledForm() {
        navigate(FillFormView.class);
        String blank = src();

        test(textField("Full name")).setValue("Maria Lind");
        test(textField("Customer number")).setValue("AP-448210");
        test(textArea("Old address")).setValue("Hafenstrasse 12\nHamburg");
        test(textArea("New address")).setValue("Kauppakatu 3\nJyväskylä");
        test(findInView(DatePicker.class).single())
                .setValue(LocalDate.of(2026, 11, 15));
        test(fillButton()).click();

        assertNotEquals(blank, src());
        assertTrue(status().contains("with your answers"), status());
        assertTrue(download().getHref().endsWith("change-of-address-AP-12.pdf"),
                download().getHref());
    }

    @Test
    void missingAnswersAreMarkedAndTheFormIsNotFilled() {
        navigate(FillFormView.class);
        String blank = src();
        test(textField("Full name")).setValue("Maria Lind");
        test(textField("Customer number")).setValue("   ");

        test(fillButton()).click();

        assertEquals(blank, src());
        assertTrue(findInView(Anchor.class).withText("Download the filled form")
                .all().isEmpty(), "Nothing to download yet");
        assertFalse(textField("Full name").isInvalid());
        assertTrue(textField("Customer number").isInvalid(),
                "Only spaces is no answer");
        assertTrue(findInView(DatePicker.class).single().isInvalid());
        assertEquals("Answer every question first.", status());
    }

    @Test
    void lettersTheFormCanNotHoldAreMarkedAndTheFormIsNotFilled() {
        navigate(FillFormView.class);
        String blank = src();
        test(textField("Full name")).setValue("Łukasz Nowak");
        test(textField("Customer number")).setValue("AP-448210");
        test(textArea("Old address")).setValue("Hafenstrasse 12\nHamburg");
        test(textArea("New address")).setValue("Kauppakatu 3\nJyväskylä");
        test(findInView(DatePicker.class).single())
                .setValue(LocalDate.of(2026, 11, 15));

        test(fillButton()).click();

        assertEquals(blank, src());
        assertTrue(textField("Full name").isInvalid());
        assertFalse(textArea("New address").isInvalid(),
                "Western European letters fit");
        assertTrue(status().contains("Western European letters"), status());
    }

    private Anchor download() {
        return findInView(Anchor.class).withText("Download the filled form")
                .single();
    }

    private TextField textField(String label) {
        return findInView(TextField.class).withCaption(label).single();
    }

    private TextArea textArea(String label) {
        return findInView(TextArea.class).withCaption(label).single();
    }

    private Button fillButton() {
        return findInView(Button.class).withText("Fill in the form").single();
    }

    private String src() {
        return findInView(PdfViewer.class).single().getElement()
                .getAttribute("src");
    }

    private String status() {
        return findInView(Paragraph.class).withClassName("uc12-status").single()
                .getText();
    }
}
