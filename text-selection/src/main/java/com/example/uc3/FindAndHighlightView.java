package com.example.uc3;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC3 — Find and highlight.
 * <p>
 * "Find next" walks the user through the matches in a text area, one at a time,
 * with a server-side {@code setSelectionRange(start, end)}. The call also
 * focuses the text area, because browsers paint the selection of a field that
 * has no focus faintly or not at all. Past the last match the search wraps to
 * the start.
 */
@Route(value = "uc3", layout = MainLayout.class)
@PageTitle("UC3 — Find and highlight")
@UseCaseDescription("Stepping through search matches in a text area")
@Menu(order = 3, title = "UC3 — Find and highlight")
public class FindAndHighlightView extends VerticalLayout {

    private static final String SAMPLE = """
            Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do
            eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut
            enim ad minim veniam, quis nostrud exercitation ullamco laboris
            nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in
            reprehenderit in voluptate velit esse cillum dolore eu fugiat
            nulla pariatur. Excepteur sint occaecat cupidatat non proident,
            sunt in culpa qui officia deserunt mollit anim id est laborum.""";

    public FindAndHighlightView() {
        add(new H1("UC3 — Find and highlight"));
        add(new Paragraph(
                "Type a search term and press \"Find next\" repeatedly. The "
                        + "next occurrence is selected in place. Past the "
                        + "last match the search wraps to the start."));

        TextField search = new TextField();
        search.setPlaceholder("Search term, e.g. \"dolor\"");
        search.setWidth("280px");
        search.setMaxWidth("100%");

        TextArea content = new TextArea();
        content.setValue(SAMPLE);
        content.setWidthFull();
        content.setHeight("260px");
        content.addClassName("uc-textarea");

        Span status = new Span();
        status.addClassName("uc-status");

        // Cursor position after the last match. Starting at -1 means the
        // first "Find next" begins from the start of the document.
        int[] cursor = { -1 };

        Button findNext = new Button("Find next", e -> {
            String needle = search.getValue();
            if (needle == null || needle.isEmpty()) {
                status.setText("Enter a search term first");
                return;
            }
            String haystack = content.getValue();
            int from = cursor[0] + 1;
            int idx = haystack.indexOf(needle, from);
            if (idx < 0) {
                idx = haystack.indexOf(needle);
                if (idx < 0) {
                    status.setText("No matches for \"" + needle + "\"");
                    cursor[0] = -1;
                    return;
                }
                status.setText("Wrapped to start — match at " + idx);
            } else {
                status.setText("Match at " + idx);
            }
            cursor[0] = idx;
            content.setSelectionRange(idx, idx + needle.length());
        });

        search.addValueChangeListener(e -> cursor[0] = -1);

        HorizontalLayout searchRow = new HorizontalLayout(search, findNext,
                status);
        searchRow.setAlignItems(Alignment.CENTER);
        searchRow.addClassName("uc-toolbar");

        add(searchRow, content);
    }
}
