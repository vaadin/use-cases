package com.example.uc6;

import com.example.common.UnreleasedApi;
import com.example.common.UseCaseDescription;
import com.example.documents.SampleDocuments;
import com.example.documents.SampleDocuments.Order;
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
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC6 — Print a shipping label.
 * <p>
 * A warehouse worker prints the label of an order from the order list, with one
 * click on the row. The application calls {@code PdfViewer#print()}, which
 * renders the label for printing and opens the browser's print dialog.
 * <p>
 * {@code print()} does nothing until the document has loaded, so for an order
 * whose label is not shown yet the view sets the label as the viewer's source
 * first and prints on {@code document-load}. Whether the user then printed or
 * cancelled, the server is not told, so the list can not mark the order as
 * printed.
 */
@UnreleasedApi("https://github.com/vaadin/web-components/pull/13053")
@Route(value = "uc6", layout = MainLayout.class)
@PageTitle("UC6 — Print a shipping label")
@UseCaseDescription("Printing a document from a button in the app's own UI")
@Menu(order = 6, title = "UC6 — Print a shipping label")
@StyleSheet("uc6.css")
public class PrintLabelView extends VerticalLayout {

    private final PdfViewer viewer = new PdfViewer();
    private final Paragraph log = new Paragraph();
    private @Nullable Order shown;
    private @Nullable Order loaded;
    private boolean printWhenLoaded;

    public PrintLabelView() {
        addClassName("uc6-view");
        add(new H1("UC6 — Print a shipping label"));
        add(new Paragraph("Click \"Print label\" on an order. The label opens "
                + "in the viewer and the print dialog follows. Pick \"Save as "
                + "PDF\" in the dialog if you do not want to use paper."));

        Grid<Order> orders = new Grid<>();
        orders.addColumn(Order::number).setHeader("Order").setAutoWidth(true)
                .setFlexGrow(0);
        orders.addColumn(Order::recipient).setHeader("Recipient");
        orders.addComponentColumn(order -> {
            Button print = new Button("Print label",
                    event -> printLabel(order));
            print.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.SMALL);
            return print;
        }).setHeader("Label").setAutoWidth(true).setFlexGrow(0);
        orders.setItems(SampleDocuments.ORDERS);
        orders.setAllRowsVisible(true);
        orders.addClassName("uc6-orders");

        log.addClassName("uc6-log");
        viewer.addClassName("uc6-viewer");
        viewer.setZoom(PdfViewer.Zoom.PAGE_FIT);
        viewer.addDocumentLoadListener(event -> {
            loaded = shown;
            if (printWhenLoaded) {
                printWhenLoaded = false;
                print();
            }
        });

        Div list = new Div(orders, log);
        list.addClassName("uc6-list");
        Div layout = new Div(list, viewer);
        layout.addClassNames("uc6-layout", "side-by-side");
        add(layout);
        show(SampleDocuments.ORDERS.getFirst());
        add(ApiGap.note(5, "print() resolves the same way whether the user "
                + "printed, cancelled or the print failed, so the app can not "
                + "mark a label as printed."));
        add(ApiGap.note(7, "the viewer's own print and download buttons can "
                + "not be hidden, so the toolbar repeats the app's button."));
    }

    private void printLabel(Order order) {
        if (order.equals(loaded)) {
            print();
            return;
        }
        show(order);
        printWhenLoaded = true;
        log.setText("Loading the label of order " + order.number() + "…");
    }

    private void show(Order order) {
        shown = order;
        loaded = null;
        printWhenLoaded = false;
        viewer.setSrc(order.label().handler());
    }

    private void print() {
        viewer.print();
        Order order = loaded;
        log.setText("Opened the print dialog for order "
                + (order == null ? "—" : order.number())
                + ". The app is not told whether you printed it.");
    }
}
