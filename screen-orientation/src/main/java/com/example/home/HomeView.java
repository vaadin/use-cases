package com.example.home;

import com.example.common.BaseHomeView;
import com.example.common.UseCaseDescription;
import com.example.uc1.AdaptiveLayoutView;
import com.example.uc2.OrientationViewerView;
import com.example.uc3.RotatePromptView;
import com.example.uc4.LockForVideoView;
import com.example.uc5.LockErrorView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@UseCaseDescription("Adapting to and locking the device's screen orientation")
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Screen Orientation API — use cases",
                "Each card below exercises one use case of Page#screenOrientationSignal() "
                        + "and Page#lockOrientation(...). The signal carries the current "
                        + "ScreenOrientation type (portrait-primary, landscape-primary, ...) "
                        + "and rotation angle, and notifies the server whenever the browser "
                        + "reports a change. Lock requests are subject to platform support and "
                        + "usually require fullscreen — see UC4 and UC5.");

        Div cards = new Div();
        cards.addClassName("home-cards");
        cards.add(homeCard("UC1", "Adaptive layout",
                "Side-by-side in landscape, stacked in portrait.",
                AdaptiveLayoutView.class));
        cards.add(homeCard("UC2", "Orientation viewer",
                "Live type, angle and support state, with a rotating arrow.",
                OrientationViewerView.class));
        cards.add(homeCard("UC3", "Rotate-your-device overlay",
                "Block the view until the user rotates to the required orientation.",
                RotatePromptView.class));
        cards.add(homeCard("UC4", "Lock landscape for video",
                "Enter fullscreen + lock landscape while playing media.",
                LockForVideoView.class));
        cards.add(homeCard("UC5", "Lock error UX",
                "Display SecurityError / NotSupportedError / AbortError reactively.",
                LockErrorView.class));
        add(cards);
    }
}
