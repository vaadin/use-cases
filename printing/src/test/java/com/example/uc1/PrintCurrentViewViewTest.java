package com.example.uc1;

import java.util.concurrent.atomic.AtomicInteger;

import com.example.PrintTestSupport;
import com.example.print.PrintEvents;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.shared.Registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = PrintCurrentViewView.class)
class PrintCurrentViewViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersTheDocumentAndTheControls() {
        navigate(PrintCurrentViewView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> "UC1 — Print the current view".equals(h.getText())));
        assertNotNull(findInView(Button.class).id("print-button"));
        Div document = findInView(Div.class).id("order-document");
        assertTrue(document.hasClassName("printable"),
                "The document is the only subtree that should print");
    }

    @Test
    void printButtonOpensThePrintDialog() {
        navigate(PrintCurrentViewView.class);
        assertFalse(PrintTestSupport.printRequested());

        test(findInView(Button.class).id("print-button")).click();

        assertTrue(PrintTestSupport.printRequested(),
                "Clicking Print should call window.print()");
    }

    @Test
    void internalNoteStaysOffThePaperUntilAskedFor() {
        navigate(PrintCurrentViewView.class);

        Div note = findInView(Div.class).id("internal-note");
        assertTrue(note.hasClassName("no-print"));

        test(findInView(Checkbox.class).id("include-note")).click();
        assertFalse(note.hasClassName("no-print"));

        test(findInView(Checkbox.class).id("include-note")).click();
        assertTrue(note.hasClassName("no-print"));
    }

    @Test
    void statusFollowsTheBrowsersPrintEvents() {
        navigate(PrintCurrentViewView.class);
        Span status = findInView(Span.class).id("print-status");
        PrintEvents events = findInView(PrintEvents.class).single();

        events.beforePrint();
        assertTrue(status.getText().contains("Printing"));

        events.afterPrint();
        assertTrue(status.getText().contains("closed 1 time"),
                "Actual: " + status.getText());

        events.beforePrint();
        events.afterPrint();
        assertTrue(status.getText().contains("closed 2 times"),
                "Actual: " + status.getText());
    }

    @Test
    void aRemovedListenerStopsBeingNotified() {
        navigate(PrintCurrentViewView.class);
        PrintEvents events = findInView(PrintEvents.class).single();

        AtomicInteger calls = new AtomicInteger();
        Registration registration = events
                .addAfterPrintListener(calls::incrementAndGet);

        events.afterPrint();
        assertEquals(1, calls.get());

        registration.remove();
        events.afterPrint();
        assertEquals(1, calls.get(),
                "A removed listener must not be notified again");
    }

    @Test
    void leavingTheViewTakesTheWindowListenersWithIt() {
        PrintCurrentViewView view = navigate(PrintCurrentViewView.class);
        PrintEvents events = findInView(PrintEvents.class).single();
        roundTrip();

        view.remove(events);

        assertTrue(PrintTestSupport.pendingJsContains("delete registry"),
                "Detaching must abort the window listeners, or every visit "
                        + "leaks another beforeprint/afterprint pair");
    }
}
