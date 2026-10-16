package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.common.UseCaseDescription;
import com.example.uc1.MaintenanceBannerView;
import com.example.uc2.SharedCartView;
import com.example.uc3.OrderExportView;
import com.example.uc4.PriceListEditorView;
import com.example.uc5.CustomerDetailsView;
import com.example.uc6.SeatSelectionView;
import com.example.uc7.ScopePlaygroundView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@UseCaseDescription("Picking the bean scope that shares state with the right users, tabs and pages")
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
        addGroup("Shared by users and sessions", VaadinIcon.USERS, Accent.BLUE,
                List.of(MaintenanceBannerView.class, SharedCartView.class,
                        OrderExportView.class));
        addGroup("Kept for one window or editor", VaadinIcon.EDIT,
                Accent.PURPLE,
                List.of(PriceListEditorView.class, CustomerDetailsView.class));
        addGroup("Kept per browser tab", VaadinIcon.BROWSER, Accent.GREEN,
                List.of(SeatSelectionView.class));
        addGroup("All scopes side by side", VaadinIcon.SLIDERS, Accent.ORANGE,
                List.of(ScopePlaygroundView.class));
    }
}
