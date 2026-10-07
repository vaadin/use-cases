package com.example.uc6;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.upload.Upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = ImportCsvView.class)
class ImportCsvViewTest extends SpringBrowserlessTest {

    @Test
    void sampleFile_listsEveryProblemWithItsLine_andImportsOnlyTheGoodRows() {
        navigate(ImportCsvView.class);

        upload("products.csv",
                ImportCsvView.SAMPLE.getBytes(StandardCharsets.UTF_8));

        assertTrue(find(Span.class)
                .withText("products.csv: 4 rows ready, 4 problems.").exists());
        assertEquals(List.of(
                new ImportCsvView.Problem(5,
                        "Stock \"-5\" is not a whole number of 0 or more."),
                new ImportCsvView.Problem(6,
                        "SKU PN-010 appears more than once."),
                new ImportCsvView.Problem(7, "Name is missing."),
                new ImportCsvView.Problem(8,
                        "Price \"twelve\" is not a number of 0 or more.")),
                rows(grid("problems")));

        test(find(Button.class).withText("Import 4 rows").single()).click();

        List<ImportCsvView.Product> catalogue = rows(grid("catalogue"));
        assertEquals(List.of("BK-001", "BK-002", "PN-010", "ST-102"),
                catalogue.stream().map(ImportCsvView.Product::sku).toList());
        assertEquals("Notebook A4, squared", catalogue.get(1).name());
        assertTrue(find(Span.class).withText("Imported 4 products.").exists());
    }

    @Test
    void semicolonSeparatedWindowsFile_withOtherColumnOrder_isUnderstood() {
        navigate(ImportCsvView.class);
        String content = "Name;Stock;SKU;Price\r\n"
                + "\"Crème \"\"fraîche\"\"\nnotebook\";3;CF-1;12,50\r\n";

        upload("export.csv", content.getBytes(Charset.forName("windows-1252")));
        test(find(Button.class).withText("Import 1 row").single()).click();

        assertEquals(List.of(new ImportCsvView.Product("CF-1",
                "Crème \"fraîche\"\nnotebook", new BigDecimal("12.50"), 3)),
                rows(grid("catalogue")));
    }

    @Test
    void fileWithoutTheRequiredColumns_importsNothing() {
        navigate(ImportCsvView.class);

        upload("contacts.csv", "name,email\nAda,ada@example.com\n"
                .getBytes(StandardCharsets.UTF_8));

        assertEquals(List.of(new ImportCsvView.Problem(1,
                "Missing column(s): sku, price, stock. Nothing was imported.")),
                rows(grid("problems")));
        assertFalse(find(Button.class).withTextContaining("Import").exists());
    }

    private void upload(String fileName, byte[] content) {
        test(findInView(Upload.class).single()).upload(fileName, "text/csv",
                content);
    }

    @SuppressWarnings("unchecked")
    private <T> Grid<T> grid(String id) {
        return find(Grid.class).withId(id).single();
    }

    private <T> List<T> rows(Grid<T> grid) {
        return IntStream.range(0, test(grid).size())
                .mapToObj(i -> test(grid).getRow(i)).toList();
    }
}
