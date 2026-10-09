package com.example.collab;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Shared chrome for the use-case views: the heading, the sampler's description
 * of what the sample shows, and — where a use case has something to say about
 * the API rather than about the demo — a note under it.
 */
public abstract class UseCaseView extends VerticalLayout {

    protected UseCaseView(String heading, String intro) {
        addClassName("use-case");
        add(new H1(heading));
        Paragraph paragraph = new Paragraph(intro);
        paragraph.addClassName("use-case-intro");
        add(paragraph);
    }

    /**
     * Adds a note about the signal API: what this view had to do by hand, or
     * what it could not do at all. The same findings are collected in
     * API-GAPS.md; they are repeated in the running app because that is where a
     * reader meets them.
     */
    protected void addNote(String note) {
        Paragraph paragraph = new Paragraph(note);
        paragraph.addClassName("use-case-note");
        add(paragraph);
    }

    protected void addSection(String title, Component... content) {
        add(new H3(title));
        add(content);
    }
}
