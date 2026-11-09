package com.example.uc5;

import java.util.ArrayList;
import java.util.List;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.clipboard.Clipboard;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.spreadsheet.Spreadsheet;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC5 — Paste a table from a spreadsheet.
 * <p>
 * Select cells in the Vaadin Spreadsheet below and copy with Ctrl/Cmd-C, or
 * copy a range from Microsoft Excel or Google Sheets, then paste into the drop
 * zone. The clipboard carries the data as both plain text (tab separated) and
 * HTML (an HTML table). The HTML branch is the only unambiguous one when cell
 * values contain tabs or newlines, so this view parses
 * {@link com.vaadin.flow.component.clipboard.PasteEvent#getHtml()} and hydrates
 * a {@link Grid} from it. When there is no HTML table it falls back to the
 * tab-separated plain text, which is what the Vaadin Spreadsheet copies.
 * <p>
 * {@code Clipboard.onPaste} only fires for the focused element, so the drop
 * zone gets {@code tabindex="0"}.
 */
@Route(value = "uc5", layout = MainLayout.class)
@PageTitle("UC5 — Paste a table")
@UseCaseDescription("Pasting cells from a spreadsheet into a grid")
@Menu(order = 5, title = "UC5 — Paste a table")
public class PasteSpreadsheetView extends VerticalLayout {

    public PasteSpreadsheetView() {
        add(new H1("UC5 — Paste a table"));
        add(new Paragraph(
                "Select cells in the spreadsheet below and copy them with "
                        + "Ctrl+C / Cmd+C. Click the drop zone and paste with "
                        + "Ctrl+V / Cmd+V: the cells appear in a grid. You can "
                        + "also copy a range from Microsoft Excel or Google "
                        + "Sheets. Their copy includes an HTML table, which "
                        + "keeps cells apart even when a cell holds a tab or a "
                        + "line break."));

        Spreadsheet spreadsheet = buildSampleSpreadsheet();
        add(spreadsheet);

        Div dropZone = new Div();
        dropZone.addClassName("paste-drop-zone");
        dropZone.setText("Paste here (Ctrl+V / Cmd+V)");
        dropZone.setWidthFull();
        // paste fires on the focused element, so the non-editable Div needs
        // to be focusable.
        dropZone.getElement().setAttribute("tabindex", "0");

        Grid<List<String>> grid = new Grid<>();
        grid.setVisible(false);

        Clipboard.onPaste(dropZone, event -> {
            List<List<String>> rows = parseRows(event.getHtml(),
                    event.getText());
            if (rows.isEmpty()) {
                dropZone.setText(
                        "No table on the clipboard — copy a range from "
                                + "the spreadsheet, Excel or Google Sheets.");
                return;
            }
            populateGrid(grid, rows);
            dropZone.setText("Pasted " + rows.size()
                    + " rows. The first row is used as the header.");
        });

        add(dropZone, grid);
    }

    private Spreadsheet buildSampleSpreadsheet() {
        Spreadsheet spreadsheet = new Spreadsheet();
        spreadsheet.setHeight("260px");
        spreadsheet.setWidthFull();

        String[] headers = { "Region", "Q1", "Q2", "Q3", "Q4" };
        for (int col = 0; col < headers.length; col++) {
            spreadsheet.createCell(0, col, headers[col]);
        }
        Object[][] data = { { "North", 124000, 138500, 142300, 156800 },
                { "South", 98700, 102400, 119900, 131200 },
                { "East", 87600, 94500, 88700, 101300 },
                { "West", 145900, 152300, 148800, 167400 } };
        for (int r = 0; r < data.length; r++) {
            for (int c = 0; c < data[r].length; c++) {
                spreadsheet.createCell(r + 1, c, data[r][c]);
            }
        }
        return spreadsheet;
    }

    /**
     * The rows of a pasted table: from the HTML table when there is one,
     * otherwise from the tab-separated plain text.
     */
    static List<List<String>> parseRows(@Nullable String html,
            @Nullable String text) {
        // Excel and Google Sheets always include a <table>, which stays
        // unambiguous when cells contain tabs or newlines.
        List<List<String>> rows = html != null ? parseHtmlTable(html)
                : List.of();
        if (rows.isEmpty() && text != null) {
            rows = parseTsv(text);
        }
        return rows;
    }

    private static List<List<String>> parseHtmlTable(String html) {
        Document doc = Jsoup.parse(html);
        Element table = doc.selectFirst("table");
        if (table == null) {
            return List.of();
        }
        List<List<String>> rows = new ArrayList<>();
        for (Element tr : table.select("tr")) {
            List<String> row = new ArrayList<>();
            for (Element cell : tr.select("th, td")) {
                row.add(cell.text());
            }
            if (!row.isEmpty()) {
                rows.add(row);
            }
        }
        return rows;
    }

    private static List<List<String>> parseTsv(String text) {
        List<List<String>> rows = new ArrayList<>();
        for (String line : text.split("\r?\n")) {
            if (line.isEmpty()) {
                continue;
            }
            rows.add(List.of(line.split("\t", -1)));
        }
        return rows;
    }

    private void populateGrid(Grid<List<String>> grid,
            List<List<String>> rows) {
        grid.removeAllColumns();
        List<String> header = rows.getFirst();
        List<List<String>> body = rows.size() > 1 ? rows.subList(1, rows.size())
                : List.of();
        for (int i = 0; i < header.size(); i++) {
            int index = i;
            grid.addColumn(row -> index < row.size() ? row.get(index) : "")
                    .setHeader(header.get(i)).setAutoWidth(true);
        }
        grid.setItems(body);
        grid.setAllRowsVisible(true);
        grid.setVisible(true);
    }
}
