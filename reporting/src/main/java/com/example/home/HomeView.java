package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.uc1.DownloadInvoiceView;
import com.example.uc2.OpenInNewTabView;
import com.example.uc3.PreviewInvoiceView;
import com.example.uc4.BillingRunView;
import com.example.uc5.DeliveryReceiptView;
import com.example.views.MainLayout;

import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Reporting — use cases",
                "Turning application data into a business document and "
                        + "getting it to the customer: generate the PDF, hand "
                        + "it to the browser, show it before it goes out, run "
                        + "a whole month of them, and know that it arrived. "
                        + "API-GAPS.md records where Vaadin stops helping.");
        addMenuCards(List.of(DownloadInvoiceView.class, OpenInNewTabView.class,
                PreviewInvoiceView.class, BillingRunView.class,
                DeliveryReceiptView.class));
    }
}
