package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.common.UseCaseDescription;
import com.example.uc1.AppTourView;
import com.example.uc2.ComplexViewTourView;
import com.example.uc3.LearnByDoingView;
import com.example.uc4.CrossViewTourView;
import com.example.uc5.HotspotsView;
import com.example.uc6.SpotlightView;
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
        addGroup("Introducing the app", VaadinIcon.HANDS_UP, Accent.BLUE,
                List.of(AppTourView.class, HotspotsView.class));
        addGroup("Explaining a view or a task", VaadinIcon.LIGHTBULB,
                Accent.ORANGE,
                List.of(ComplexViewTourView.class, LearnByDoingView.class,
                        CrossViewTourView.class, SpotlightView.class));
    }
}
