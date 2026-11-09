package com.example.uc6;

import com.example.common.UseCaseDescription;
import com.example.documents.SampleDocuments;
import com.example.documents.SampleDocuments.Order;
import com.example.views.ApiGap;
import com.example.views.MainLayout;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.pdfviewer.PdfViewer;
import com.vaadin.flow.component.pdfviewer.Zoom;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC6 — Print a shipping label.
 * <p>
 * A warehouse worker prints the label of an order from the order list. The
 * viewer shows the label of the selected order, but it can not print
 * (API-GAPS.md #2): it has no print button and no {@code print()} method. The
 * "Print label" link of each row therefore opens the label in a new tab, served
 * inline, where the browser's own PDF viewer prints it.
 * <p>
 * The server is not told whether the user printed the label, so the list can
 * not mark the order as printed.
 */
@Route(value = "uc6", layout = MainLayout.class)
@PageTitle("UC6 — Print a shipping label")
@UseCaseDescription("Printing a document from a button in the app's own UI")
@Menu(order = 6, title = "UC6 — Print a shipping label")
@StyleSheet("uc6.css")
public class PrintLabelView extends VerticalLayout {

    private final PdfViewer viewer = new PdfViewer();

    public PrintLabelView() {
        addClassName("uc6-view");
        add(new H1("UC6 — Print a shipping label"));
        add(new Paragraph("Pick an order to see its label in the viewer. The "
                + "viewer can not print, so \"Print label\" opens the label "
                + "in a new tab: print it from the browser's PDF viewer there. "
                + "Pick \"Save as PDF\" in the print dialog if you do not "
                + "want to use paper."));

        Grid<Order> orders = new Grid<>();
        orders.addColumn(Order::number).setHeader("Order").setAutoWidth(true)
                .setFlexGrow(0);
        orders.addColumn(Order::recipient).setHeader("Recipient");
        orders.addComponentColumn(PrintLabelView::printLink).setHeader("Label")
                .setAutoWidth(true).setFlexGrow(0);
        orders.setItems(SampleDocuments.ORDERS);
        orders.setAllRowsVisible(true);
        orders.addThemeVariants(GridVariant.WRAP_CELL_CONTENT);
        orders.addClassName("uc6-orders");
        orders.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                viewer.setSrc(event.getValue().label().handler());
            }
        });

        viewer.addClassName("uc6-viewer");
        viewer.setZoom(Zoom.PAGE_FIT);

        Div layout = new Div(orders, viewer);
        layout.addClassNames("uc6-layout", "side-by-side");
        add(layout);
        add(ApiGap.note(2, "the viewer can not print, and the app is not told "
                + "whether the user printed, so it can not mark a label as "
                + "printed."));
        orders.select(SampleDocuments.ORDERS.getFirst());
    }

    private static Anchor printLink(Order order) {
        Anchor link = new Anchor(order.label().handler().inline(),
                "Print label");
        link.setTarget(AnchorTarget.BLANK);
        return link;
    }
}
