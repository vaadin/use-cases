package com.example.home;

import com.example.common.BaseHomeView;
import com.example.uc1.DownloadInvoiceView;
import com.example.uc2.OpenInNewTabView;
import com.example.uc3.PreviewInvoiceView;
import com.example.uc4.BillingRunView;
import com.example.uc5.DeliveryReceiptView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Invoicing — use cases",
                "Turning application data into a business document and "
                        + "getting it to the customer: generate the PDF, hand "
                        + "it to the browser, show it before it goes out, run "
                        + "a whole month of them, and know that it arrived. "
                        + "API-GAPS.md records where Vaadin stops helping.");

        Div cards = new Div();
        cards.addClassName("home-cards");
        cards.add(homeCard("UC1", "Download the invoice",
                "A generated PDF delivered through DownloadHandler.",
                DownloadInvoiceView.class));
        cards.add(homeCard("UC2", "Open it in a new tab",
                "Inline delivery, and why the server cannot open it itself.",
                OpenInNewTabView.class));
        cards.add(homeCard("UC3", "Check it before sending",
                "The real document previewed inside the application.",
                PreviewInvoiceView.class));
        cards.add(homeCard("UC4", "The monthly billing run",
                "Many invoices as one numbered document, with progress.",
                BillingRunView.class));
        cards.add(homeCard("UC5", "Know that it arrived",
                "Mark an invoice sent only when the transfer completed.",
                DeliveryReceiptView.class));
        add(cards);
    }
}
