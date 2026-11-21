package com.example.uc4;

import java.util.List;

import com.example.PrintTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.BrowserlessTestConfig;
import com.vaadin.browserless.ExecutedJavaScript;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.select.Select;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@BrowserlessTestConfig(javaScriptHandlers = PrintTestSupport.Browser.class)
@ViewPackages(classes = PrintPreviewView.class)
class PrintPreviewViewTest extends SpringBrowserlessTest {

    @Test
    void previewAndPageRuleStartFromA4Portrait() {
        navigate(PrintPreviewView.class);

        assertEquals("@page { size: A4 portrait; margin: 15mm; }",
                findInView(Pre.class).id("page-rule").getText());
        Div sheet = findInView(Div.class).id("preview-sheet");
        assertEquals("210mm", sheet.getStyle().get("width"));
        assertEquals("297mm", sheet.getStyle().get("min-height"));
        assertEquals("15mm", sheet.getStyle().get("padding"));
    }

    @Test
    void changingPaperOrientationAndMarginsMovesBothTogether() {
        navigate(PrintPreviewView.class);

        test(findInView(Select.class).id("paper-select")).selectItem("Letter");
        test(findInView(Checkbox.class).id("landscape-checkbox")).click();
        test(findInView(Select.class).id("margin-select")).selectItem("25 mm");

        assertEquals("@page { size: Letter landscape; margin: 25mm; }",
                findInView(Pre.class).id("page-rule").getText());
        Div sheet = findInView(Div.class).id("preview-sheet");
        assertEquals("279mm", sheet.getStyle().get("width"),
                "Landscape swaps the edges of the sheet");
        assertEquals("216mm", sheet.getStyle().get("min-height"));
    }

    @Test
    void printingWritesThePageRuleIntoTheDocumentFirst() {
        navigate(PrintPreviewView.class);
        ExecutedJavaScript executed = ExecutedJavaScript.capture();

        test(findInView(Button.class).id("print-button")).click();

        List<String> sent = executed.getAll().stream()
                .map(ExecutedJavaScript.Execution::getExpression).toList();
        int pageRule = indexOf(sent, "style.textContent");
        int print = indexOf(sent, "window.print()");
        assertTrue(pageRule >= 0,
                "@page can only be applied through a style element");
        assertTrue(print > pageRule,
                "The page rule must be in the document before printing");
    }

    @Test
    void thePageRuleIsWrittenOnArrivalAndRemovedOnLeaving() {
        PrintPreviewView view = navigate(PrintPreviewView.class);

        assertTrue(PrintTestSupport.sentJsContains("style.textContent"),
                "The document must carry the rule the preview advertises, "
                        + "not just show it");

        view.getElement().removeFromParent();

        assertTrue(PrintTestSupport.sentJsContains("?.remove()"),
                "@page belongs to the document, so it must not follow the "
                        + "user to the next view");
    }

    private static int indexOf(List<String> expressions, String fragment) {
        for (int i = 0; i < expressions.size(); i++) {
            if (expressions.get(i).contains(fragment)) {
                return i;
            }
        }
        return -1;
    }
}
