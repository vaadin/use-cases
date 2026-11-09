package com.example.uc4;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.trigger.internal.DoubleClickTrigger;
import com.vaadin.flow.component.trigger.internal.PropertyInput;
import com.vaadin.flow.component.trigger.internal.WriteToClipboardAction;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC4 — Double-click copy.
 * <p>
 * Double-clicking a coupon code copies it to the clipboard. A
 * {@link DoubleClickTrigger} runs a {@link WriteToClipboardAction} that reads
 * the box's {@code textContent} through a {@link PropertyInput}, so the write
 * happens inside the double click, as the browser requires. The action then
 * reports the copied text back to the server, which shows it below the box.
 * <p>
 * A server-side double-click listener could only write to the clipboard after a
 * round-trip, outside the gesture, and browsers such as Safari and Firefox
 * reject that write.
 */
@Route(value = "uc4", layout = MainLayout.class)
@PageTitle("UC4 — Double-click copy")
@UseCaseDescription("Copying text to the clipboard on double-click")
@Menu(order = 4, title = "UC4 — Double-click copy")
@StyleSheet("uc4.css")
public class DoubleClickCopyView extends VerticalLayout {

    public DoubleClickCopyView() {
        addClassName("uc4-view");
        add(new H1("UC4 — Double-click copy"));
        add(new Paragraph("Double-click the coupon code to copy it, then paste "
                + "it anywhere. The copy happens in the browser, inside the "
                + "double click. The line below the code shows what the "
                + "browser reported as copied."));

        Div code = new Div("SPRING-SALE-25");
        code.setId("target");
        code.addClassName("uc4-copy-target");

        Span status = new Span("(nothing copied yet)");
        status.setId("status");
        status.addClassName("uc4-status");

        new DoubleClickTrigger(code).triggers(new WriteToClipboardAction(
                new PropertyInput<>(code, "textContent", String.class), null,
                copied -> status.setText("Copied: " + copied),
                error -> status.setText("Copy failed: " + error.message())));

        add(code, status);
    }
}
