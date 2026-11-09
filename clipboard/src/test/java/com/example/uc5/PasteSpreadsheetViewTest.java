package com.example.uc5;

import java.util.List;

import com.example.ClipboardTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = PasteSpreadsheetView.class)
class PasteSpreadsheetViewTest extends SpringBrowserlessTest {

    private static final String SHEET_HTML = """
            <table>
              <tr><th>Region</th><th>Q1</th></tr>
              <tr><td>North</td><td>124000</td></tr>
              <tr><td>South</td><td>98700</td></tr>
            </table>""";

    @Test
    void pastingATableFillsTheGridWithTheFirstRowAsHeader() {
        navigate(PasteSpreadsheetView.class);
        Div dropZone = dropZone();

        ClipboardTestSupport.paste(dropZone, "Region\tQ1\nNorth\t124000",
                SHEET_HTML);

        @SuppressWarnings("unchecked")
        Grid<List<String>> grid = findInView(Grid.class).single();
        assertTrue(grid.isVisible());
        assertEquals("Region", test(grid).getHeaderCell(0));
        assertEquals("Q1", test(grid).getHeaderCell(1));
        assertEquals(2, test(grid).size());
        assertEquals("South", test(grid).getCellText(1, 0));
        assertEquals("Pasted 3 rows. The first row is used as the header.",
                dropZone.getText());
    }

    @Test
    void pastingTextWithoutATableExplainsWhatToCopy() {
        navigate(PasteSpreadsheetView.class);
        Div dropZone = dropZone();

        ClipboardTestSupport.paste(dropZone, null, null);

        assertTrue(dropZone.getText().startsWith("No table on the clipboard"));
        assertTrue(findInView(Grid.class).all().isEmpty(),
                "the grid stays hidden");
    }

    @Test
    void theHtmlTableKeepsCellsWithTabsAndLineBreaksApart() {
        // The tab-separated text of the same copy splits these cells wrongly.
        String html = "<table><tr><td>a\tb</td><td>line 1<br>line 2</td></tr>"
                + "</table>";
        String text = "\"a\tb\"\t\"line 1\nline 2\"";

        assertEquals(List.of(List.of("a b", "line 1 line 2")),
                PasteSpreadsheetView.parseRows(html, text));
    }

    @Test
    void withoutAnHtmlTableTheTabSeparatedTextIsUsed() {
        assertEquals(List.of(List.of("Region", "Q1"), List.of("North", "")),
                PasteSpreadsheetView.parseRows("<p>not a table</p>",
                        "Region\tQ1\r\nNorth\t\n"));
    }

    private Div dropZone() {
        return findInView(Div.class).all().stream()
                .filter(div -> div.hasClassName("paste-drop-zone")).findFirst()
                .orElseThrow();
    }
}
