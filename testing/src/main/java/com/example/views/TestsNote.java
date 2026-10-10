package com.example.views;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

/**
 * Names the tests that cover a use case and how to run them, since in this
 * module the tests are what the use case is about.
 */
public class TestsNote extends Div {

    public TestsNote(String... lines) {
        addClassName("tests");
        for (String line : lines) {
            add(new Div(new Span(line)));
        }
    }
}
