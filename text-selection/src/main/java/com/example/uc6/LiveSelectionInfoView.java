package com.example.uc6;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.SelectionRange;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;

/**
 * UC6 — Live selection info (reactive).
 * <p>
 * As the user selects text in the textarea, a side panel reactively shows the
 * selection range, length, word count, and a clipped preview of the selected
 * substring. All four labels are bound to {@code Signal}s computed from
 * {@code selectionSignal()} — no event listener boilerplate, no manual push.
 */
@Route(value = "uc6", layout = MainLayout.class)
@PageTitle("UC6 — Live selection info")
@UseCaseDescription("Showing live details about the selected text")
@Menu(order = 6, title = "UC6 — Live selection info")
@StyleSheet("uc6.css")
public class LiveSelectionInfoView extends VerticalLayout {

    private static final String SAMPLE = """
            The quick brown fox jumps over the lazy dog. The five boxing
            wizards jump quickly. Pack my box with five dozen liquor jugs.
            Sphinx of black quartz, judge my vow. How vexingly quick daft
            zebras jump!""";

    public LiveSelectionInfoView() {
        addClassName("uc6-view");
        add(new H1("UC6 — Live selection info"));
        add(new Paragraph(
                "Drag to select text in the textarea below. The right panel "
                        + "is bound to the selection signal — it updates "
                        + "reactively without any explicit event handler."));

        TextArea text = new TextArea();
        text.setValue(SAMPLE);
        text.setWidthFull();
        text.setHeight("260px");
        text.addClassName("uc-fixed-textarea");

        Signal<SelectionRange> selection = text.selectionSignal();

        Span rangeLabel = new Span();
        Span lengthLabel = new Span();
        Span wordsLabel = new Span();
        rangeLabel.addClassName("value");
        lengthLabel.addClassName("value");
        wordsLabel.addClassName("value");
        Div preview = new Div();
        preview.addClassName("uc-mono");

        rangeLabel.bindText(selection
                .map(s -> s.isEmpty() ? "—" : s.start() + " – " + s.end()));
        lengthLabel.bindText(selection.map(s -> s.length() + " chars"));
        wordsLabel.bindText(selection.map(s -> wordCount(s) + " words"));
        preview.bindText(selection.map(LiveSelectionInfoView::previewText));

        Div info = new Div();
        info.addClassName("uc-info-grid");
        info.add(label("Range"), rangeLabel);
        info.add(label("Length"), lengthLabel);
        info.add(label("Words"), wordsLabel);

        VerticalLayout panel = new VerticalLayout(new H2("Selection"), info,
                new Span("Preview:"), preview);
        panel.setPadding(false);
        panel.setSpacing(false);
        panel.setWidth("280px");

        HorizontalLayout row = new HorizontalLayout(text, panel);
        row.setWidthFull();
        row.setFlexGrow(1, text);
        add(row);
    }

    private static Span label(String text) {
        Span s = new Span(text);
        s.addClassName("label");
        return s;
    }

    private static int wordCount(SelectionRange sel) {
        if (sel.isEmpty() || sel.content().isBlank()) {
            return 0;
        }
        return sel.content().trim().split("\\s+").length;
    }

    private static String previewText(SelectionRange sel) {
        if (sel.isEmpty()) {
            return "(no selection)";
        }
        String content = sel.content();
        return content.length() > 200 ? content.substring(0, 200) + "…"
                : content;
    }
}
