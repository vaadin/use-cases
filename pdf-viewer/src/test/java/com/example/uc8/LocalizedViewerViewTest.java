package com.example.uc8;

import java.util.Locale;

import com.example.pdfviewer.PdfViewer;
import com.example.pdfviewer.PdfViewerI18n;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.JsonNode;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WebBrowser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ViewPackages(classes = LocalizedViewerView.class)
class LocalizedViewerViewTest extends SpringBrowserlessTest {

    @Test
    void theViewerStartsInTheBrowsersLanguage() {
        browserLocale(Locale.GERMANY);
        navigate(LocalizedViewerView.class);

        assertEquals("Vergrößern", i18n().get("zoomIn").asString());
    }

    @Test
    void switchingToEnglishGoesBackToTheDefaults() {
        browserLocale(Locale.of("fi", "FI"));
        navigate(LocalizedViewerView.class);
        assertEquals("Lähennä", i18n().get("zoomIn").asString());

        @SuppressWarnings("unchecked")
        Select<Locale> language = findInView(Select.class).single();
        test(language).selectItem("English");

        assertTrue(i18n().isEmpty(), i18n().toString());
    }

    @Test
    void everyTextIsTranslated() {
        // Texts left out would show up in English in the middle of the
        // translated ones.
        int texts = PdfViewerI18n.class.getDeclaredFields().length;
        assertEquals(texts,
                JacksonUtils.beanToJson(ViewerTexts.of(Locale.of("fi"))).size(),
                "Finnish");
        assertEquals(texts,
                JacksonUtils.beanToJson(ViewerTexts.of(Locale.GERMAN)).size(),
                "German");
    }

    private JsonNode i18n() {
        return (JsonNode) findInView(PdfViewer.class).single().getElement()
                .getPropertyRaw("i18n");
    }

    /** Makes the browser report the given locale, as its request would. */
    private static void browserLocale(Locale locale) {
        WebBrowser browser = mock(WebBrowser.class);
        when(browser.getLocale()).thenReturn(locale);
        VaadinSession.getCurrent().setBrowser(browser);
    }
}
