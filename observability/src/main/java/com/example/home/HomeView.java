package com.example.home;

import com.example.common.BaseHomeView;
import com.example.views.MainLayout;

import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Observability Use Cases")
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Observability — use cases",
                "Each card below exercises one observability use case of a Vaadin Flow application.");
        addMenuCards();
    }
}
