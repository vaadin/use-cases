package com.example.uc6;

import java.util.List;

import com.example.data.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = RedactedExportView.class)
class RedactedExportViewTest extends SpringBrowserlessTest {

    @Test
    void gridShowsTheInternalColumnsTheReportDropsThem() {
        RedactedExportView view = navigate(RedactedExportView.class);

        assertTrue(findInView(H1.class).all().stream()
                .anyMatch(h1 -> "UC6 — Redact and convert on export"
                        .equals(h1.getText())));
        assertEquals("Id", test(view.grid).getHeaderCell(0));
        assertEquals(List.of("Name", "Department", "Card number", "Active"),
                view.report().columnHeaders(),
                "the id column is on screen but not in the report");
    }

    @Test
    void cardNumbersAreMaskedDownToTheLastFourDigits() {
        RedactedExportView view = navigate(RedactedExportView.class);

        Employee employee = test(view.grid).getRow(0);
        String digits = employee.cardNumber().replaceAll("\\D", "");
        String exported = view.report().rows().get(0).get(2);

        assertEquals(
                RedactedExportView.MASK + digits.substring(digits.length() - 4),
                exported);
        assertFalse(view.csv().contains(employee.cardNumber()),
                "the unmasked card number must not reach the file");
        assertTrue(view.csv().contains(exported));
    }

    @Test
    void theStatusCheckboxBecomesYesOrNo() {
        RedactedExportView view = navigate(RedactedExportView.class);

        Employee employee = test(view.grid).getRow(0);
        assertEquals(employee.active() ? "Yes" : "No",
                view.report().rows().get(0).get(3));
    }

    @Test
    void includingInternalColumnsPutsTheIdBack() {
        RedactedExportView view = navigate(RedactedExportView.class);

        view.includeInternal.setValue(true);

        Employee employee = test(view.grid).getRow(0);
        assertEquals(
                List.of("Id", "Name", "Department", "Card number", "Active"),
                view.report().columnHeaders());
        assertEquals(String.valueOf(employee.id()),
                view.report().rows().get(0).get(0));
        // The card number stays masked either way: the converter is per
        // column, not per report.
        assertFalse(view.csv().contains(employee.cardNumber()));
    }
}
