package com.example.home;

import com.example.common.BaseHomeView;
import com.example.uc1.ImageLightboxView;
import com.example.uc2.SlideshowView;
import com.example.uc3.DistractionFreeEditorView;
import com.example.uc4.ReactiveLayoutView;
import com.example.uc5.KioskExitDetectionView;
import com.example.uc6.ChartExpandView;
import com.example.uc7.AppFullscreenView;
import com.example.uc8.OverlaysFullscreenView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Fullscreen API — use cases",
                "Each card below exercises one scenario of the new Fullscreen "
                        + "API. Component#requestFullscreen() wraps a single "
                        + "component (overlays and theming keep working), "
                        + "Page#requestFullscreen() takes the entire document "
                        + "fullscreen, and Page#fullscreenSignal() reactively "
                        + "reports FULLSCREEN, NOT_FULLSCREEN, UNSUPPORTED or "
                        + "UNKNOWN. Browsers require a real user click to "
                        + "enter fullscreen.");

        Div cards = new Div();
        cards.addClassName("home-cards");
        cards.add(homeCard("UC1", "Image lightbox",
                "Click a thumbnail to fullscreen the preview pane; Escape "
                        + "returns to the gallery.",
                ImageLightboxView.class));
        cards.add(homeCard("UC2", "Slideshow / presentation",
                "Component#requestFullscreen() fullscreens just the slide; "
                        + "arrows navigate, Escape exits.",
                SlideshowView.class));
        cards.add(homeCard("UC3", "Distraction-free editor",
                "Expand a text area to fullscreen for focused writing.",
                DistractionFreeEditorView.class));
        cards.add(homeCard("UC4", "Reactive layout",
                "fullscreenSignal() drives density and visible widgets.",
                ReactiveLayoutView.class));
        cards.add(homeCard("UC5", "Kiosk: visitor sign-in",
                "Component#requestFullscreen() locks just the kiosk UI; "
                        + "visitors sign in, staff exits with a PIN, Escape "
                        + "is flagged.",
                KioskExitDetectionView.class));
        cards.add(homeCard("UC6", "Chart expand",
                "Per-card expand buttons fullscreen a single Vaadin Chart.",
                ChartExpandView.class));
        cards.add(homeCard("UC7", "View app fullscreen",
                "Page#requestFullscreen() hides browser chrome and shows "
                        + "the whole app — kiosk-style.",
                AppFullscreenView.class));
        cards.add(homeCard("UC8", "Overlays in fullscreen",
                "Menus, popovers, dropdowns, context menus and tooltips keep "
                        + "opening on top of a fullscreened component.",
                OverlaysFullscreenView.class));
        add(cards);
    }
}
