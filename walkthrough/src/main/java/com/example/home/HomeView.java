package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.common.UseCaseDescription;
import com.example.uc1.FirstRunTourView;
import com.example.uc2.ShowOnceView;
import com.example.uc3.ReplayFromHelpView;
import com.example.uc4.WhatsNewView;
import com.example.uc5.ComplexViewTourView;
import com.example.uc6.LearnByDoingView;
import com.example.uc7.CrossViewTourView;
import com.example.uc8.HotspotsView;
import com.example.uc9.SpotlightView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@UseCaseDescription("Introducing the parts of the UI to the user, step by step")
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Walkthroughs & product tours — use cases",
                "A walkthrough points at the parts of the UI one at a time "
                        + "and says what each is for: when the app opens for "
                        + "the first time, after an update, or when a view "
                        + "needs explaining. Vaadin has no tour component, so "
                        + "these use cases build one from Popover.");
        addGroup("Welcoming a new user", VaadinIcon.HANDS_UP, Accent.BLUE,
                List.of(FirstRunTourView.class, ShowOnceView.class,
                        ReplayFromHelpView.class));
        addGroup("Showing what changed", VaadinIcon.STAR_O, Accent.GREEN,
                List.of(WhatsNewView.class, HotspotsView.class));
        addGroup("Explaining a view or a task", VaadinIcon.LIGHTBULB,
                Accent.ORANGE,
                List.of(ComplexViewTourView.class, LearnByDoingView.class,
                        CrossViewTourView.class, SpotlightView.class));
    }
}
