package com.example.uc6;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.function.Supplier;

import com.example.MissingAPI;
import com.example.backend.Order;
import com.example.backend.OrderService;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC6 — Build heavy sections only when needed.
 * <p>
 * A customer page has much more on it than anyone looks at in one visit: an
 * order history, an audit log, an activity heatmap at the bottom. Building all
 * of it up front makes every visit pay for every section. Here each heavy
 * section starts as an empty placeholder and is built the first time the user
 * actually needs it — when its tab is selected, when its {@link Details} is
 * opened, or when it scrolls into view.
 * <p>
 * Tabs and details have server-side events for this; scrolling into view does
 * not, so the heatmap relies on {@link MissingAPI#onFirstVisible}.
 */
@Route(value = "uc6", layout = MainLayout.class)
@PageTitle("UC6 — Build sections when needed")
@UseCaseDescription("Deferring heavy parts of a page until a tab, a details or the scroll position asks for them")
@Menu(order = 6, title = "UC6 — Build when needed")
@StyleSheet("uc6.css")
public class DeferredSectionsView extends VerticalLayout {

    static final int HISTORY_ROWS = 5_000;
    static final int HEATMAP_WEEKS = 52;

    private final OrderService orders;
    private final ValueSignal<List<String>> built = new ValueSignal<>(
            List.of());

    public DeferredSectionsView(OrderService orders) {
        this.orders = orders;
        addClassName("uc6-view");

        add(new H1("UC6 — Build sections when needed"));
        add(new Paragraph("Only the summary is built when the page opens. "
                + "Select a tab, open the audit log or scroll to the bottom, "
                + "and watch the counter: each section is built the first "
                + "time it is needed, and only once."));

        Span counter = new Span();
        counter.addClassName("built-counter");
        counter.bindText(built
                .map(names -> names.isEmpty() ? "Heavy sections built: none yet"
                        : "Heavy sections built: " + String.join(", ", names)));
        add(counter);

        TabSheet tabs = new TabSheet();
        tabs.setWidthFull();
        tabs.add("Summary", new Paragraph("Aurora Bakery — customer since "
                + "2020, 312 orders, account in good standing."));
        Div history = deferred();
        tabs.add("Order history", history);
        Div attachments = deferred();
        tabs.add("Attachments", attachments);
        tabs.addSelectedChangeListener(event -> {
            if (event.getSelectedTab().getLabel().equals("Order history")) {
                buildOnce(history, "order history", this::buildHistory);
            } else if (event.getSelectedTab().getLabel()
                    .equals("Attachments")) {
                buildOnce(attachments, "attachments", this::buildAttachments);
            }
        });
        add(tabs);

        Div auditContent = deferred();
        Details audit = new Details("Audit log", auditContent);
        audit.addOpenedChangeListener(event -> {
            if (event.isOpened()) {
                buildOnce(auditContent, "audit log", this::buildAuditLog);
            }
        });
        add(audit);

        Div spacer = new Div(new Paragraph(
                "Scroll down: the activity heatmap is built when it comes "
                        + "into view."));
        spacer.addClassName("spacer");
        add(spacer);

        add(new H2("Activity this year"));
        Div heatmap = deferred();
        heatmap.addClassName("heatmap-slot");
        MissingAPI.onFirstVisible(heatmap,
                () -> buildOnce(heatmap, "heatmap", this::buildHeatmap));
        add(heatmap);
    }

    private static Div deferred() {
        Div placeholder = new Div();
        placeholder.addClassName("deferred");
        return placeholder;
    }

    private void buildOnce(Div placeholder, String name,
            Supplier<Component> builder) {
        if (placeholder.getComponentCount() > 0) {
            return;
        }
        placeholder.add(builder.get());
        List<String> names = new ArrayList<>(built.peek());
        names.add(name);
        built.set(List.copyOf(names));
    }

    private Component buildHistory() {
        Grid<Order> grid = new Grid<>();
        grid.addColumn(Order::id).setHeader("Order");
        grid.addColumn(Order::product).setHeader("Product");
        grid.addColumn(Order::total).setHeader("Total");
        grid.addColumn(Order::ordered).setHeader("Ordered");
        grid.setItems(orders.fetch("", true, 0, HISTORY_ROWS).toList());
        return grid;
    }

    private Component buildAttachments() {
        VerticalLayout list = new VerticalLayout();
        for (String name : List.of("Contract 2024.pdf", "VAT certificate.pdf",
                "Delivery terms.docx")) {
            list.add(new Span(name));
        }
        return list;
    }

    private Component buildAuditLog() {
        VerticalLayout log = new VerticalLayout();
        log.setSpacing(false);
        for (int i = 1; i <= 200; i++) {
            log.add(new Span("2026-%02d-%02d  Credit limit reviewed by clerk %d"
                    .formatted(1 + i % 12, 1 + i % 28, 1 + i % 7)));
        }
        return log;
    }

    private Component buildHeatmap() {
        Div grid = new Div();
        grid.addClassName("heatmap");
        SplittableRandom random = new SplittableRandom(7);
        for (int day = 0; day < HEATMAP_WEEKS * 7; day++) {
            Div cell = new Div();
            cell.addClassName("level-" + random.nextInt(5));
            grid.add(cell);
        }
        return grid;
    }

    // Package-private test seam.
    List<String> builtSections() {
        return built.peek();
    }
}
