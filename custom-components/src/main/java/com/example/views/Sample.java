package com.example.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;

/** The bordered box that shows a component in use. */
public class Sample extends Div {

    public Sample(String title, Component... content) {
        addClassName("sample");
        add(new H2(title));
        add(content);
    }
}
