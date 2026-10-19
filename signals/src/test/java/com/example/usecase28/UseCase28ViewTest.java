package com.example.usecase28;

import com.example.usecase28.LogEntry.Severity;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.signals.local.ValueSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = UseCase28View.class)
@WithMockUser
class UseCase28ViewTest extends SpringBrowserlessTest {

    @Test
    void seedsThreeInitialEntries() {
        navigate(UseCase28View.class);
        runPendingSignalsTasks();

        UseCase28View view = (UseCase28View) getCurrentView();
        assertEquals(3, view.entries.peek().size(),
                "Three rows should be seeded before the feed starts");
    }

    @Test
    void serverReclassificationFlashesTheRow() throws InterruptedException {
        navigate(UseCase28View.class);
        runPendingSignalsTasks();

        UseCase28View view = (UseCase28View) getCurrentView();
        ValueSignal<LogEntry> first = view.entries.peek().get(0);
        LogEntry entry = first.peek();
        Severity target = (entry.severity() == Severity.ERROR) ? Severity.INFO
                : Severity.ERROR;

        // Change the entry from a non-UI thread, like the server feed does.
        Thread server = new Thread(() -> first.set(entry.withSeverity(target)));
        server.start();
        server.join();
        runPendingSignalsTasks();

        assertEquals(1, view.backgroundFlashes.get(),
                "A server-side change should flash the row");
    }

    @Test
    @SuppressWarnings("unchecked")
    void operatorReclassificationUpdatesSeverity() {
        navigate(UseCase28View.class);
        runPendingSignalsTasks();

        UseCase28View view = (UseCase28View) getCurrentView();
        Select<Severity> firstSeverity = (Select<Severity>) $view(Select.class)
                .all().get(0);

        Severity initial = view.entries.peek().get(0).peek().severity();
        Severity target = (initial == Severity.ERROR) ? Severity.INFO
                : Severity.ERROR;
        test(firstSeverity).selectItem(target.name());
        runPendingSignalsTasks();

        assertEquals(target, view.entries.peek().get(0).peek().severity(),
                "Operator edit should propagate to the underlying entry");
        assertEquals(0, view.backgroundFlashes.get(),
                "The operator's own edit should not flash the row");
    }

    @Test
    void feedKeepsRowsUnderMax() {
        navigate(UseCase28View.class);
        runPendingSignalsTasks();

        UseCase28View view = (UseCase28View) getCurrentView();
        for (int i = 0; i < 40; i++) {
            view.pushServerLine();
        }
        runPendingSignalsTasks();

        assertTrue(view.entries.peek().size() <= 20,
                "Viewer must trim to MAX_ROWS=20 lines, was: "
                        + view.entries.peek().size());
    }
}
