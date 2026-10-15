package com.example.uc9;

import java.util.HashMap;
import java.util.Map;

import com.example.common.UseCaseDescription;
import com.example.documents.SampleDocuments;
import com.example.documents.SampleDocuments.PurchaseOrder;
import com.example.pdfviewer.PdfViewer;
import com.example.views.ApiGap;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC9 — Approve in the toolbar.
 * <p>
 * A manager goes through the purchase orders waiting for approval. The decision
 * is made while looking at the document, so the "Approve" and "Reject" buttons
 * sit in the viewer's toolbar, next to its own actions, each with a tooltip
 * that says what happens. A decision moves on to the next waiting order.
 * <p>
 * The buttons are ordinary Flow {@code Button}s with
 * {@code setTooltipText(...)}. The web component has no slot for application
 * controls, so {@code PdfViewer#addToToolbar} puts them into the slot of the
 * viewer's own action buttons (API-GAPS.md #7).
 */
@Route(value = "uc9", layout = MainLayout.class)
@PageTitle("UC9 — Approve in the toolbar")
@UseCaseDescription("Adding the app's own actions, with tooltips, to the viewer's toolbar")
@Menu(order = 9, title = "UC9 — Approve in the toolbar")
@StyleSheet("uc9.css")
public class ApproveInToolbarView extends VerticalLayout {

    /** What the manager decided about an order. */
    enum Decision {
        WAITING("Waiting"), APPROVED("Approved"), REJECTED("Rejected");

        private final String label;

        Decision(String label) {
            this.label = label;
        }
    }

    private final Map<PurchaseOrder, Decision> decisions = new HashMap<>();
    private final Grid<PurchaseOrder> orders = new Grid<>();
    private final PdfViewer viewer = new PdfViewer();
    private final Button approve = new Button(VaadinIcon.CHECK.create());
    private final Button reject = new Button(VaadinIcon.BAN.create());
    private @Nullable PurchaseOrder shown;

    public ApproveInToolbarView() {
        addClassName("uc9-view");
        add(new H1("UC9 — Approve in the toolbar"));
        add(new Paragraph("Purchase orders over 1,000 EUR need a manager's "
                + "approval. Open an order and decide with the two buttons the "
                + "app adds to the viewer's toolbar, before the viewer's own "
                + "find, download and print buttons. Hover over them, or "
                + "focus them with the keyboard, to see what each one does. "
                + "The next waiting order opens after each decision."));

        orders.addColumn(PurchaseOrder::number).setHeader("Order")
                .setAutoWidth(true).setFlexGrow(0);
        orders.addColumn(PurchaseOrder::supplier).setHeader("Supplier");
        orders.addColumn(order -> SampleDocuments.money(order.total()))
                .setHeader("Total").setAutoWidth(true).setFlexGrow(0);
        orders.addColumn(order -> decision(order).label).setHeader("Status")
                .setAutoWidth(true).setFlexGrow(0);
        orders.setItems(SampleDocuments.PURCHASE_ORDERS);
        orders.setAllRowsVisible(true);
        orders.addClassName("uc9-orders");
        orders.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                show(event.getValue());
            }
        });

        approve.setAriaLabel("Approve");
        approve.setTooltipText(
                "Approve the order. It is sent to the supplier.");
        approve.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SUCCESS);
        approve.addClickListener(event -> decide(Decision.APPROVED));
        reject.setAriaLabel("Reject");
        reject.setTooltipText(
                "Reject the order. The requester gets it back to change it.");
        reject.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.ERROR);
        reject.addClickListener(event -> decide(Decision.REJECTED));
        viewer.addToToolbar(approve, reject);
        viewer.addClassName("uc9-viewer");

        Div layout = new Div(orders, viewer);
        layout.addClassNames("uc9-layout", "side-by-side");
        add(layout);
        add(ApiGap.note(7, "the viewer has no slot for the app's own toolbar "
                + "buttons. These use the slot of the viewer's own buttons, "
                + "which is not part of its API."));

        orders.select(SampleDocuments.PURCHASE_ORDERS.getFirst());
    }

    private Decision decision(PurchaseOrder order) {
        return decisions.getOrDefault(order, Decision.WAITING);
    }

    private void show(PurchaseOrder order) {
        shown = order;
        viewer.setSrc(order.pdf().handler());
        boolean waiting = decision(order) == Decision.WAITING;
        approve.setEnabled(waiting);
        reject.setEnabled(waiting);
    }

    private void decide(Decision decision) {
        PurchaseOrder order = shown;
        if (order == null) {
            return;
        }
        decisions.put(order, decision);
        orders.getDataProvider().refreshItem(order);
        SampleDocuments.PURCHASE_ORDERS.stream()
                .filter(next -> decision(next) == Decision.WAITING).findFirst()
                .ifPresentOrElse(orders::select, () -> show(order));
    }
}
