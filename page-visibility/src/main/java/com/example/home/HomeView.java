package com.example.home;

import com.example.common.BaseHomeView;
import com.example.common.UseCaseDescription;
import com.example.uc1.UpdateWhenActiveView;
import com.example.uc2.PresenceAvatarsView;
import com.example.uc3.NotificationGatingView;
import com.example.uc4.RefreshStaleDataView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@UseCaseDescription("Reacting on the server when the user's tab is hidden, visible or focused")
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Page Visibility API — use cases",
                "Each card below exercises one use case of Page#pageVisibilitySignal(). "
                        + "The signal reports VISIBLE, VISIBLE_NOT_FOCUSED, HIDDEN or UNKNOWN "
                        + "and reactively notifies the server whenever the user's tab "
                        + "visibility or focus changes.");

        Div cards = new Div();
        cards.addClassName("home-cards");
        cards.add(homeCard("UC1", "Update when active",
                "Pause server work while the tab is hidden.",
                UpdateWhenActiveView.class));
        cards.add(homeCard("UC2", "Presence avatars",
                "\"Away\" status broadcast across browsers.",
                PresenceAvatarsView.class));
        cards.add(homeCard("UC3", "Notification gating",
                "Switch between in-tab toast and Web Push delivery.",
                NotificationGatingView.class));
        cards.add(homeCard("UC4", "Refresh stale data",
                "Re-fetch automatically when the user returns.",
                RefreshStaleDataView.class));
        add(cards);
    }
}
