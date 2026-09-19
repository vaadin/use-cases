package com.example.home;

import com.example.common.BaseHomeView;
import com.example.uc1.PrintCurrentViewView;
import com.example.uc2.PrintRouteView;
import com.example.uc3.PrintableListView;
import com.example.uc4.PrintPreviewView;
import com.example.uc5.HeaderFooterView;
import com.example.uc6.PrintDashboardView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Printing — use cases",
                "Vaadin has no printing API: no Page#print(), no print "
                        + "lifecycle events, no way to mark a component as "
                        + "chrome rather than content, and no API for the CSS "
                        + "page box. Each card below prints something real "
                        + "anyway, and API-GAPS.md records what it cost.");

        Div cards = new Div();
        cards.addClassName("home-cards");
        cards.add(homeCard("UC1", "Print the current view",
                "One button, and an application shell that stays off the paper.",
                PrintCurrentViewView.class));
        cards.add(homeCard("UC2", "A print-only route",
                "The document opens in its own window, prints itself and closes.",
                PrintRouteView.class));
        cards.add(homeCard("UC3", "Printing a long list",
                "A virtualised Grid prints nothing; the same rows as a table print everything.",
                PrintableListView.class));
        cards.add(homeCard("UC4", "Paper setup and preview",
                "Paper size, orientation and margins, previewed and applied to @page.",
                PrintPreviewView.class));
        cards.add(homeCard("UC5", "Letterhead and page numbers",
                "Server-side pagination, because CSS page counters are not implemented.",
                HeaderFooterView.class));
        cards.add(homeCard("UC6", "Printing a dashboard",
                "Charts reflowed to the paper width, widgets kept whole.",
                PrintDashboardView.class));
        add(cards);
    }
}
