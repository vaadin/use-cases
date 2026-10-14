package com.example.usecase10;

import java.util.UUID;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.streams.TransferContext;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Use Case 10: Bridging Event-Based APIs to Signals
 *
 * Demonstrates the fundamental pattern of wrapping existing event-based APIs
 * into reactive signals. Three different event sources are bridged:
 *
 * 1. Upload component listeners → ValueSignal&lt;UploadState&gt; 2. Keyboard
 * shortcuts → ValueSignal&lt;String&gt; 3. Dark mode media query events →
 * ValueSignal&lt;Boolean&gt;
 *
 * All three are then composed with Signal.computed() into a unified summary.
 *
 * Key pattern: event → signal.set() — no framework magic needed.
 */
@Route(value = "use-case-10", layout = MainLayout.class)
@PageTitle("Use Case 10: Bridging Events to Signals")
@UseCaseDescription("Turning DOM and browser events into signals")
@Menu(order = 10, title = "UC 10: Events to Signals")
@StyleSheet("usecase10.css")
@AnonymousAllowed
public class UseCase10View extends VerticalLayout {

    // --- Signal fields: one per event source ---

    private final ValueSignal<UploadState> uploadStateSignal = new ValueSignal<>(
            new UploadState.Idle());

    private final ValueSignal<String> lastShortcutSignal = new ValueSignal<>(
            "None pressed yet");

    private final ValueSignal<Boolean> darkModeSignal = new ValueSignal<>(
            false);

    public UseCase10View() {
        addClassName("usecase10-view");
        setSpacing(true);
        setPadding(true);

        H2 title = new H2("Use Case 10: Bridging Events to Signals");

        Paragraph description = new Paragraph(
                "This use case demonstrates how to wrap event-based APIs into reactive signals. "
                        + "Each card below bridges a different event source — Upload listeners, "
                        + "keyboard shortcuts, and OS dark mode preference — "
                        + "into a ValueSignal. All three are then composed into a single computed signal.");

        // --- Card 1: Upload State Signal ---
        Div uploadCard = createCard("Upload State Signal",
                "UploadHandler: whenStart / onProgress / whenComplete");
        buildUploadCard(uploadCard);

        // --- Card 2: Keyboard Shortcut Signal ---
        Div shortcutCard = createCard("Keyboard Shortcut Signal",
                "Shortcuts: addShortcutListener");
        buildShortcutCard(shortcutCard);

        // --- Card 3: Dark Mode Signal ---
        Div darkModeCard = createCard("Dark Mode Preference Signal",
                "matchMedia('prefers-color-scheme: dark'): change event");
        buildDarkModeCard(darkModeCard);

        // --- Cards row ---
        Div cardsRow = new Div(uploadCard, shortcutCard, darkModeCard);
        cardsRow.addClassName("cards-row");

        // --- Composition section ---
        Div compositionSection = buildCompositionSection();

        add(title, description, cardsRow, compositionSection);
    }

    // ---------------------------------------------------------------
    // Card 1: Upload — 4 imperative listeners → 1 reactive signal
    // ---------------------------------------------------------------

    private void buildUploadCard(Div card) {
        // Bridge: UploadHandler callbacks → uploadStateSignal
        Upload upload = new Upload(UploadHandler
                .inMemory((metadata, data) -> uploadStateSignal
                        .set(new UploadState.Succeeded(metadata.fileName(),
                                metadata.contentLength())))
                .whenStart((TransferContext ctx) -> uploadStateSignal
                        .set(new UploadState.InProgress(ctx.fileName(), 0,
                                ctx.contentLength())))
                .onProgress(
                        (TransferContext ctx, Long transferred, Long total) -> {
                            uploadStateSignal.set(new UploadState.InProgress(
                                    ctx.fileName(), transferred, total));
                            try {
                                // Slow down transfer so progress states are
                                // visible
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        })
                .whenComplete((TransferContext ctx, Boolean success) -> {
                    if (!success) {
                        String reason = ctx.exception() != null
                                && ctx.exception().getMessage() != null
                                        ? ctx.exception().getMessage()
                                        : "Unknown error";
                        uploadStateSignal.set(
                                new UploadState.Failed(ctx.fileName(), reason));
                    }
                }));
        upload.setWidth("100%");

        // Status label bound to signal
        Paragraph statusLabel = new Paragraph(
                uploadStateSignal.map(UploadState::label));
        statusLabel.addClassName("upload-status-label");

        // Progress bar bound to signal
        ProgressBar progressBar = new ProgressBar(0, 100, 0);
        progressBar.setWidth("100%");
        progressBar.bindValue(() -> switch (uploadStateSignal.get()) {
        case UploadState.InProgress p -> (double) p.progressPercent();
        case UploadState.Succeeded _ -> 100.0;
        default -> 0.0;
        });
        progressBar.bindVisible(() -> uploadStateSignal
                .get() instanceof UploadState.InProgress);

        card.add(upload, statusLabel, progressBar);
    }

    // ---------------------------------------------------------------
    // Card 2: Keyboard Shortcuts
    // ---------------------------------------------------------------

    private void buildShortcutCard(Div card) {
        Span shortcutLabel = new Span(lastShortcutSignal);
        shortcutLabel.addClassName("shortcut-label");

        Paragraph hint = new Paragraph(
                "Try pressing Ctrl+K, Ctrl+B, or Ctrl+J");
        hint.addClassName("hint-text");

        card.add(shortcutLabel, hint);
    }

    // ---------------------------------------------------------------
    // Card 3: Dark Mode Preference
    // ---------------------------------------------------------------

    private void buildDarkModeCard(Div card) {
        Div indicator = createStatusIndicator(
                () -> darkModeSignal.get() ? "Dark Mode" : "Light Mode",
                darkModeSignal);

        Paragraph hint = new Paragraph(
                "Try toggling OS dark mode or browser theme");
        hint.addClassName("hint-text");

        card.add(indicator, hint);
    }

    // ---------------------------------------------------------------
    // Composition: all 3 signals → 1 computed signal
    // ---------------------------------------------------------------

    private Div buildCompositionSection() {
        Div section = new Div();
        section.addClassName("composition-section");

        H3 heading = new H3("Composed System Summary");
        heading.addClassName("composition-heading");

        Paragraph intro = new Paragraph(
                "All three signals combined into one Signal.computed() — "
                        + "the reactive payoff of the event-to-signal pattern:");

        Signal<String> summarySignal = Signal.computed(() -> {
            // Upload
            String uploadPart = switch (uploadStateSignal.get()) {
            case UploadState.Idle _ -> "Upload: idle";
            case UploadState.InProgress p ->
                "Upload: " + p.progressPercent() + "%";
            case UploadState.Succeeded s ->
                "Upload: done (" + s.fileName() + ")";
            case UploadState.Failed _ -> "Upload: FAILED";
            };

            // Shortcut
            String shortcutPart = "Shortcut: " + lastShortcutSignal.get();

            // Dark mode
            String darkPart = darkModeSignal.get() ? "Theme: dark"
                    : "Theme: light";

            return uploadPart + "  |  " + shortcutPart + "  |  " + darkPart;
        });

        Paragraph summaryText = new Paragraph(summarySignal);
        summaryText.addClassName("composition-summary-text");

        section.add(heading, intro, summaryText);
        return section;
    }

    // ---------------------------------------------------------------
    // Event bridges (set up on attach)
    // ---------------------------------------------------------------

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        // Bridge: keyboard shortcuts → lastShortcutSignal
        Shortcuts.addShortcutListener(this,
                () -> lastShortcutSignal.set("Ctrl+K"), Key.KEY_K,
                KeyModifier.CONTROL);
        Shortcuts.addShortcutListener(this,
                () -> lastShortcutSignal.set("Ctrl+B"), Key.KEY_B,
                KeyModifier.CONTROL);
        Shortcuts.addShortcutListener(this,
                () -> lastShortcutSignal.set("Ctrl+J"), Key.KEY_J,
                KeyModifier.CONTROL);

        String unregisterKey = UUID.randomUUID().toString();

        // Bridge: prefers-color-scheme: dark → darkModeSignal
        getElement().executeJs(
                """
                            const mq = window.matchMedia('(prefers-color-scheme: dark)');
                            const update = (e) => this.$server.onDarkModeChange(mq.matches);
                            mq.addEventListener('change', update);
                            window[$0] = () => mq.removeEventListener('change', update);
                            update();
                        """,
                unregisterKey);

        addDetachListener(detach -> {
            detach.unregisterListener();
            detach.getUI().getPage().executeJs("""
                        if (window[$0]) {
                          window[$0]();
                          delete window[$0];
                        }
                    """, unregisterKey);
        });
    }

    @ClientCallable
    public void onDarkModeChange(boolean dark) {
        darkModeSignal.set(dark);
    }

    // ---------------------------------------------------------------
    // UI helpers
    // ---------------------------------------------------------------

    private Div createCard(String title, String subtitle) {
        Div card = new Div();
        card.addClassName("card");

        H3 cardTitle = new H3(title);
        cardTitle.addClassName("card-title");

        Paragraph cardSubtitle = new Paragraph(subtitle);
        cardSubtitle.addClassName("card-subtitle");

        card.add(cardTitle, cardSubtitle);
        return card;
    }

    private Div createStatusIndicator(Signal<String> labelSignal,
            Signal<Boolean> activeSignal) {
        Div row = new Div();
        row.addClassName("status-indicator");

        Span dot = new Span();
        dot.addClassName("status-indicator-dot");
        dot.getClassNames().bind("is-active", activeSignal);

        Span label = new Span(labelSignal);
        label.addClassName("status-indicator-label");

        row.add(dot, label);
        return row;
    }
}
