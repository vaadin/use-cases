package com.example.uc2;

import java.util.List;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

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
import com.vaadin.flow.component.wakelock.WakeLockAvailability;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC2 — Step-by-step recipe.
 * <p>
 * Hands-busy tasks like cooking, lab protocols, sheet music or assembly
 * instructions need the screen to stay on for as long as the user is looking at
 * the page. The view requests the wake lock when it is attached and releases it
 * when it is detached, both from one {@code whenAttached} handler, so the lock
 * lives exactly as long as the view — no toggle for the user to forget.
 */
@Route(value = "uc2", layout = MainLayout.class)
@PageTitle("UC2 — Step-by-step recipe")
@UseCaseDescription("Keeping the screen on while someone follows step-by-step instructions")
@Menu(order = 2, title = "UC2 — Step-by-step recipe")
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

    public RecipeView() {
        addClassName("uc2-view");
        add(new H1("UC2 — Step-by-step recipe"));
        add(new Paragraph("The wake lock is requested when you open this "
                + "leek tart recipe and released when you navigate away. "
                + "While you have buttery hands the screen does not dim, "
                + "and there is nothing to switch on or off."));

        Span statusBadge = new Span();
        statusBadge.addClassName("status-badge");
        Signal<Boolean> active = WakeLock.activeSignal();
        Signal<WakeLockAvailability> availability = WakeLock
                .availabilitySignal();
        statusBadge
                .bindText(Signal.computed(() -> statusText(availability.get(),
                        Boolean.TRUE.equals(active.get()))));
        statusBadge.bindClassName("active", active);
        add(new HorizontalLayout(new Span("Wake lock:"), statusBadge));

        add(new H2("Steps"));
        Div stepsContainer = new Div();
        stepsContainer.addClassName("uc2-steps");
        for (int i = 0; i < STEPS.size(); i++) {
            int index = i;
            Div step = new Div(new Span((i + 1) + ". " + STEPS.get(i)));
            step.addClassName("uc2-step");
            step.bindClassName("current", currentStep.map(c -> c == index));
            step.bindClassName("done", currentStep.map(c -> c > index));
            stepsContainer.add(step);
        }
        add(stepsContainer);

        Button prevButton = new Button("Previous");
        Button nextButton = new Button("Next step");
        prevButton.addClickListener(
                e -> currentStep.set(Math.max(0, currentStep.peek() - 1)));
        nextButton.addClickListener(e -> currentStep
                .set(Math.min(STEPS.size() - 1, currentStep.peek() + 1)));
        add(new HorizontalLayout(prevButton, nextButton));

        whenAttached(ui -> {
            WakeLock.request(ui);
            return () -> WakeLock.release(ui);
        });
    }

    private static String statusText(WakeLockAvailability availability,
            boolean active) {
        if (active) {
            return "Holding — the screen stays on";
        }
        if (availability == WakeLockAvailability.UNSUPPORTED) {
            return "Not available — needs a supporting browser and HTTPS";
        }
        return "Requested — waiting for the browser";
    }
}
