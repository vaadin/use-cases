package com.example.uc3;

import com.example.home.HomeView;
import com.example.uc3.ScalingSignalsView.Row;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.observability.micrometer.MeterNames;
import com.vaadin.observability.micrometer.ObservabilitySettings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The kit's UI-state measurement is opt-in and <em>off by default</em>, because
 * it costs a state-tree walk per sampled interaction. An application that reads
 * those meters therefore has to cope with them not existing — and a page of em
 * dashes with no explanation is the worst way to do it. This is the default
 * configuration, not an exotic one, so it gets its own context.
 */
@SpringBootTest(properties = "vaadin.observability.ui-state=false")
@ViewPackages(classes = { ScalingSignalsView.class, HomeView.class })
@DirtiesContext
class ScalingSignalsViewDisabledTest extends SpringBrowserlessTest {

    @Autowired
    MeterRegistry registry;

    @Autowired
    ObservabilitySettings settings;

    @Test
    void withTheFeatureOffTheViewSaysSoInsteadOfShowingZeros() {
        assertFalse(settings.isUiState(),
                "this context should have the feature switched off");
        assertNull(registry.find(MeterNames.UI_STATE_NODES).gauge(),
                "the kit should publish no state-size gauge when off");

        navigate(ScalingSignalsView.class);
        runPendingSignalsTasks();

        assertTrue(
                findInView(H1.class).all().stream()
                        .anyMatch(h -> h.getText().startsWith("UC3")),
                "the view must still render with the meters absent");

        // Told, not left to guess: the badge and the configuration readout both
        // name the property to set.
        String verdict = findInView(Span.class).id("capacity-verdict")
                .getText();
        assertTrue(verdict.contains("not being measured"),
                "the badge should say the measurement is off: " + verdict);
        assertTrue(verdict.contains("vaadin.observability.ui-state"),
                "the badge should name the property to turn it on: " + verdict);

        String config = findInView(Div.class).id("ui-state-config").getText();
        assertTrue(config.contains("ui-state is off"),
                "the config readout should state the feature is off: "
                        + config);

        // A missing meter reads as an em dash rather than as a real zero, which
        // would claim the server is holding no state at all.
        assertEquals("—", capacityRow("UI state nodes (total)").value(),
                "an absent gauge must not be reported as zero");
        assertEquals("—", capacityRow("Largest single UI").value(),
                "an absent gauge must not be reported as zero");

        // The counts the kit always publishes are unaffected, so the view is
        // still useful with the feature off.
        assertFalse(
                "—".equals(capacityRow("Active UIs (browser tabs)").value()),
                "UI counts do not depend on the ui-state feature");
    }

    private Row capacityRow(String signal) {
        Grid<Row> grid = findInView(Grid.class).id("capacity-grid");
        return grid.getListDataView().getItems()
                .filter(row -> signal.equals(row.signal())).findFirst()
                .orElseThrow(() -> new AssertionError(
                        "no readout row named: " + signal));
    }
}
