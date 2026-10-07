package com.example.uc20;

import java.util.List;
import java.util.Map;

import com.example.views.MainLayout;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.trigger.internal.DoubleClickTrigger;
import com.vaadin.flow.component.trigger.internal.OpenInNewTabAction;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC20 — Double-click a row to open it in a new tab.
 * <p>
 * Each row in the list below is wired to a {@link DoubleClickTrigger} paired
 * with an {@link OpenInNewTabAction} carrying that row's URL. The gesture
 * context (the double-click) is preserved through to {@code window.open}, so
 * the popup blocker permits the new tab — the call would be rejected if it ran
 * on a server-driven push.
 * <p>
 * The high-level {@code Anchor} component handles {@code target="_blank"} on a
 * click, but it can't easily attach to a {@code dblclick} (or to any non-click
 * gesture). The trigger API decouples "which gesture" from "what to do".
 */
@Route(value = "uc20", layout = MainLayout.class)
@PageTitle("UC20 — Double-click → new tab")
@Menu(order = 20, title = "UC20 — Double-click → new tab")
@StyleSheet("uc20.css")
public class DoubleClickOpenView extends VerticalLayout {

    private static final List<Map.Entry<String, String>> LINKS = List.of(
            Map.entry("Vaadin Flow",
                    "https://vaadin.com/docs/latest/flow/overview"),
            Map.entry("Vaadin Components",
                    "https://vaadin.com/docs/latest/components"),
            Map.entry("Java Platform",
                    "https://docs.oracle.com/en/java/javase/25/"),
            Map.entry("MDN Web Docs", "https://developer.mozilla.org"),
            Map.entry("HTTP Cats", "https://http.cat"));

    public DoubleClickOpenView() {
        addClassName("uc20-view");
        add(new H1("UC20 — Double-click → new tab"));
        add(new Paragraph(
                "Double-click any row to open its URL in a new tab. The "
                        + "DoubleClickTrigger keeps the gesture context, so the "
                        + "browser's popup blocker permits window.open. A "
                        + "regular Anchor with target=\"_blank\" would do the "
                        + "same on click; the trigger API lets us hook the "
                        + "same effect to a non-click gesture."));

        Div list = new Div();
        list.setId("list");
        list.addClassName("link-list");
        for (Map.Entry<String, String> link : LINKS) {
            Div row = new Div(link.getKey());
            row.addClassName("row");
            row.getElement().setAttribute("data-url", link.getValue());
            new DoubleClickTrigger(row)
                    .triggers(new OpenInNewTabAction(link.getValue()));
            list.add(row);
        }
        add(list);
    }
}
