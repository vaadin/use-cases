package com.example.uc8;

import java.util.List;

import com.example.SupportedLocales;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = LocaleAwareSortingView.class)
class LocaleAwareSortingViewTest extends SpringBrowserlessTest {

    @Test
    void englishSortsAccentedLettersWithTheirBaseLetter() {
        LocaleAwareSortingView view = navigate(LocaleAwareSortingView.class);
        runPendingSignalsTasks();

        assertEquals("UC8 — Sorting and searching names",
                findInView(H1.class).single().getText());
        List<String> names = view.names();
        assertEquals(List.of("Aalto", "Åberg", "Ahlström", "Ängström"),
                names.subList(0, 4));
        assertTrue(names.indexOf("van Dijk") < names.indexOf("Zeller"));
    }

    @Test
    void finnishPutsÅÄÖAfterZ() {
        LocaleAwareSortingView view = navigate(LocaleAwareSortingView.class);

        UI.getCurrent().setLocale(SupportedLocales.FINNISH);
        runPendingSignalsTasks();

        List<String> names = view.names();
        int z = names.indexOf("Zeller");
        assertEquals(List.of("Åberg", "Ängström", "Öberg"),
                names.subList(z + 1, z + 4));
        assertEquals("Nimi", test(view.grid()).getHeaderCell(0));
    }

    @Test
    void plainCompareToSortsByCodePoint() {
        LocaleAwareSortingView view = navigate(LocaleAwareSortingView.class);

        test(view.localeAwareBox()).click();
        runPendingSignalsTasks();

        List<String> names = view.names();
        // Lowercase and accented letters come after every plain capital.
        assertEquals("Zeller", names.get(names.indexOf("van Dijk") - 1));
        assertEquals("Łukasik", names.getLast());
    }

    @Test
    void searchIgnoresAccentsAndCase() {
        LocaleAwareSortingView view = navigate(LocaleAwareSortingView.class);

        test(view.searchField()).setValue("angstrom");
        runPendingSignalsTasks();
        assertEquals(List.of("Ängström"), view.names());

        test(view.searchField()).setValue("CELIK");
        runPendingSignalsTasks();
        assertEquals(List.of("Çelik"), view.names());

        // Cities count too: Malmö and Göteborg.
        test(view.searchField()).setValue("o");
        runPendingSignalsTasks();
        assertTrue(view.names().containsAll(List.of("Eriksson", "Öberg")));
    }
}
