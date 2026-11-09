package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.common.UseCaseDescription;
import com.example.uc1.ShortcutSaveView;
import com.example.uc10.HighlightView;
import com.example.uc11.RightClickCoordsView;
import com.example.uc12.AccessibleSaveView;
import com.example.uc13.ClientFilterView;
import com.example.uc14.ResponsiveCardsView;
import com.example.uc15.LiveSizeReadoutView;
import com.example.uc16.PointerTrackerView;
import com.example.uc17.AtomicResetView;
import com.example.uc18.AutoSaveSignalView;
import com.example.uc19.DynamicResponsiveStylingView;
import com.example.uc2.ImageGalleryView;
import com.example.uc20.DoubleClickOpenView;
import com.example.uc21.ShortcutDownloadView;
import com.example.uc22.KeyEventLogView;
import com.example.uc23.KonamiCodeView;
import com.example.uc24.DisableButtonGroupView;
import com.example.uc3.HiddenShareLinkView;
import com.example.uc4.DoubleClickCopyView;
import com.example.uc5.IdleWarningView;
import com.example.uc6.NetworkStatusView;
import com.example.uc7.CrossTabBroadcastView;
import com.example.uc8.LongPressDeleteView;
import com.example.uc9.ScrollIntoViewView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@UseCaseDescription("Running actions in the browser on clicks, shortcuts and other events without a server round-trip")
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Trigger / Action API — use cases",
                "Each card below is one use case of Flow's trigger and "
                        + "action API. A trigger fires in the browser on a "
                        + "click, a keyboard shortcut, a resize, an idle "
                        + "timeout or a message from another tab. It reads "
                        + "inputs and runs actions inside the same browser "
                        + "event, without a server round-trip. Many use cases "
                        + "build their own triggers, actions and inputs.");
        addGroup("Browser APIs that need a gesture", VaadinIcon.HAND,
                Accent.BLUE,
                List.of(ShortcutSaveView.class, HiddenShareLinkView.class,
                        DoubleClickCopyView.class, DoubleClickOpenView.class,
                        ShortcutDownloadView.class));
        addGroup("Instant feedback in the browser", VaadinIcon.BOLT,
                Accent.ORANGE,
                List.of(ImageGalleryView.class, ScrollIntoViewView.class,
                        HighlightView.class, AccessibleSaveView.class,
                        ClientFilterView.class, AtomicResetView.class,
                        DisableButtonGroupView.class));
        addGroup("Custom gestures and live input", VaadinIcon.KEYBOARD_O,
                Accent.PURPLE,
                List.of(LongPressDeleteView.class, RightClickCoordsView.class,
                        PointerTrackerView.class, AutoSaveSignalView.class,
                        KeyEventLogView.class, KonamiCodeView.class));
        addGroup("Adapting to the available size", VaadinIcon.RESIZE_H,
                Accent.YELLOW,
                List.of(ResponsiveCardsView.class, LiveSizeReadoutView.class,
                        DynamicResponsiveStylingView.class));
        addGroup("Watching the browser's state", VaadinIcon.BROWSER,
                Accent.GREEN, List.of(IdleWarningView.class,
                        NetworkStatusView.class, CrossTabBroadcastView.class));
    }
}
