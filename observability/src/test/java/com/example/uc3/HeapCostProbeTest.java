package com.example.uc3;

import com.example.uc3.HeapCostProbe.HeapCost;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The probe needs no Vaadin environment: a component tree can be built and
 * weighed detached, which is what lets it measure a batch of views without
 * attaching them to a UI.
 */
class HeapCostProbeTest {

    @Test
    void measuresANodeCountAndAByteCost() {
        HeapCost cost = HeapCostProbe.measure(HeapCostProbe.DEFAULT_INSTANCES);

        assertTrue(cost.nodesPerInstance() > 0,
                "a representative view should have measurable state");
        // A batch of data-bound grids is megabytes of retained objects, so the
        // heap delta must be positive even allowing for collector noise. If
        // this ever goes flat, the value UC3 tells you to configure is built on
        // sand and should fail loudly rather than quietly look plausible.
        assertTrue(cost.isMeasured(),
                "retaining " + HeapCostProbe.DEFAULT_INSTANCES
                        + " views should show up as a heap delta, got "
                        + cost.heapDeltaBytes() + " bytes");
        assertTrue(cost.bytesPerInstance() > 0,
                "a measured cost per view instance should be positive");
        assertTrue(cost.bytesPerNode() > 0,
                "the value to configure as ui-state-bytes-per-node should be "
                        + "positive");
        // The number goes into a kit property typed as int, so it has to be a
        // plausible per-node cost rather than something absurd.
        assertTrue(cost.bytesPerNode() < 100_000,
                "a per-node cost of " + cost.bytesPerNode()
                        + " B is not credible; the measurement is off");

        System.out.println("MEASURED bytesPerNode=" + cost.bytesPerNode()
                + " bytesPerInstance=" + cost.bytesPerInstance()
                + " nodesPerInstance=" + cost.nodesPerInstance());
    }
}
