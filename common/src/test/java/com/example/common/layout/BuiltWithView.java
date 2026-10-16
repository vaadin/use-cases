package com.example.common.layout;

import com.example.common.BuiltWith;
import com.example.common.TestMainLayout;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@Route(value = "built-with", layout = TestMainLayout.class)
public class BuiltWithView extends Div {

    public BuiltWithView() {
        add(new BuiltWith("A service loader file").resource(
                "META-INF/services/com.vaadin.flow.server.VaadinServiceInitListener"));
    }
}
