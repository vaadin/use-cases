package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.uc1.ManualToggleView;
import com.example.uc2.RecipeView;
import com.example.uc3.SlideshowView;
import com.example.uc4.WorkoutTimerView;
import com.example.views.MainLayout;

import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Screen Wake Lock API — use cases",
                "WakeLock#request() asks the browser to keep the screen awake; "
                        + "WakeLock#activeSignal() reflects whether the browser currently "
                        + "holds the lock. The client re-acquires the lock when the tab "
                        + "becomes visible again, so a single request() covers the "
                        + "lifetime of a view.");
        addMenuCards(List.of(ManualToggleView.class, RecipeView.class,
                SlideshowView.class, WorkoutTimerView.class));
    }
}
