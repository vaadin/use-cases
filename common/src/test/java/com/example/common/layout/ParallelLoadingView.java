package com.example.common.layout;

import com.example.common.TestMainLayout;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "parallel-loading", layout = TestMainLayout.class)
@PageTitle("Use Case 19: Parallel Loading")
public class ParallelLoadingView extends Div {
}
