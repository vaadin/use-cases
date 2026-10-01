package com.example.home;

import com.example.common.BaseHomeView;
import com.example.uc1.PermissionStateView;
import com.example.uc2.LongTaskView;
import com.example.uc3.ClickToOpenView;
import com.example.uc4.TaggedCounterView;
import com.example.uc5.HiddenTabOnlyView;
import com.example.uc6.DeniedFallbackView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Web Notifications API — use cases",
                "Each card below exercises one use case of the browser's "
                        + "Notification API: native OS notifications shown "
                        + "while the tab is open. Flow has no API for it yet, "
                        + "so the views use a small shim (MissingAPI) that "
                        + "requests permission inside the click gesture, "
                        + "exposes the permission as a signal and reports "
                        + "notification clicks back to the server.");

        Div cards = new Div();
        cards.addClassName("home-cards");
        cards.add(homeCard("UC1", "Ask for permission",
                "Prompt on a click and show the permission state live.",
                PermissionStateView.class));
        cards.add(homeCard("UC2", "Task finished",
                "Notify when a slow server job completes.",
                LongTaskView.class));
        cards.add(homeCard("UC3", "Click to open",
                "Clicking the notification focuses the tab and opens the item.",
                ClickToOpenView.class));
        cards.add(homeCard("UC4", "Collapse with tag",
                "Replace a notification instead of stacking new ones.",
                TaggedCounterView.class));
        cards.add(homeCard("UC5", "Only when hidden",
                "OS notification when away, in-app when looking.",
                HiddenTabOnlyView.class));
        cards.add(homeCard("UC6", "Denied permission",
                "Explain how to re-enable and fall back to in-app.",
                DeniedFallbackView.class));
        add(cards);
    }
}
