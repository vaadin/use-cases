package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.common.UseCaseDescription;
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
@UseCaseDescription("Taking a component or the whole app fullscreen and reacting when it changes")
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Fullscreen API — use cases",
                "Each card below shows one scenario of the Fullscreen API. "
                        + "Fullscreen.onClick(button).enter(component) shows "
                        + "a single component fullscreen (overlays and "
                        + "theming keep working), "
                        + "Fullscreen.onClick(button).enter() takes the whole "
                        + "page fullscreen, Fullscreen.exit() leaves it, and "
                        + "Fullscreen.stateSignal() reports FULLSCREEN, "
                        + "NOT_FULLSCREEN or UNSUPPORTED. Browsers only "
                        + "enter fullscreen on a real user click, so the "
                        + "request is bound to the button's click.");
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
