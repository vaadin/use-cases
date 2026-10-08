package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.uc1.UpdateWhenActiveView;
import com.example.uc2.PresenceAvatarsView;
import com.example.uc3.NotificationGatingView;
import com.example.uc4.RefreshStaleDataView;
import com.example.views.MainLayout;

import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Page Visibility API — use cases",
                "Page#pageVisibilitySignal() reports VISIBLE, VISIBLE_NOT_FOCUSED, "
                        + "HIDDEN or UNKNOWN and notifies the server whenever the "
                        + "visibility or focus of the user's tab changes.");
        addMenuCards(List.of(UpdateWhenActiveView.class,
                PresenceAvatarsView.class, NotificationGatingView.class,
                RefreshStaleDataView.class));
    }
}
