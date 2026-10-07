package com.example.home;

import com.example.common.BaseHomeView;
import com.example.uc1.ManualToggleView;
import com.example.uc2.RecipeView;
import com.example.uc3.SlideshowView;
import com.example.uc4.WorkoutTimerView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Screen Wake Lock API — use cases",
                "Each card below exercises one use case of the WakeLock API. "
                        + "WakeLock#request() asks the browser to keep the screen "
                        + "awake; WakeLock#activeSignal() reflects whether the "
                        + "browser is currently holding the lock. The client "
                        + "transparently re-acquires the lock when the tab "
                        + "becomes visible again, so a single request() covers "
                        + "the lifetime of a view.");

        Div cards = new Div();
        cards.addClassName("home-cards");
        cards.add(homeCard("UC1", "Manual keep-awake toggle",
                "Toggle the lock on demand and watch the signal flip.",
                ManualToggleView.class));
        cards.add(homeCard("UC2", "Recipe — lifetime of view",
                "Lock acquired on attach, released on detach.",
                RecipeView.class));
        cards.add(homeCard("UC3", "Presentation slideshow",
                "Lock held only while a presentation is running.",
                SlideshowView.class));
        cards.add(homeCard("UC4", "Workout interval timer",
                "Lock follows an internal running-state signal.",
                WorkoutTimerView.class));
        add(cards);
    }
}
