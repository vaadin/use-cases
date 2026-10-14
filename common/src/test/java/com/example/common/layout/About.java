package com.example.common.layout;

import com.example.common.TestMainLayout;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

/** A route that is not a use case, as its name does not end with "View". */
@Route(value = "about", layout = TestMainLayout.class)
public class About extends Div {
}
