package com.example.uc8;

import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.trigger.internal.CallbackAction;
import com.vaadin.flow.component.trigger.internal.LiteralInput;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC8 — Long-press to confirm a destructive action.
 * <p>
 * Press and hold the red button for 800ms. The custom {@link LongPressTrigger}
 * fires once the hold threshold elapses, runs a {@link CallbackAction} that
 * pretends to delete a row, and updates the status badge. Releasing early
 * cancels — and a regular short click is also suppressed so accidental taps
 * never delete anything.
 */
@Route(value = "uc8", layout = MainLayout.class)
@PageTitle("UC8 — Long-press to delete")
@Menu(order = 8, title = "UC8 — Long-press to delete")
@StyleSheet("uc8.css")
public class LongPressDeleteView extends VerticalLayout {

    private int deleteCount = 0;

    public LongPressDeleteView() {
        addClassName("uc8-view");
        add(new H1("UC8 — Long-press to delete"));
        add(new Paragraph(
                "Press and hold the Delete button for 800ms to confirm. "
                        + "A regular click does nothing — the trigger suppresses "
                        + "it. Real-world: touch-friendly destructive actions "
                        + "without a confirm dialog."));

        Button deleteButton = new Button("Hold to delete");
        deleteButton.setId("delete");
        deleteButton.addClassName("delete-button");

        Span status = new Span("0 rows deleted");
        status.setId("status");
        status.addClassName("status");

        new LongPressTrigger(deleteButton, 800)
                .triggers(new CallbackAction<>(String.class, payload -> {
                    deleteCount++;
                    status.setText(deleteCount + " row"
                            + (deleteCount == 1 ? "" : "s") + " deleted");
                }, new LiteralInput<>("delete")));

        add(new HorizontalLayout(deleteButton, status));
    }
}
