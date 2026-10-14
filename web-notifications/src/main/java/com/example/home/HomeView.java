package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.common.UseCaseDescription;
import com.example.uc1.PermissionStateView;
import com.example.uc2.LongTaskView;
import com.example.uc3.ClickToOpenView;
import com.example.uc4.TaggedCounterView;
import com.example.uc5.HiddenTabOnlyView;
import com.example.uc6.DeniedFallbackView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@UseCaseDescription("Showing native OS notifications from an open tab and reacting when the user clicks them")
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Web Notifications API — use cases",
                "Each card below shows native OS notifications from a Flow "
                        + "app while its tab is open, using the browser's "
                        + "Notification API. Flow has no API for it yet, so "
                        + "the views use a small shim (MissingAPI) that asks "
                        + "for permission inside the click, exposes the "
                        + "permission as a signal and reports notification "
                        + "clicks back to the server.");
        addGroup("Getting permission", VaadinIcon.KEY, Accent.BLUE,
                List.of(PermissionStateView.class, DeniedFallbackView.class));
        addGroup("Notifying at the right moment", VaadinIcon.BELL,
                Accent.PURPLE,
                List.of(LongTaskView.class, HiddenTabOnlyView.class));
        addGroup("Clicking and replacing notifications", VaadinIcon.CURSOR,
                Accent.GREEN,
                List.of(ClickToOpenView.class, TaggedCounterView.class));
    }
}
