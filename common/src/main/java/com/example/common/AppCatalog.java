package com.example.common;

import java.util.List;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.select.Select;

public final class AppCatalog {

    public record App(String id, String title, String url) {
    }

    public static final List<App> APPS = List.of(
            new App("clipboard", "Clipboard API",
                    "https://clipboard-cases.fly.dev/"),
            new App("collaboration", "Collaboration (Signals)",
                    "https://collaboration-cases.fly.dev/"),
            new App("fullscreen", "Fullscreen API",
                    "https://fullscreen-cases.fly.dev/"),
            new App("geolocation", "Geolocation API",
                    "https://geo-cases.fly.dev/"),
            new App("observability", "Observability",
                    "https://observability-cases.fly.dev/"),
            new App("page-visibility", "Page Visibility API",
                    "https://page-visibility-cases.fly.dev/"),
            new App("route-hierarchy", "Route Hierarchy & Menu",
                    "https://route-hierarchy-cases.fly.dev/"),
            new App("screen-orientation", "Screen Orientation API",
                    "https://screen-orientation-cases.fly.dev/"),
            new App("signals", "Signal API", "https://signals-cases.fly.dev/"),
            new App("text-selection", "Text Selection API",
                    "https://text-selection-cases.fly.dev/"),
            new App("triggers", "Trigger / Action API",
                    "https://triggers-cases.fly.dev/"),
            new App("wake-lock", "Screen Wake Lock API",
                    "https://wake-lock-cases.fly.dev/"),
            new App("web-share", "Web Share API",
                    "https://web-share-cases.fly.dev/"));

    private AppCatalog() {
    }

    public static Div createSelector(String currentAppId) {
        Select<App> select = new Select<>();
        select.setLabel("Application");
        select.setItems(APPS);
        select.setItemLabelGenerator(App::title);
        select.setWidthFull();
        APPS.stream().filter(a -> a.id().equals(currentAppId)).findFirst()
                .ifPresent(select::setValue);
        select.addValueChangeListener(event -> {
            App selected = event.getValue();
            if (selected != null && !selected.id().equals(currentAppId)) {
                UI.getCurrent().getPage().setLocation(selected.url());
            }
        });

        Div wrapper = new Div(select);
        wrapper.getStyle().set("padding",
                "var(--vaadin-padding-s) var(--vaadin-padding-m) var(--vaadin-padding-m)");
        return wrapper;
    }
}
