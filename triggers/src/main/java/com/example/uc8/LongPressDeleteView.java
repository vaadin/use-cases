package com.example.uc8;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.trigger.internal.CallbackAction;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC8 — Long-press to delete.
 * <p>
 * Press and hold the red button for 800 ms to delete a row. The custom
 * {@link LongPressTrigger} starts a timer in the browser on {@code pointerdown}
 * and cancels it when the pointer is released or leaves the button, so only a
 * full hold reaches the server, through a {@link CallbackAction}. The button
 * has no click listener, so a short click or an accidental tap does nothing.
 * The click that follows a completed hold is swallowed by the trigger.
 */
@Route(value = "uc8", layout = MainLayout.class)
@PageTitle("UC8 — Long-press to delete")
@UseCaseDescription("Requiring a press-and-hold to confirm a destructive action")
@Menu(order = 8, title = "UC8 — Long-press to delete")
@StyleSheet("uc8.css")
public class LongPressDeleteView extends VerticalLayout {

    public LongPressDeleteView() {
        addClassName("uc8-view");
        add(new H1("UC8 — Long-press to delete"));
        add(new Paragraph("Press and hold the button for 800 ms to delete a "
                + "row. A short click or tap does nothing, and letting go "
                + "early cancels. This confirms a destructive action on a "
                + "touch screen without a confirmation dialog."));

        ValueSignal<Integer> deleted = new ValueSignal<>(0);

        Button deleteButton = new Button("Hold to delete");
        deleteButton.setId("delete");
        deleteButton.addClassName("uc8-delete");
        deleteButton.addThemeVariants(ButtonVariant.ERROR);

        Span status = new Span();
        status.setId("status");
        status.addClassName("uc8-status");
        status.bindText(deleted.map(count -> count + " row"
                + (count == 1 ? "" : "s") + " deleted"));

        new LongPressTrigger(deleteButton, 800).triggers(
                new CallbackAction<>(() -> deleted.update(count -> count + 1)));

        add(new HorizontalLayout(deleteButton, status));
    }
}
