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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Collaboration Use Cases")
@UseCaseDescription("Letting several users see and edit the same data at the same time, with shared signals only")
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    /**
     * The use cases grouped by the kind of problem they are about. The side
     * navigation keeps the UC1 to UC13 order; within a group the cards follow
     * it too.
     */
    private record Group(String heading,
            List<Class<? extends Component>> views) {
    }

    private static final List<Group> GROUPS = List.of(
            new Group("See who else is here",
                    List.of(PresenceView.class, OptInPresenceView.class,
                            CustomUserListView.class)),
            new Group("Chat and edit a form together",
                    List.of(ChatView.class, CollaborativeFormView.class,
                            FormEventsView.class, FormAccessView.class)),
            new Group("Edit a grid together",
                    List.of(CollaborativeGridView.class,
                            CollaborativeGridProView.class)),
            new Group("Keep shared data correct",
                    List.of(ConflictsView.class, ComputedStateView.class,
                            RoomsView.class, PendingStateView.class)));

    public HomeView() {
        super("Collaboration — use cases",
                "The Collaboration Engine Sampler rebuilt with shared signals "
                        + "only, without Collaboration Kit. UC1–UC9 match the "
                        + "sampler's nine samples. UC10–UC13 show what "
                        + "Collaboration Kit can not do. Every view shows "
                        + "several simulated users side by side, so you do not "
                        + "need a second browser tab. Open one anyway to see "
                        + "that the state is really shared.");
        GROUPS.forEach(group -> add(new H2(group.heading()),
                menuCards(group.views())));
    }
}
