package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.common.UseCaseDescription;
import com.example.uc1.AdaptiveLayoutView;
import com.example.uc2.OrientationViewerView;
import com.example.uc3.RotatePromptView;
import com.example.uc4.LockForVideoView;
import com.example.uc5.LockErrorView;
import com.example.views.MainLayout;

import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@UseCaseDescription("Adapting to and locking the device's screen orientation")
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Screen Orientation API — use cases",
                "ScreenOrientation.orientationSignal() carries the current "
                        + "orientation type (portrait-primary, landscape-primary, "
                        + "...) and rotation angle, and updates whenever the "
                        + "device rotates. ScreenOrientation.lock(...) depends on "
                        + "the device and usually requires fullscreen — see UC4 "
                        + "and UC5.");
        addMenuCards(List.of(AdaptiveLayoutView.class,
                OrientationViewerView.class, RotatePromptView.class,
                LockForVideoView.class, LockErrorView.class));
    }
}
