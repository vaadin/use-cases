package com.example.uc10;

import java.util.List;

import com.example.SupportedLocales;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = TranslatedDataView.class)
class TranslatedDataViewTest extends SpringBrowserlessTest {

    @Autowired
    private CategoryCatalog catalog;

    @BeforeEach
    void resetCatalog() {
        catalog.reset();
    }

    @Test
    void usersSeeNamesInTheirLanguageWithFallbackMarked() {
        TranslatedDataView view = navigate(TranslatedDataView.class);
        runPendingSignalsTasks();

        assertEquals("UC10 — Translated data",
                findInView(H1.class).single().getText());
        assertEquals(List.of("Beverages", "Bakery", "Dairy", "Frozen food",
                "Household", "Pet supplies"), view.previewTexts());

        UI.getCurrent().setLocale(SupportedLocales.FINNISH);
        runPendingSignalsTasks();

        assertEquals(List.of("Juomat", "Leipomo", "Maitotuotteet",
                "Frozen foodkääntämätön", "Kodin tarvikkeet",
                "Pet supplieskääntämätön"), view.previewTexts());
        assertTrue(view.coverage().contains("Suomi 4/6"));
        assertTrue(view.coverage().contains("English 6/6"));
    }

    @Test
    void comboBoxLabelsFollowTheLanguage() {
        navigate(TranslatedDataView.class);
        ComboBox<?> picker = findInView(ComboBox.class).single();

        UI.getCurrent().setLocale(SupportedLocales.HEBREW);
        runPendingSignalsTasks();

        assertEquals("קטגוריה", picker.getLabel());
        assertEquals("משקאות", test(picker).getSuggestions().getFirst());
    }

    @Test
    void savingATranslationShowsItToUsers() {
        TranslatedDataView view = navigate(TranslatedDataView.class);
        UI.getCurrent().setLocale(SupportedLocales.FINNISH);
        runPendingSignalsTasks();

        test(view.list()).selectItem("Pet supplies");
        assertEquals("Pet supplies",
                view.field(SupportedLocales.ENGLISH).getValue());
        assertEquals("", view.field(SupportedLocales.FINNISH).getValue());
        test(view.field(SupportedLocales.FINNISH))
                .setValue("Lemmikkitarvikkeet");
        test(view.saveButton()).click();
        runPendingSignalsTasks();

        assertEquals("Lemmikkitarvikkeet", view.previewTexts().getLast());
        assertTrue(view.coverage().contains("Suomi 5/6"));
    }

    @Test
    void addingRequiresAnEnglishName() {
        TranslatedDataView view = navigate(TranslatedDataView.class);

        test(view.field(SupportedLocales.GERMAN)).setValue("Gemüse");
        test(view.saveButton()).click();
        runPendingSignalsTasks();
        assertTrue(view.field(SupportedLocales.ENGLISH).isInvalid());
        assertEquals(6, view.previewTexts().size());

        test(view.field(SupportedLocales.ENGLISH)).setValue("Vegetables");
        test(view.saveButton()).click();
        runPendingSignalsTasks();
        assertEquals("Vegetables", view.previewTexts().getLast());
        assertEquals("", view.field(SupportedLocales.ENGLISH).getValue());
    }

    @Test
    void anEditInAnotherSessionReachesThisOne() {
        TranslatedDataView view = navigate(TranslatedDataView.class);
        UI.getCurrent().setLocale(SupportedLocales.GERMAN);
        runPendingSignalsTasks();
        test(view.list()).selectItem("Backwaren");
        test(view.field(SupportedLocales.FINNISH)).setValue("Leipä");

        // Another administrator, in another session, translates a category.
        catalog.rename(catalog.categories().peek().get(4),
                LocalizedText.of("en", "Household", "de", "Haushalt"));
        runPendingSignalsTasks();

        assertEquals("Haushalt", view.previewTexts().get(4));
        // What this administrator was typing is kept.
        assertEquals("Leipä", view.field(SupportedLocales.FINNISH).getValue());
    }
}
