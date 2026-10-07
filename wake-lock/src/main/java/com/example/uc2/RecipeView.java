package com.example.uc2;

import java.util.List;

import com.example.views.MainLayout;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.wakelock.WakeLock;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC2 — Step-by-step recipe.
 * <p>
 * Hands-busy scenarios like cooking, lab protocols, sheet music, or assembly
 * instructions need the screen to stay on for as long as the user is looking at
 * the page. The view requests the wake lock in {@code onAttach} and releases it
 * in {@code onDetach}, so the lock lifetime matches the view lifetime — no
 * toggle for the user to forget.
 */
@Route(value = "uc2", layout = MainLayout.class)
@Menu(order = 2, title = "UC2 — Recipe")
@StyleSheet("uc2.css")
public class RecipeView extends VerticalLayout {

    private static final List<String> STEPS = List.of(
            "Preheat the oven to 200°C.",
            "Whisk 3 eggs with 200 ml of milk and a pinch of salt.",
            "Slice the leeks lengthwise, then chop into 1 cm pieces.",
            "Sweat the leeks in butter on medium heat for 6 minutes.",
            "Spread the leeks in a buttered tart shell, pour over the eggs.",
            "Bake for 25 minutes. Rest 5 minutes before slicing.");

    private final ValueSignal<Integer> currentStep = new ValueSignal<>(0);
    private final Span statusBadge = new Span();
    private final Div stepsContainer = new Div();
    private final Button nextButton = new Button("Next step");
    private final Button prevButton = new Button("Previous");

    public RecipeView() {
        addClassName("uc2-view");
        add(new H1("UC2 — Leek tart, step by step"));
        add(new Paragraph("The wake lock is requested when this page is "
                + "opened and released when you navigate away. While you "
                + "have buttery hands the screen will not dim, but you "
                + "do not have to remember to toggle anything."));

        statusBadge.addClassName("status-badge");
        add(new HorizontalLayout(new Span("Wake lock:"), statusBadge));

        add(new H2("Steps"));
        stepsContainer.addClassName("recipe-steps");
        for (int i = 0; i < STEPS.size(); i++) {
            int index = i;
            Div step = new Div(new Span((i + 1) + ". " + STEPS.get(i)));
            step.addClassName("recipe-step");
            step.bindClassName("current", currentStep.map(c -> c == index));
            step.bindClassName("done", currentStep.map(c -> c > index));
            stepsContainer.add(step);
        }
        add(stepsContainer);

        prevButton.addClickListener(
                e -> currentStep.set(Math.max(0, currentStep.peek() - 1)));
        nextButton.addClickListener(e -> currentStep
                .set(Math.min(STEPS.size() - 1, currentStep.peek() + 1)));
        add(new HorizontalLayout(prevButton, nextButton));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        Signal<Boolean> active = WakeLock.activeSignal();

        statusBadge.bindText(active.map(held -> Boolean.TRUE.equals(held)
                ? "Holding — screen will stay on"
                : "Released — waiting for browser"));
        statusBadge.bindClassName("active", active);

        WakeLock.request();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        WakeLock.release();
        super.onDetach(detachEvent);
    }
}
