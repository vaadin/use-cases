package com.example.usecase28;

import java.time.LocalTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.example.usecase23.SchedulerService;
import com.example.usecase28.LogEntry.Severity;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Server log viewer. Lines arrive from a simulated background feed every second
 * and pulse yellow when added. The viewer also lets a triage operator
 * re-classify a line's severity through a dropdown; user-driven severity edits
 * must NOT pulse, otherwise the operator can't distinguish "the server just
 * sent me something new" from "I just changed something".
 * <p>
 * The contextual effect uses
 * {@link com.vaadin.flow.signals.EffectContext#isBackgroundChange()} to flash
 * only on changes that originated outside the operator's request.
 */
@PageTitle("Use Case 28: Server log viewer")
@Route(value = "use-case-28", layout = MainLayout.class)
@Menu(order = 28, title = "UC 28: Server log viewer")
@AnonymousAllowed
@StyleSheet("usecase28.css")
public class UseCase28View extends VerticalLayout {

    private static final int MAX_ROWS = 20;
    private static final String[] SOURCES = { "auth-svc", "payments-svc",
            "scheduler", "ingest-worker", "edge-cache" };
    private static final String[] MESSAGES = { "Request completed in 42 ms",
            "Token refreshed", "Retrying upstream call (attempt 2/3)",
            "Cache miss, falling back to origin",
            "Connection pool saturated, queueing",
            "Payment captured for order #4821", "Worker heartbeat received" };

    final ListSignal<LogEntry> entries = new ListSignal<>();
    private final Random random = new Random();
    private @Nullable String taskId;

    public UseCase28View(SchedulerService schedulerService) {
        setSpacing(true);
        addClassName("usecase28-view");
        setPadding(true);

        add(new H2("Use Case 28: Server log viewer"), new Paragraph(
                "Log lines arrive every second from a simulated server feed."
                        + " Each row pulses when it first appears AND when its"
                        + " severity is re-classified by the server — but"
                        + " never when the operator re-classifies it via the"
                        + " dropdown. A contextual effect uses"
                        + " EffectContext.isBackgroundChange() to tell the two"
                        + " apart."));

        Div header = createHeaderRow();
        Div rows = new Div();
        rows.getStyle().set("display", "flex").set("flex-direction", "column");
        rows.bindChildren(entries, this::createRow);

        add(header, rows);

        // Seed a few initial lines so the operator has something to look at
        // before the first scheduler tick.
        for (int i = 0; i < 3; i++) {
            entries.insertFirst(generateRandomEntry());
        }

        addAttachListener(event -> {
            taskId = "uc28-log-feed-" + event.getUI().getUIId();
            // Mutation happens on the scheduler thread — the contextual
            // effect will see this as a background change.
            schedulerService.scheduleTask(taskId, this::pushServerLine, 1500,
                    1500, TimeUnit.MILLISECONDS);
        });
        addDetachListener(event -> {
            if (taskId != null) {
                schedulerService.cancelTask(taskId);
            }
        });
    }

    /**
     * Simulates a server-driven log update. Either inserts a brand-new line or
     * re-classifies an existing one's severity from the server side.
     */
    void pushServerLine() {
        List<ValueSignal<LogEntry>> current = entries.peek();
        boolean reclassify = !current.isEmpty() && random.nextInt(4) == 0;
        if (reclassify) {
            ValueSignal<LogEntry> victim = current
                    .get(random.nextInt(current.size()));
            LogEntry existing = victim.peek();
            Severity newSeverity = randomSeverity();
            if (newSeverity != existing.severity()) {
                victim.set(existing.withSeverity(newSeverity));
                return;
            }
        }
        entries.insertFirst(generateRandomEntry());
        // Trim to the latest MAX_ROWS lines.
        List<ValueSignal<LogEntry>> after = entries.peek();
        if (after.size() > MAX_ROWS) {
            for (int i = after.size() - 1; i >= MAX_ROWS; i--) {
                entries.remove(after.get(i));
            }
        }
    }

    private LogEntry generateRandomEntry() {
        return new LogEntry(LocalTime.now(),
                SOURCES[random.nextInt(SOURCES.length)],
                MESSAGES[random.nextInt(MESSAGES.length)], randomSeverity());
    }

    private Severity randomSeverity() {
        int pick = random.nextInt(10);
        if (pick < 7) {
            return Severity.INFO;
        } else if (pick < 9) {
            return Severity.WARN;
        }
        return Severity.ERROR;
    }

    private Div createHeaderRow() {
        Div row = new Div();
        row.getStyle().set("display", "grid")
                .set("grid-template-columns", "100px 130px 1fr 110px")
                .set("gap", "var(--lumo-space-s)")
                .set("padding", "var(--lumo-space-s) var(--lumo-space-m)")
                .set("border-bottom", "2px solid var(--lumo-contrast-20pct)")
                .set("font-weight", "bold")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");
        row.add(new Span("Time"), new Span("Source"), new Span("Message"),
                new Span("Severity"));
        return row;
    }

    private Div createRow(ValueSignal<LogEntry> entrySignal) {
        Div row = new Div();
        row.getStyle().set("display", "grid")
                .set("grid-template-columns", "100px 130px 1fr 110px")
                .set("gap", "var(--lumo-space-s)").set("align-items", "center")
                .set("padding", "var(--lumo-space-xs) var(--lumo-space-m)")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

        Span time = new Span();
        time.bindText(
                entrySignal.map(e -> e.timestamp().withNano(0).toString()));
        time.getStyle().set("font-family", "monospace").set("color",
                "var(--lumo-secondary-text-color)");

        Span source = new Span();
        source.bindText(entrySignal.map(LogEntry::source));
        source.getStyle().set("font-family", "monospace");

        Span message = new Span();
        message.bindText(entrySignal.map(LogEntry::message));

        Select<Severity> severitySelect = new Select<>();
        severitySelect.setItems(Severity.values());
        severitySelect.bindValue(entrySignal.map(LogEntry::severity),
                entrySignal.updater(LogEntry::withSeverity));

        // Contextual effect: only flash on changes that originated from the
        // server feed (background), never on the operator's own dropdown edit.
        Signal.effect(row, ctx -> {
            entrySignal.get();
            if (ctx.isBackgroundChange()) {
                row.getElement().flashClass("uc28-flash");
            }
        });

        row.add(time, source, message, severitySelect);
        return row;
    }
}
