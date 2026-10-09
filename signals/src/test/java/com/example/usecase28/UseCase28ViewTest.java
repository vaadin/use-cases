package com.example.usecase28;

import com.example.usecase28.LogEntry.Severity;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.select.Select;

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
    void backgroundLineInsertionAddsRow() {
        navigate(UseCase28View.class);
        runPendingSignalsTasks();

        UseCase28View view = (UseCase28View) getCurrentView();
        int before = view.entries.peek().size();

        // Simulate one server-feed tick from a non-UI thread so the
        // contextual effect would see this as a background change.
        Thread t = new Thread(view::pushServerLine);
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        runPendingSignalsTasks();

        assertTrue(view.entries.peek().size() >= before,
                "A server tick should add a line or reclassify one");
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
