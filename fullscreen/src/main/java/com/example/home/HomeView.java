package com.example.home;

import java.util.List;

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

import com.vaadin.flow.component.icon.VaadinIcon;
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
        addGroup("Showing one thing big", VaadinIcon.EXPAND_SQUARE, Accent.BLUE,
                List.of(ImageLightboxView.class, SlideshowView.class,
                        DistractionFreeEditorView.class,
                        ChartExpandView.class));
        addGroup("Taking over the screen", VaadinIcon.DESKTOP, Accent.PURPLE,
                List.of(KioskExitDetectionView.class, AppFullscreenView.class));
        addGroup("Staying usable in fullscreen", VaadinIcon.SLIDERS,
                Accent.GREEN, List.of(ReactiveLayoutView.class,
                        OverlaysFullscreenView.class));
    }
}
