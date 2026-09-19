package com.example.uc5;

import com.example.PrintTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.select.Select;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = HeaderFooterView.class)
class HeaderFooterViewTest extends SpringBrowserlessTest {

    @Test
    void sixtyFourLinesBecomeFourNumberedSheets() {
        navigate(HeaderFooterView.class);

        assertEquals(4, sheetCount());
        for (int number = 1; number <= 4; number++) {
            String sheet = sheetText(number);
            assertTrue(sheet.contains("Page " + number + " of 4"),
                    "Sheet " + number + ": " + sheet);
            assertTrue(sheet.contains("Kettle & Cup Oy"),
                    "Every sheet repeats the letterhead");
        }
        assertTrue(sheetText(1).contains("continued overleaf"));
        assertTrue(sheetText(4).contains("Total"),
                "Only the last sheet closes the document");
    }

    @Test
    void repaginatesWhenMoreLinesFitOnASheet() {
        navigate(HeaderFooterView.class);

        test(findInView(Select.class).id("lines-select")).selectItem("40");

        assertEquals(2, sheetCount());
        assertTrue(sheetText(1).contains("Page 1 of 2"));
        assertTrue(sheetText(2).contains("Total"));
    }

    @Test
    void printButtonOpensThePrintDialog() {
        navigate(HeaderFooterView.class);

        test(findInView(Button.class).id("print-button")).click();

        assertTrue(PrintTestSupport.printRequested());
    }

    private long sheetCount() {
        return findInView(Div.class).withClassName("sheet").all().size();
    }

    private String sheetText(int number) {
        return findInView(Div.class).id("sheet-" + number).getElement()
                .getTextRecursively();
    }
}
