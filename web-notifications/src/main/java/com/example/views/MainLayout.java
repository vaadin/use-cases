package com.example.views;

import com.example.common.BaseMainLayout;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.router.PageTitle;

@PageTitle("Web Notifications API Use Cases")
// The browser half of MissingAPI, which every view uses.
@JsModule("./web-notifications.ts")
public class MainLayout extends BaseMainLayout {

    public MainLayout() {
        super("web-notifications", "Web Notifications API Use Cases");
    }
}
