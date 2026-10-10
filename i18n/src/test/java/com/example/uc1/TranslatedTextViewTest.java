package com.example.uc1;

import com.example.SupportedLocales;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.IntegerField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = TranslatedTextView.class)
class TranslatedTextViewTest extends SpringBrowserlessTest {

    @Test
    void rendersEnglishTexts() {
        navigate(TranslatedTextView.class);

        assertEquals("UC1 — Translated texts",
                findInView(H1.class).single().getText());
        assertEquals("Thank you for your order", sampleTitle());
        assertTrue(hasParagraph("Hello Maria, your order #1042 is confirmed."));
        assertTrue(hasParagraph("You ordered 3 items."));
        assertTrue(hasParagraph("Expected delivery: March 5, 2026."));
        assertEquals("Track parcel",
                findInView(Button.class).single().getText());
    }

    @Test
    void localeChangeRewritesTextsInPlace() {
        navigate(TranslatedTextView.class);

        UI.getCurrent().setLocale(SupportedLocales.GERMAN);

        assertEquals("Vielen Dank für Ihre Bestellung", sampleTitle());
        assertTrue(hasParagraph(
                "Hallo Maria, Ihre Bestellung #1042 ist bestätigt."));
        assertTrue(hasParagraph("Sie haben 3 Artikel bestellt."));
        assertTrue(hasParagraph("Voraussichtliche Lieferung: 5. März 2026."));
        assertEquals("Artikel",
                findInView(IntegerField.class).single().getLabel());
    }

    @Test
    void pluralFollowsTheQuantity() {
        navigate(TranslatedTextView.class);
        IntegerField quantity = findInView(IntegerField.class).single();

        test(quantity).setValue(1);
        assertTrue(hasParagraph("You ordered one item."));
        test(quantity).setValue(0);
        assertTrue(hasParagraph("Your basket is empty."));

        UI.getCurrent().setLocale(SupportedLocales.HEBREW);
        test(quantity).setValue(2);
        assertTrue(hasParagraph("הזמנת שני פריטים."));
    }

    @Test
    void missingTranslationsFallBackOrShowTheMarker() {
        navigate(TranslatedTextView.class);

        UI.getCurrent().setLocale(SupportedLocales.FINNISH);

        // Only the English fallback file has this key.
        assertTrue(hasParagraph("You can return any item within 30 days."));
        // No file has this one.
        assertTrue(hasParagraph("!fi: uc1.newsletter"));
    }

    private String sampleTitle() {
        return findInView(H2.class).single().getText();
    }

    private boolean hasParagraph(String text) {
        return findInView(Paragraph.class).all().stream()
                .anyMatch(p -> text.equals(p.getText()));
    }
}
