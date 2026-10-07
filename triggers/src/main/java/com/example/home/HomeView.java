package com.example.home;

import com.example.common.BaseHomeView;
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
import com.example.uc2.SubmitAndDisableView;
import com.example.uc20.DoubleClickOpenView;
import com.example.uc21.ShortcutDownloadView;
import com.example.uc22.KeyEventLogView;
import com.example.uc23.KonamiCodeView;
import com.example.uc3.LiveSignalCounterView;
import com.example.uc4.JsTriggerView;
import com.example.uc5.IdleWarningView;
import com.example.uc6.NetworkStatusView;
import com.example.uc7.CrossTabBroadcastView;
import com.example.uc8.LongPressDeleteView;
import com.example.uc9.ScrollIntoViewView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Trigger / Action API — use cases",
                "Each card below exercises one use case of the "
                        + "com.vaadin.flow.component.trigger.internal API. A "
                        + "Trigger fires on the client (a click, a keyboard "
                        + "shortcut, a resize, an idle timeout, a cross-tab "
                        + "broadcast, …), reads zero or more Inputs, and runs "
                        + "one or more Actions — all inside the original "
                        + "browser handler. The set focuses on the SPI itself "
                        + "(custom Triggers, Actions, and Inputs against the "
                        + "public abstract classes); basic clipboard or "
                        + "fullscreen demos belong in the sibling clipboard/ "
                        + "and fullscreen/ modules.");

        Div cards = new Div();
        cards.addClassName("home-cards");
        cards.add(homeCard("UC1", "Ctrl+S snapshot",
                "ShortcutTrigger fires WriteToClipboardAction + a badge flip atomically in the user gesture — @Shortcut would have to round-trip.",
                ShortcutSaveView.class));
        cards.add(homeCard("UC2", "Click image, dim siblings",
                "One ClickTrigger fans three SetPropertyAction calls onto three non-HasEnabled <img> tiles in a single event.",
                SubmitAndDisableView.class));
        cards.add(homeCard("UC3", "Hidden share link",
                "SignalInput reads a ValueSignal that's bound to nothing; WriteToClipboardAction copies it inside the gesture.",
                LiveSignalCounterView.class));
        cards.add(homeCard("UC4", "Double-click copy",
                "Built-in DoubleClickTrigger — the high-level Clipboard API is click-only.",
                JsTriggerView.class));
        cards.add(homeCard("UC5", "Idle warning",
                "Custom IdleTrigger fires after 5s of no activity; SetSignalAction toggles a state badge.",
                IdleWarningView.class));
        cards.add(homeCard("UC6", "Network status",
                "Custom NetworkStatusTrigger + client-side action that reads navigator.onLine.",
                NetworkStatusView.class));
        cards.add(homeCard("UC7", "Cross-tab broadcast",
                "Custom BroadcastChannelTrigger forwards messages from other tabs into a signal.",
                CrossTabBroadcastView.class));
        cards.add(homeCard("UC8", "Long-press to delete",
                "Custom LongPressTrigger composes pointer events with a timer.",
                LongPressDeleteView.class));
        cards.add(homeCard("UC9", "Scroll into view",
                "Smallest custom Action — captures a target, calls scrollIntoView.",
                ScrollIntoViewView.class));
        cards.add(homeCard("UC10", "Highlight",
                "Custom Action with constructor-time colour and duration as JS captures.",
                HighlightView.class));
        cards.add(homeCard("UC11", "Right-click coords",
                "Custom PointInput returns {x,y}; Jackson decodes into a Point record server-side.",
                RightClickCoordsView.class));
        cards.add(homeCard("UC12", "Accessibility announce",
                "Custom AnnounceAction writes into an aria-live region for screen readers.",
                AccessibleSaveView.class));
        cards.add(homeCard("UC13", "Client-side filter",
                "Server renders the full list once; a custom action filters rows in JS.",
                ClientFilterView.class));
        cards.add(homeCard("UC14", "Responsive cards",
                "SizeTrigger + custom ClassByWidthAction swap a card grid between 1, 2, 3 columns.",
                ResponsiveCardsView.class));
        cards.add(homeCard("UC15", "Live size readout",
                "SizeTrigger.EventData.width/height feed SetPropertyAction directly — no callback.",
                LiveSizeReadoutView.class));
        cards.add(homeCard("UC16", "Pointer tracker",
                "MouseEventTrigger(pointermove) + SetPropertyAction with clientX/Y built-in inputs.",
                PointerTrackerView.class));
        cards.add(homeCard("UC17", "Atomic reset",
                "One ClickTrigger fans out to three SetPropertyActions on different targets.",
                AtomicResetView.class));
        cards.add(homeCard("UC18", "Auto-save signal",
                "DomEventTrigger(input) + SetSignalAction routes typing into a ValueSignal.",
                AutoSaveSignalView.class));
        cards.add(homeCard("UC19", "Dynamic responsive styling",
                "Three colour pickers + SizeTrigger; one Action shared across four triggers.",
                DynamicResponsiveStylingView.class));
        cards.add(homeCard("UC20", "Double-click → new tab",
                "DoubleClickTrigger + OpenInNewTabAction — the high-level API is click-only.",
                DoubleClickOpenView.class));
        cards.add(homeCard("UC21", "Shortcut download",
                "Ctrl+Shift+D fires DownloadAction with a server-side DownloadHandler.",
                ShortcutDownloadView.class));
        cards.add(homeCard("UC22", "Key event log",
                "Local KeyboardEventTrigger; SetPropertyAction mirrors event.key/code/modifiers.",
                KeyEventLogView.class));
        cards.add(homeCard("UC23", "Konami code",
                "Local SequenceTrigger fires only on the complete sequence; partial state stays client-side.",
                KonamiCodeView.class));
        add(cards);
    }
}
