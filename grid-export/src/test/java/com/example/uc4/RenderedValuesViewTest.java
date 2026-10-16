package com.example.uc4;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import com.example.data.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = RenderedValuesView.class)
class RenderedValuesViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersEveryRendererKind() {
        RenderedValuesView view = navigate(RenderedValuesView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h1 -> "UC4 — Rendered columns as text".equals(h1.getText())));
        assertEquals(RenderedValuesView.ROW_COUNT, test(view.grid).size());
        assertEquals(
                List.of("Name", "Status", "Active", "Salary", "Hired",
                        "Country", "Contact", "Actions"),
                view.report().columnHeaders());
    }

    @Test
    void everyRendererKindExportsItsPlainTextEquivalent() {
        RenderedValuesView view = navigate(RenderedValuesView.class);

        Employee employee = test(view.grid).getRow(0);
        List<String> exported = view.report().rows().get(0);

        assertEquals(employee.name(), exported.get(0),
                "plain ValueProvider column");
        assertEquals(employee.active() ? "Active" : "On leave", exported.get(1),
                "ComponentRenderer returning a badge");
        assertEquals(employee.active() ? "Yes" : "No", exported.get(2),
                "ComponentRenderer returning a Checkbox");
        assertEquals(
                String.format(Locale.US, RenderedValuesView.SALARY_FORMAT,
                        employee.salary()),
                exported.get(3), "NumberRenderer keeps its formatting");
        assertEquals(
                employee.hired()
                        .format(DateTimeFormatter
                                .ofPattern(RenderedValuesView.DATE_PATTERN)),
                exported.get(4), "LocalDateRenderer keeps its pattern");
        assertEquals(employee.country(), exported.get(5),
                "single-property LitRenderer");
        assertEquals(employee.name() + " · " + employee.country(),
                exported.get(6), "two-property LitRenderer, via an extractor");
        assertEquals("Edit", exported.get(7),
                "ComponentRenderer returning a Button with an icon");
    }

    @Test
    void formattedNumbersAreQuotedInTheCsv() {
        RenderedValuesView view = navigate(RenderedValuesView.class);

        Employee employee = test(view.grid).getRow(0);
        String salary = String.format(Locale.US,
                RenderedValuesView.SALARY_FORMAT, employee.salary());
        assertTrue(salary.contains(","),
                "the demo salary format uses a thousands separator");
        assertTrue(view.csv().contains('"' + salary + '"'),
                "a cell containing a comma has to be quoted");
    }

    @Test
    void aTwoPropertyLitRendererColumnNeedsAnExplicitExtractor() {
        RenderedValuesView view = navigate(RenderedValuesView.class);

        test(view.withoutExtractor).click();

        String message = view.problem.getText();
        assertTrue(message.contains("Contact"),
                "the message should name the column that cannot be read: "
                        + message);
        assertTrue(message.contains("withColumnExtractor"),
                "the message should say what to do about it: " + message);
    }
}
