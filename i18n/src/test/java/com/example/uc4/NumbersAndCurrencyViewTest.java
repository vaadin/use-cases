package com.example.uc4;

import java.math.BigDecimal;
import java.util.Currency;

import com.example.SupportedLocales;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = NumbersAndCurrencyView.class)
class NumbersAndCurrencyViewTest extends SpringBrowserlessTest {

    // CLDR separates groups and the currency with no-break spaces.
    private static final String NBSP = " ";
    private static final String NNBSP = " ";

    @Test
    void rendersOrderInEnglishWithEuros() {
        NumbersAndCurrencyView view = navigate(NumbersAndCurrencyView.class);
        runPendingSignalsTasks();

        assertEquals("UC4 — Numbers and currency",
                findInView(H1.class).single().getText());
        Grid<?> grid = view.grid();
        assertEquals("Unit price", test(grid).getHeaderCell(2));
        assertEquals("€24.90", test(grid).getCellText(0, 2));
        assertEquals("€298.80", test(grid).getCellText(0, 3));
        assertTrue(totalsContain("Subtotal: €564.05"));
        assertTrue(totalsContain("VAT 25.5%: €143.83"));
        assertTrue(totalsContain("Orders this year: 1,234,567 (1M)"));
    }

    @Test
    void localeChangesTheFormatButNotTheCurrency() {
        NumbersAndCurrencyView view = navigate(NumbersAndCurrencyView.class);

        UI.getCurrent().setLocale(SupportedLocales.GERMAN);
        runPendingSignalsTasks();

        assertEquals("Stückpreis", test(view.grid()).getHeaderCell(2));
        assertEquals("24,90" + NBSP + "€", test(view.grid()).getCellText(0, 2));
        assertTrue(totalsContain("Zwischensumme: 564,05" + NBSP + "€"));
        assertTrue(totalsContain("1.234.567"));
        assertEquals(SupportedLocales.GERMAN, view.priceField().getLocale());

        UI.getCurrent().setLocale(SupportedLocales.FINNISH);
        runPendingSignalsTasks();
        assertTrue(totalsContain("Välisumma: 564,05" + NBSP + "€"));
        assertTrue(totalsContain("1" + NBSP + "234" + NBSP + "567"));
    }

    @Test
    void currencyDecidesTheDecimals() {
        NumbersAndCurrencyView view = navigate(NumbersAndCurrencyView.class);

        test(view.currencySelect()).selectItem("JPY");
        runPendingSignalsTasks();

        assertEquals("¥25", test(view.grid()).getCellText(0, 2));
        assertTrue(totalsContain("Total: ¥708"));
    }

    @Test
    void arabicUsesArabicIndicDigits() {
        assertEquals("١٢٫٥٠ US$",
                NumbersAndCurrencyView
                        .money(new BigDecimal("12.50"), SupportedLocales.ARABIC,
                                Currency.getInstance("USD"))
                        .replaceAll("\\p{Cf}", "").replace(NBSP, " "));
    }

    private boolean totalsContain(String text) {
        return findInView(Span.class).all().stream()
                .anyMatch(s -> s.getText().contains(text));
    }
}
