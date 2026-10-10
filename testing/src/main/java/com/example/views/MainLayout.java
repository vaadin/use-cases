package com.example.views;

import com.example.common.BaseMainLayout;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@PageTitle("Testing Use Cases")
// The layout must be accessible too, or every view inside it is denied.
@AnonymousAllowed
public class MainLayout extends BaseMainLayout {

    public MainLayout() {
        super("testing", "Testing Use Cases");
    }
}
