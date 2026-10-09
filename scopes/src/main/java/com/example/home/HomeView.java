package com.example.home;

import com.example.common.BaseHomeView;
import com.example.uc1.MaintenanceBannerView;
import com.example.uc2.SharedCartView;
import com.example.uc3.OrderExportView;
import com.example.uc4.PriceListEditorView;
import com.example.uc5.CustomerDetailsView;
import com.example.uc6.SeatSelectionView;
import com.example.uc7.TicketPerTabView;
import com.example.uc8.ScopePlaygroundView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Scopes — use cases",
                "Each card below shows a real situation where one bean scope "
                        + "is the right fit: application (singleton), Spring "
                        + "HTTP session, Vaadin session, browser tab, UI and "
                        + "route. The scope decides who shares a bean and "
                        + "which user actions — a reload, a second tab, "
                        + "navigating away — keep or discard it.");

        Div cards = new Div();
        cards.addClassName("home-cards");
        cards.add(homeCard("UC1 · Application", "Maintenance banner",
                "A notice every user sees at once.",
                MaintenanceBannerView.class));
        cards.add(homeCard("UC2 · Vaadin session", "Shared shopping cart",
                "One cart for all of a user's tabs.", SharedCartView.class));
        cards.add(homeCard("UC3 · HTTP session", "Export settings",
                "Settings a plain Spring MVC download endpoint also reads.",
                OrderExportView.class));
        cards.add(homeCard("UC4 · UI", "Undo history",
                "Toolbar and editor share one undo history per window.",
                PriceListEditorView.class));
        cards.add(homeCard("UC5 · Route", "Customer draft",
                "A draft kept across the pages of an editor, dropped on leaving.",
                CustomerDetailsView.class));
        cards.add(homeCard("UC6 · Browser tab", "Booking wizard",
                "A booking that survives reloads, separate per tab.",
                SeatSelectionView.class));
        cards.add(homeCard("UC7 · Browser tab", "Ticket per tab",
                "Each tab keeps its own ticket and reply after a reload.",
                TicketPerTabView.class));
        cards.add(homeCard("UC8 · All scopes", "Scope playground",
                "Counters showing which scope survives which action.",
                ScopePlaygroundView.class));
        add(cards);
    }
}
