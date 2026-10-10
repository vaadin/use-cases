package com.example.uc11;

import com.example.common.UseCaseDescription;
import com.example.orders.OrderStore;
import com.example.views.MainLayout;
import com.example.views.TestsNote;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.shared.Registration;

/**
 * UC11 — Load testing.
 * <p>
 * How many users can the order desk serve at once? Vaadin's load testing plugin
 * answers that without hand-written load scripts: it runs the Playwright
 * checkout test from UC8 once, records the HTTP traffic, turns it into a k6
 * script that handles Vaadin's session, CSRF and UI ids, and runs that script
 * with many virtual users against the packaged application, failing the build
 * when response times exceed the thresholds.
 * <p>
 * This view shows the server's side while a test runs: open sessions and UIs,
 * and the orders the virtual users have placed.
 */
@Route(value = "uc11", layout = MainLayout.class)
@PageTitle("UC11 — Load testing")
@UseCaseDescription("Load testing with k6 scripts recorded from an end-to-end test")
@Menu(order = 11, title = "UC11 — Load testing")
@AnonymousAllowed
public class LoadTestView extends VerticalLayout {

    private final SessionCounter counter;
    private final OrderStore store;
    private final Span stats = new Span();
    private @Nullable Registration polling;

    public LoadTestView(SessionCounter counter, OrderStore store) {
        this.counter = counter;
        this.store = store;
        add(new H1("UC11 — Load testing"));
        add(new Paragraph("Keep this page open while the load test runs: "
                + "the numbers below grow as virtual users go through the "
                + "checkout."));

        stats.addClassName("stats");
        update();
        Div sample = new Div(stats);
        sample.addClassName("sample");
        add(sample,
                new TestsNote("Scenario: uc8/CheckoutPlaywrightIT",
                        "Run: mvn -pl testing -am verify -Pload "
                                + "-Dk6.vus=20 -Dk6.duration=1m",
                        "Scripts: testing/target/k6/tests"));
        addAttachListener(this::start);
        addDetachListener(this::stop);
    }

    private void start(AttachEvent event) {
        event.getUI().setPollInterval(1000);
        polling = event.getUI().addPollListener(e -> update());
    }

    private void stop(DetachEvent event) {
        event.getUI().setPollInterval(-1);
        if (polling != null) {
            polling.remove();
            polling = null;
        }
    }

    void update() {
        stats.setText(("%d open sessions · %d open UIs · %d sessions since "
                + "start · %d orders placed").formatted(counter.sessions(),
                        counter.uis(), counter.totalSessions(),
                        store.all().size()));
    }

    // Package-private test seam.
    String stats() {
        return stats.getText();
    }
}
