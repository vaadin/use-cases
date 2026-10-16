package com.example.common.layout;

import com.example.common.TestMainLayout;
import com.example.common.UnreleasedApi;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@UnreleasedApi("https://github.com/vaadin/flow/pull/26495")
@Route(value = "unreleased-api", layout = TestMainLayout.class)
public class UnreleasedApiView extends Div {
}
