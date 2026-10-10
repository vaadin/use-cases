package com.example.uc9;

import com.example.SupportedLocales;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@ViewPackages(classes = TranslationSignalView.class)
class TranslationSignalViewTest extends SpringBrowserlessTest {

    @Test
    void rendersBoundTranslations() {
        TranslationSignalView view = navigate(TranslationSignalView.class);
        runPendingSignalsTasks();

        assertEquals("UC9 — Translations as signals",
                findInView(H1.class).single().getText());
        assertEquals("Your basket", view.titleText());
        assertEquals("One item in the basket", view.itemsText());
        assertEquals("Checkout", view.checkout().getText());
    }

    @Test
    void textsFollowTheLanguage() {
        TranslationSignalView view = navigate(TranslationSignalView.class);

        UI.getCurrent().setLocale(SupportedLocales.FINNISH);
        runPendingSignalsTasks();

        assertEquals("Ostoskorisi", view.titleText());
        assertEquals("Ostoskorissa on yksi tuote", view.itemsText());
        assertEquals("Kassalle", view.checkout().getText());
    }

    @Test
    void textsFollowTheCountParameter() {
        TranslationSignalView view = navigate(TranslationSignalView.class);
        UI.getCurrent().setLocale(SupportedLocales.ARABIC);

        test(view.addItem()).click();
        runPendingSignalsTasks();
        assertEquals("منتجان في السلة", view.itemsText());

        test(view.removeItem()).click();
        test(view.removeItem()).click();
        runPendingSignalsTasks();
        assertEquals("السلة فارغة", view.itemsText());
        assertFalse(view.checkout().isEnabled());
    }
}
