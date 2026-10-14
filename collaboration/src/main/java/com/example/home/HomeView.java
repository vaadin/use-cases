package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.common.UseCaseDescription;
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

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Collaboration Use Cases")
@UseCaseDescription("Letting several users see and edit the same data at the same time, with shared signals only")
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Collaboration — use cases",
                "The Collaboration Engine Sampler rebuilt with shared signals "
                        + "only, without Collaboration Kit. UC1–UC9 match the "
                        + "sampler's nine samples. UC10–UC13 show what "
                        + "Collaboration Kit can not do. Every view shows "
                        + "several simulated users side by side, so you do not "
                        + "need a second browser tab. Open one anyway to see "
                        + "that the state is really shared.");
        addGroup("See who else is here", VaadinIcon.USERS, Accent.BLUE,
                List.of(PresenceView.class, OptInPresenceView.class,
                        CustomUserListView.class));
        addGroup("Chat and edit a form together", VaadinIcon.COMMENTS_O,
                Accent.GREEN,
                List.of(ChatView.class, CollaborativeFormView.class,
                        FormEventsView.class, FormAccessView.class));
        addGroup("Edit a grid together", VaadinIcon.TABLE, Accent.PURPLE,
                List.of(CollaborativeGridView.class,
                        CollaborativeGridProView.class));
        addGroup("Keep shared data correct", VaadinIcon.CHECK_CIRCLE_O,
                Accent.ORANGE,
                List.of(ConflictsView.class, ComputedStateView.class,
                        RoomsView.class, PendingStateView.class));
    }
}
