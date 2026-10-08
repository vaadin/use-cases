package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.uc1.PrintCurrentViewView;
import com.example.uc2.PrintRouteView;
import com.example.uc3.PrintableListView;
import com.example.uc4.PrintPreviewView;
import com.example.uc5.HeaderFooterView;
import com.example.uc6.PrintDashboardView;
import com.example.uc7.ExpandForPrintView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.icon.VaadinIcon;
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
        addGroup("Starting a print", VaadinIcon.PRINT, Accent.BLUE,
                List.of(PrintCurrentViewView.class, PrintRouteView.class));
        addGroup("Components that don't print", VaadinIcon.EYE_SLASH,
                Accent.ORANGE, List.of(PrintableListView.class,
                        PrintDashboardView.class, ExpandForPrintView.class));
        addGroup("Laying out the page", VaadinIcon.FILE_TEXT_O, Accent.GREEN,
                List.of(PrintPreviewView.class, HeaderFooterView.class));
    }
}
