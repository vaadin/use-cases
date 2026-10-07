package com.example.uc7;

import com.example.home.HomeView;
import com.example.uc7.MonitoringStackView.Row;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The hosted demo runs Prometheus and Grafana as sibling Fly apps and hands
 * their addresses to the view through properties (see fly.toml). The browser
 * follows the public hostnames while the server calls Prometheus over the
 * private network, so the two must not be mixed up: a link to the private
 * hostname is dead for a visitor, and an API call to the public one goes
 * through the proxy for nothing.
 */
@SpringBootTest(properties = {
        "uc7.prometheus.url=https://prometheus.example.test",
        // Refused immediately, so the view degrades without a DNS wait.
        "uc7.prometheus.api-url=http://127.0.0.1:1",
        "uc7.grafana.url=https://grafana.example.test" })
@ViewPackages(classes = { MonitoringStackView.class, HomeView.class })
class MonitoringStackViewHostedUrlsTest extends SpringBrowserlessTest {

    @Test
    void linksFollowThePublicHostnames() {
        navigate(MonitoringStackView.class);
        runPendingSignalsTasks();

        assertEquals("https://prometheus.example.test/targets",
                href("Prometheus targets"));
        assertEquals("https://prometheus.example.test/graph",
                href("Prometheus graph"));
        assertEquals("https://grafana.example.test/d/vaadin-app",
                href("Grafana dashboard"));
    }

    @Test
    void querySourcesOpenPrefilledInPrometheus() {
        navigate(MonitoringStackView.class);
        runPendingSignalsTasks();

        Grid<Row> grid = findInView(Grid.class).single();
        Row p95 = grid.getListDataView().getItems()
                .filter(r -> r.signal().startsWith("Request p95")).findFirst()
                .orElseThrow();
        assertTrue(p95.href() != null && p95.href().startsWith(
                "https://prometheus.example.test/graph?g0.expr=histogram_quantile"),
                "a dashboard query should open in Prometheus: " + p95.href());
        assertTrue(p95.href().contains("&g0.tab=graph"),
                "the graph tab should be selected: " + p95.href());

        // The scrape row's source is the API the server called, but it opens
        // the human-readable targets page on the public hostname.
        Row scrape = grid.getListDataView().getItems()
                .filter(r -> "Prometheus scrape target".equals(r.signal()))
                .findFirst().orElseThrow();
        assertEquals("https://prometheus.example.test/targets", scrape.href());

        // And the example queries follow the same public hostname. (Grid cells
        // are rendered on demand, so the anchors found here are the example
        // section's; the grid rows are covered through Row.href above.)
        long examples = findInView(Anchor.class).all().stream()
                .filter(a -> a.getHref().startsWith(
                        "https://prometheus.example.test/graph?g0.expr="))
                .count();
        assertEquals(6, examples,
                "the six example queries should link to the public Prometheus");
    }

    @Test
    void apiCallsFollowTheApiUrl() {
        navigate(MonitoringStackView.class);
        runPendingSignalsTasks();

        Grid<Row> grid = findInView(Grid.class).single();
        Row scrape = grid.getListDataView().getItems()
                .filter(r -> "Prometheus scrape target".equals(r.signal()))
                .findFirst().orElseThrow();
        assertEquals("http://127.0.0.1:1/api/v1/targets", scrape.source());
        assertTrue(scrape.value().contains("not reachable at http://127.0.0.1:1"),
                "the readout should name the API address it tried: "
                        + scrape.value());
    }

    /** The action links are anchors wrapping a tertiary button as their face. */
    private String href(String label) {
        return findInView(Anchor.class).all().stream()
                .filter(a -> a.getChildren().anyMatch(c -> c instanceof Button b
                        && label.equals(b.getText())))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no link " + label))
                .getHref();
    }
}
