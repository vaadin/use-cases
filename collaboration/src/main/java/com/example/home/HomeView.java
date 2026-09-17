package com.example.home;

import com.example.common.BaseHomeView;
import com.example.views.MainLayout;

import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Collaboration Use Cases")
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Collaboration — use cases",
                "The Collaboration Engine Sampler rebuilt on shared signals only, with no Collaboration Kit dependency. "
                        + "UC1–UC9 mirror the sampler's nine samples one for one; UC10–UC13 cover collaboration that "
                        + "Collaboration Kit cannot express. Every view runs several simulated users side by side, so a "
                        + "second browser tab is a bonus rather than a requirement — although opening one shows that the "
                        + "state really is shared.");
        addMenuLinkList();
    }
}
