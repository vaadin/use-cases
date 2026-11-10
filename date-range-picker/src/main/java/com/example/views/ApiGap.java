package com.example.views;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Paragraph;

/**
 * A note in a view about where the API falls short, linking to the entry in
 * {@code API-GAPS.md}, so the view text and the file say the same thing.
 */
public final class ApiGap {

    private static final String API_GAPS_URL = "https://github.com/vaadin/use-cases/blob/main/date-range-picker/API-GAPS.md";

    private ApiGap() {
    }

    /**
     * Creates the note.
     *
     * @param number
     *            the number of the entry in {@code API-GAPS.md}
     * @param text
     *            what falls short, in one or two sentences
     * @return a paragraph ending in a link to the entry
     */
    public static Paragraph note(int number, String text) {
        Anchor link = new Anchor(API_GAPS_URL, "API-GAPS.md #" + number);
        link.setTarget(AnchorTarget.BLANK);
        Paragraph note = new Paragraph(new Text("API gap: " + text + " See "),
                link, new Text("."));
        note.addClassName("api-gap");
        return note;
    }
}
