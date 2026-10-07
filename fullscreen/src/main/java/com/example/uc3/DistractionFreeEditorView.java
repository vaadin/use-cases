package com.example.uc3;

import com.example.views.MainLayout;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.fullscreen.Fullscreen;
import com.vaadin.flow.component.fullscreen.FullscreenState;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;

/**
 * UC3 — Distraction-free editor.
 * <p>
 * A pane that wraps the text area together with a Done button and live word
 * counter is enlarged via
 * {@link Fullscreen#onClick(com.vaadin.flow.component.Component)
 * Fullscreen.onClick(expand).enter(editorPane)}. The rest of the view (heading,
 * intro, navigation drawer) is hidden by the wrapper, so the writer sees only
 * the editor, the word counter, and the Done button. Exit with Escape or by
 * clicking Done — which calls {@link Fullscreen#exit()}; exiting needs no user
 * gesture, so it is a plain click listener.
 */
@Route(value = "uc3", layout = MainLayout.class)
@Menu(order = 3, title = "UC3 — Distraction-free editor")
@StyleSheet("uc3.css")
public class DistractionFreeEditorView extends VerticalLayout {

    private final TextArea editor = new TextArea();
    private final Span wordCount = new Span("0 words");
    private final Span stateBadge = new Span();
    private final Div editorPane = new Div();
    private final Button done = new Button("Done", e -> Fullscreen.exit());

    public DistractionFreeEditorView() {
        addClassName("uc3-view");
        add(new H1("UC3 — Distraction-free editor"));
        add(new Paragraph(
                "Click “Expand to fullscreen” to focus only on the text. The "
                        + "rest of the app — navigation, header, this "
                        + "paragraph — is hidden by the fullscreen wrapper. "
                        + "A Done button appears inside the pane only while "
                        + "fullscreen so it can be clicked to exit; Escape "
                        + "exits too."));

        stateBadge.addClassName("status-badge");
        add(stateBadge);

        editor.addClassName("editor-area");
        editor.setLabel("Write something");
        editor.setPlaceholder("Start typing…");
        editor.setValueChangeMode(ValueChangeMode.LAZY);
        editor.addValueChangeListener(e -> updateWordCount(e.getValue()));

        Button expand = new Button("Expand to fullscreen");
        // Fullscreen needs the click's user gesture, so bind the request to the
        // Expand button's click trigger.
        Fullscreen.onClick(expand).enter(editorPane);
        wordCount.addClassName("editor-stats");

        HorizontalLayout paneFooter = new HorizontalLayout(done, wordCount);
        paneFooter.addClassName("editor-pane-footer");
        paneFooter.setAlignItems(Alignment.CENTER);
        paneFooter.setWidthFull();

        editorPane.addClassName("editor-pane");
        editorPane.add(editor, paneFooter);

        HorizontalLayout toolbar = new HorizontalLayout(expand);
        toolbar.addClassName("editor-toolbar");
        toolbar.setAlignItems(Alignment.CENTER);
        add(toolbar);

        add(editorPane);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        Signal<FullscreenState> fs = Fullscreen.stateSignal();

        Signal<Boolean> fullscreen = fs
                .map(s -> s == FullscreenState.FULLSCREEN);
        stateBadge.bindText(fs.map(DistractionFreeEditorView::badgeText));
        stateBadge.bindClassName("unsupported",
                fs.map(s -> s == FullscreenState.UNSUPPORTED));
        editorPane.bindClassName("fullscreen", fullscreen);
        editor.bindClassName("fullscreen", fullscreen);
        // Done only makes sense while fullscreen — it exits the pane.
        done.bindVisible(fullscreen);
    }

    private void updateWordCount(String value) {
        long words = value == null || value.isBlank() ? 0
                : value.trim().split("\\s+").length;
        wordCount.setText(words + (words == 1 ? " word" : " words"));
    }

    private static String badgeText(FullscreenState state) {
        // FULLSCREEN: the badge sits outside the fullscreened pane, so it's
        // invisible while fullscreen — keep the idle text instead of
        // flipping to a message no one sees.
        return switch (state) {
        case FULLSCREEN, NOT_FULLSCREEN ->
            "Click Expand to write distraction-free";
        case UNSUPPORTED -> "Fullscreen is not supported in this browser";
        case UNKNOWN -> "Detecting fullscreen support…";
        };
    }
}
