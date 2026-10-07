package com.example.uc3;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.StateNode;

/**
 * Produces the one number the Observability Kit cannot measure for itself: what
 * a state-tree node costs in bytes.
 * <p>
 * The kit measures UI state in <em>nodes</em> and says plainly why it stops
 * there — a node count is a proxy for retained heap, not a measurement of it,
 * since one {@code Grid} node backed by 100 000 rows counts as a single node.
 * So it publishes no byte figure unless the application supplies the
 * conversion:
 *
 * <pre>
 * vaadin.observability.ui-state-bytes-per-node=96
 * </pre>
 *
 * with which {@code vaadin.ui.state.size} becomes available. That is the right
 * split — a guessed per-user cost published as a metric is worse than a missing
 * one — but it leaves the application to come up with the number, and the kit's
 * documentation describes the method rather than providing it: settle the heap,
 * build a number of copies of a representative view, keep them reachable, and
 * read the difference from {@code MemoryMXBean}. This class is that method, so
 * UC3 can show where the configured value comes from and whether it still
 * holds.
 * <p>
 * <strong>What the number is and is not.</strong> It is the retained cost of a
 * component tree plus the data it holds, measured in this JVM with these
 * components — good enough to configure the kit with, and far better than a
 * guessed constant. It is not exact: {@code System.gc()} is a hint, not a
 * command, so a concurrent collection or an allocation on another thread lands
 * in the delta; and it excludes everything a real session holds
 * <em>outside</em> the tree — session attributes, security context, JPA
 * first-level cache, application caches the view merely points at. Treat it as
 * a floor on per-user cost, re-measure on the JVM you deploy, and re-measure
 * when the views change shape, because a configured constant cannot notice that
 * they have.
 */
final class HeapCostProbe {

    /**
     * Enough instances that the per-instance cost is much larger than the
     * measurement noise, few enough that the probe stays a click rather than a
     * pause.
     */
    static final int DEFAULT_INSTANCES = 200;

    /**
     * How many times the batch is built and weighed. Odd, so the median is a
     * real reading rather than an average of two.
     */
    private static final int ROUNDS = 3;

    /** How many rows of backing data the representative view's grid holds. */
    private static final int GRID_ROWS = 100;

    private static final MemoryMXBean MEMORY = ManagementFactory
            .getMemoryMXBean();

    private HeapCostProbe() {
    }

    /**
     * The measured cost of holding one view's worth of state on the server.
     *
     * @param instances
     *            how many copies were built
     * @param heapDeltaBytes
     *            total heap the batch retained
     * @param nodesPerInstance
     *            state-tree nodes in one instance, counted the same way the kit
     *            counts them, so bytes and nodes are in the same unit
     * @param measuredAt
     *            wall-clock millis, so the readout can say how old it is
     */
    record HeapCost(int instances, long heapDeltaBytes, int nodesPerInstance,
            long measuredAt) {

        static final HeapCost NONE = new HeapCost(0, 0, 0, 0);

        boolean isMeasured() {
            return instances > 0 && heapDeltaBytes > 0 && nodesPerInstance > 0;
        }

        /** Bytes one retained view instance costs. */
        long bytesPerInstance() {
            return instances == 0 ? 0 : heapDeltaBytes / instances;
        }

        /**
         * Bytes one state node costs — the value to configure as
         * {@code vaadin.observability.ui-state-bytes-per-node}.
         */
        int bytesPerNode() {
            long nodes = (long) instances * nodesPerInstance;
            return nodes == 0 ? 0 : (int) (heapDeltaBytes / nodes);
        }
    }

    /**
     * Runs the measurement. Blocks the calling thread for a moment.
     * <p>
     * The batch is built and weighed {@value #ROUNDS} times and the median heap
     * delta is used, because a single reading is noisy: {@code System.gc()} is
     * a request, and a collection landing inside a reading can move the result
     * by a factor of two. A median of a few rounds is what makes the output
     * stable enough to paste into a configuration file.
     */
    static HeapCost measure(int instances) {
        long[] deltas = new long[ROUNDS];
        int nodesPerInstance = 0;
        for (int round = 0; round < ROUNDS; round++) {
            List<Component> held = new ArrayList<>(instances);
            settle();
            long before = usedHeap();
            for (int i = 0; i < instances; i++) {
                held.add(representativeView());
            }
            settle();
            deltas[round] = Math.max(0, usedHeap() - before);
            // Counted after the reading so that walking a tree cannot allocate
            // into the measured delta. `held` is still strongly referenced
            // here, which is what kept the batch out of the collector's reach
            // during settle().
            if (!held.isEmpty()) {
                nodesPerInstance = stateNodes(held.get(0).getElement());
            }
            held.clear();
        }
        Arrays.sort(deltas);
        return new HeapCost(instances, deltas[ROUNDS / 2], nodesPerInstance,
                System.currentTimeMillis());
    }

    /**
     * Counts state-tree nodes the way the kit counts a live UI's — through
     * {@code StateNode.visitNodeTree}, because an {@code Element.getChildren()}
     * walk cannot reach virtual children. The kit keeps its own sampler
     * internal, so a probe that has to produce numbers in the kit's unit counts
     * them again here.
     * <p>
     * Worth knowing when reading the result: a component's <em>data</em> is not
     * in this count. A {@code Grid} holding a hundred rows is a handful of
     * nodes, and those rows are most of its weight. That is why bytes per node
     * comes out very differently for a data-bound view than for a form, and why
     * one global constant can only ever approximate a mixed application.
     */
    private static int stateNodes(Element root) {
        int[] nodes = { 0 };
        root.getNode().visitNodeTree((StateNode node) -> nodes[0]++);
        return nodes[0];
    }

    /**
     * A stand-in for a real screen: a heading, a form, a grid with its backing
     * rows, and an action bar. The point is not to match any particular view
     * but to have the same <em>kind</em> of state a view holds — components,
     * listeners, and the data behind them.
     */
    private static Component representativeView() {
        VerticalLayout view = new VerticalLayout();
        view.add(new H2("Orders"));

        FormLayout form = new FormLayout();
        ComboBox<String> status = new ComboBox<>("Status");
        status.setItems("New", "Packed", "Shipped", "Delivered");
        form.add(new TextField("Customer"), new TextField("Reference"), status,
                new Checkbox("Priority"), new TextArea("Notes"));
        view.add(form);

        Grid<Order> grid = new Grid<>();
        grid.addColumn(Order::reference).setHeader("Reference");
        grid.addColumn(Order::customer).setHeader("Customer");
        grid.addColumn(Order::status).setHeader("Status");
        grid.addColumn(Order::total).setHeader("Total");
        grid.setItems(orders());
        view.add(grid);

        view.add(new HorizontalLayout(new Button("Save"), new Button("Cancel"),
                new Span("Unsaved changes")));
        return view;
    }

    private record Order(String reference, String customer, String status,
            String total) {
    }

    private static List<Order> orders() {
        List<Order> orders = new ArrayList<>(GRID_ROWS);
        for (int i = 0; i < GRID_ROWS; i++) {
            orders.add(new Order("REF-" + i, "Customer " + i,
                    i % 2 == 0 ? "Shipped" : "New", i * 13 + ".00"));
        }
        return orders;
    }

    private static long usedHeap() {
        return MEMORY.getHeapMemoryUsage().getUsed();
    }

    /**
     * Best-effort quiescing of the heap before a reading. {@code System.gc()}
     * only requests a collection, so it is called twice with a short pause: the
     * first pass usually clears the garbage the previous step made, the second
     * catches what the first promoted. This is the weakest link in the
     * measurement and the reason the readout calls the result an estimate.
     */
    private static void settle() {
        for (int i = 0; i < 2; i++) {
            System.gc();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
