package com.example.uc5;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.spreadsheet.Spreadsheet;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = PasteSpreadsheetView.class)
class PasteSpreadsheetViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersHeadingSpreadsheetAndDropZone() {
        navigate(PasteSpreadsheetView.class);

        assertTrue(findInView(H1.class).all().stream()
                .anyMatch(h -> "UC5 — Paste a table from a spreadsheet"
                        .equals(h.getText())));
        assertTrue(findInView(Spreadsheet.class).all().size() >= 1);
        assertTrue(findInView(Div.class).all().stream()
                .anyMatch(d -> d.getClassNames().contains("drop-zone") && "0"
                        .equals(d.getElement().getAttribute("tabindex"))));
    }
}
