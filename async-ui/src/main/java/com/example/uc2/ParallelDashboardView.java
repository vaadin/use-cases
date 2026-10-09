package com.example.uc2;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.example.AsyncState;
import com.example.MissingAPI;
import com.example.backend.SimulatedLatency;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC2 — Dashboard whose widgets load in parallel.
 * <p>
 * Each widget asks its own slow backend query when the view opens. The page
 * renders at once with a skeleton in every card, and each card fills in on its
 * own through server push as soon as its answer arrives — the slowest query no
 * longer decides when the user sees anything. A failing widget shows an error
 * with a retry button instead of taking the whole page down.
 * <p>
 * The loading state of each widget is a signal of {@link AsyncState}, filled by
 * {@link MissingAPI#load}: the shim stands in for an asynchronous signal Flow
 * does not have yet.
 */
@Route(value = "uc2", layout = MainLayout.class)
@PageTitle("UC2 — Parallel dashboard")
@UseCaseDescription("Rendering the page at once and filling in each slow widget as it arrives")
@Menu(order = 2, title = "UC2 — Parallel dashboard")
@StyleSheet("uc2.css")
public class ParallelDashboardView extends VerticalLayout {

    private final SimulatedLatency latency;
    private final Checkbox failBacklog = new Checkbox(
            "Make \"Support backlog\" fail");
    private final List<Widget> widgets = new ArrayList<>();

    public ParallelDashboardView(SimulatedLatency latency) {
        this.latency = latency;
        addClassName("uc2-view");

        add(new H1("UC2 — Parallel dashboard"));
        add(new Paragraph("Four widgets, four slow queries: 2 s, 1 s, 1 s "
                + "and 3 s, as each card says. All four start together, so "
                + "the two 1-second cards fill in at the same moment, before "
                + "the first card, and the whole page is complete after 3 s "
                + "instead of the 7 s it would take one query at a time."));

        Div cards = new Div();
        cards.addClassName("widget-cards");
        // Not in screen order, and two the same: the cards visibly do not
        // load one after another.
        widgets.add(new Widget("Revenue today", Duration.ofSeconds(2),
                "€ 18,240", false));
        widgets.add(
                new Widget("Open orders", Duration.ofSeconds(1), "312", false));
        widgets.add(new Widget("Top product", Duration.ofSeconds(1),
                "Espresso beans 1 kg", false));
        widgets.add(new Widget("Support backlog", Duration.ofSeconds(3),
                "27 tickets", true));
        widgets.forEach(cards::add);

        Button reload = new Button("Reload all", event -> loadAll());
        add(new HorizontalLayout(reload, failBacklog), cards);

        loadAll();
    }

    private void loadAll() {
        widgets.forEach(Widget::load);
    }

    // Package-private test seam.
    List<Widget> widgets() {
        return widgets;
    }

    final class Widget extends Card {

        private final String title;
        private final Duration delay;
        private final String answer;
        private final boolean canFail;
        private final ValueSignal<AsyncState<String>> state = new ValueSignal<>(
                AsyncState.loading());
        private @Nullable Registration pending;

        Widget(String title, Duration delay, String answer, boolean canFail) {
            this.title = title;
            this.delay = delay;
            this.answer = answer;
            this.canFail = canFail;
            addClassName("widget");
            addThemeVariants(CardVariant.OUTLINED);
            setTitle(title);
            setSubtitle("Query takes %d s".formatted(delay.toSeconds()));

            Div skeleton = new Div();
            skeleton.addClassName("skeleton");
            skeleton.bindVisible(state.map(AsyncState::isLoading));

            Span value = new Span();
            value.addClassName("widget-value");
            value.bindText(state.map(
                    s -> s instanceof AsyncState.Loaded<String>(String v) ? v
                            : ""));
            value.bindVisible(state.map(AsyncState::isLoaded));

            Span error = new Span();
            error.addClassName("widget-error");
            error.bindText(state.map(
                    s -> s instanceof AsyncState.Failed<String>(Throwable e)
                            ? e.getMessage()
                            : ""));
            Button retry = new Button("Retry", event -> load());
            retry.addThemeVariants(ButtonVariant.SMALL);
            Div failure = new Div(error, retry);
            failure.addClassName("widget-failure");
            failure.bindVisible(state.map(AsyncState::isFailed));

            add(skeleton, value, failure);
        }

        void load() {
            if (pending != null) {
                pending.remove();
            }
            // Read the checkbox here, on the UI thread: the query itself runs
            // on a background thread that must not touch components.
            boolean fail = canFail && failBacklog.getValue();
            pending = MissingAPI.load(this, state,
                    () -> latency.after(delay, () -> {
                        if (fail) {
                            throw new IllegalStateException(
                                    "Ticketing system timed out");
                        }
                        return answer;
                    }));
        }

        String title() {
            return title;
        }

        AsyncState<String> state() {
            return state.peek();
        }
    }
}
