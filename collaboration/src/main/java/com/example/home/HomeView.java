package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.uc1.PresenceView;
import com.example.uc10.ConflictsView;
import com.example.uc11.ComputedStateView;
import com.example.uc12.RoomsView;
import com.example.uc13.PendingStateView;
import com.example.uc2.OptInPresenceView;
import com.example.uc3.CustomUserListView;
import com.example.uc4.ChatView;
import com.example.uc5.CollaborativeFormView;
import com.example.uc6.FormEventsView;
import com.example.uc7.FormAccessView;
import com.example.uc8.CollaborativeGridView;
import com.example.uc9.CollaborativeGridProView;
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
        addMenuCards(List.of(PresenceView.class, OptInPresenceView.class,
                CustomUserListView.class, ChatView.class,
                CollaborativeFormView.class, FormEventsView.class,
                FormAccessView.class, CollaborativeGridView.class,
                CollaborativeGridProView.class, ConflictsView.class,
                ComputedStateView.class, RoomsView.class,
                PendingStateView.class));
    }
}
