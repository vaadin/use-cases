package com.example.uc7;

import com.example.home.HomeView;
import com.example.uc7.MonitoringStackView.Row;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The export hop is fully testable here: a {@link PrometheusMeterRegistry} bean
 * exists in the test context, so the exposition text is real. The scrape and
 * query hops need Prometheus, which is not running in a test — so what these
 * tests pin down is that the view degrades honestly instead of failing, which
 * is also how it behaves in the hosted demo.
 */
// The API address is pinned to a port nothing listens on, so the tests see "no
// Prometheus" even on a developer machine where the compose stack is running.
@SpringBootTest(properties = "uc7.prometheus.api-url=http://127.0.0.1:1")
@ViewPackages(classes = { MonitoringStackView.class, HomeView.class })
class MonitoringStackViewTest extends SpringBrowserlessTest {

    @Autowired
    PrometheusMeterRegistry registry;

    @Test
    void rendersHeadingActionsAndReadout() {
        navigate(MonitoringStackView.class);
        runPendingSignalsTasks();

        assertTrue(
                findInView(H1.class).all().stream()
                        .anyMatch(h -> h.getText().startsWith("UC7")),
                "UC7 heading should render");
        for (String label : new String[] { "Refresh", "Generate traffic" }) {
            assertEquals(1,
                    findInView(Button.class).withText(label).all().size(),
                    "action button should render: " + label);
        }
        assertTrue(test(findInView(Grid.class).single()).size() >= 3,
                "readout should list the export, scrape and query rows");
    }

    @Test
    void reportsTheExportedSeriesFromTheRegistry() {
        // The app exports the kit's meters in Prometheus format; that hop needs
        // no external stack, so it must report a real count.
        registry.timer("vaadin.request.duration", "outcome", "success")
                .record(java.time.Duration.ofMillis(12));

        navigate(MonitoringStackView.class);
        runPendingSignalsTasks();

        Grid<Row> grid = findInView(Grid.class).single();
        Row exported = grid.getListDataView().getItems()
                .filter(r -> r.signal().startsWith("Exported series"))
                .findFirst().orElse(null);
        assertTrue(exported != null, "exported-series row should be present");
        assertTrue(Integer.parseInt(exported.value()) > 0,
                "the app should export at least one vaadin_* series, got: "
                        + exported.value());
    }

    @Test
    void degradesWhenPrometheusIsNotRunning() {
        navigate(MonitoringStackView.class);
        runPendingSignalsTasks();

        // No Prometheus in a test run: the badge and the scrape row must say so
        // rather than the view failing.
        Span summary = findInView(Span.class).first();
        assertTrue(summary.getText().contains("vaadin_* series"),
                "summary should report the export count: " + summary.getText());
        assertTrue(summary.getText().endsWith("Prometheus not reachable"),
                "the badge should carry a short verdict only, not the detail: "
                        + summary.getText());

        Grid<Row> grid = findInView(Grid.class).single();
        Row scrape = grid.getListDataView().getItems()
                .filter(r -> "Prometheus scrape target".equals(r.signal()))
                .findFirst().orElse(null);
        assertTrue(scrape != null, "scrape row should be present");
        assertTrue(scrape.value().contains("not reachable"),
                "with no stack running the scrape row should say so, got: "
                        + scrape.value());
    }

    @Test
    void generatingTrafficAddsSamplesAndRefreshesTheReadout() {
        navigate(MonitoringStackView.class);
        runPendingSignalsTasks();

        long before = requestTimerCount();
        test(findInView(Button.class).withText("Generate traffic").single())
                .click();
        runPendingSignalsTasks();

        assertTrue(requestTimerCount() > before,
                "generating traffic should record request samples");
        Grid<Row> grid = findInView(Grid.class).single();
        assertTrue(
                grid.getListDataView().getItems().anyMatch(
                        r -> r.signal().startsWith("Histogram buckets")),
                "the readout should keep reporting bucket availability");
    }

    @Test
    void publishesHistogramBucketsSoPercentilesCanBeCharted() {
        // Percentiles are computed by Prometheus from buckets; without them a
        // p95 panel is silently empty, so the config is worth pinning down.
        registry.timer("vaadin.request.duration", "outcome", "success")
                .record(java.time.Duration.ofMillis(20));

        assertTrue(
                registry.scrape()
                        .contains("vaadin_request_duration_seconds_bucket"),
                "vaadin.request.duration should publish histogram buckets");
    }

    @Test
    void publishesSloBoundariesAroundTheLatenciesThatMatter() {
        // The default histogram spread puts most buckets in ranges a UI
        // interaction never reaches, so a p95 gets read off one very wide
        // bucket. These boundaries are what give the percentile panels useful
        // resolution; a silent config regression would flatten them again.
        registry.timer("vaadin.request.duration", "outcome", "success")
                .record(java.time.Duration.ofMillis(120));
        String exposition = registry.scrape();

        for (String boundary : new String[] { "0.025", "0.05", "0.1", "0.25",
                "0.5", "1.0", "2.0" }) {
            assertTrue(
                    exposition
                            .contains("vaadin_request_duration_seconds_bucket")
                            && exposition.contains("le=\"" + boundary + "\""),
                    "request duration should publish an SLO bucket at le="
                            + boundary);
        }
    }

    private long requestTimerCount() {
        return registry.find("vaadin.request.duration").timers().stream()
                .mapToLong(t -> t.count()).sum();
    }
}
