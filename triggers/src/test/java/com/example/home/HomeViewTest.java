package com.example.home;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.sidenav.SideNavItem;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = { HomeView.class, ShortcutSaveView.class,
        SubmitAndDisableView.class, LiveSignalCounterView.class,
        JsTriggerView.class, IdleWarningView.class, NetworkStatusView.class,
        CrossTabBroadcastView.class, LongPressDeleteView.class,
        ScrollIntoViewView.class, HighlightView.class,
        RightClickCoordsView.class, AccessibleSaveView.class,
        ClientFilterView.class, ResponsiveCardsView.class,
        LiveSizeReadoutView.class, PointerTrackerView.class,
        AtomicResetView.class, AutoSaveSignalView.class,
        DynamicResponsiveStylingView.class, DoubleClickOpenView.class,
        ShortcutDownloadView.class, KeyEventLogView.class,
        KonamiCodeView.class })
class HomeViewTest extends SpringBrowserlessTest {

    private record UseCase(String tag, String title, String problem) {
    }

    private record Group(String heading, List<UseCase> useCases) {
    }

    private static final UseCase UC1 = new UseCase("UC1", "Ctrl+S snapshot",
            "Copying the notes to the clipboard with Ctrl+S");
    private static final UseCase UC2 = new UseCase("UC2",
            "Image gallery select",
            "Selecting an image and dimming the others at once");
    private static final UseCase UC3 = new UseCase("UC3", "Hidden share link",
            "Copying a link the page never shows");
    private static final UseCase UC4 = new UseCase("UC4", "Double-click copy",
            "Copying text on a double-click");
    private static final UseCase UC5 = new UseCase("UC5", "Idle warning",
            "Warning users who have gone idle");
    private static final UseCase UC6 = new UseCase("UC6", "Network status",
            "Showing when the browser goes offline");
    private static final UseCase UC7 = new UseCase("UC7", "Cross-tab broadcast",
            "Passing messages between open browser tabs");
    private static final UseCase UC8 = new UseCase("UC8",
            "Long-press to delete", "Guarding a delete behind a long press");
    private static final UseCase UC9 = new UseCase("UC9", "Scroll into view",
            "Scrolling to a section without a server round-trip");
    private static final UseCase UC10 = new UseCase("UC10", "Highlight",
            "Reusing one highlight effect with different settings");
    private static final UseCase UC11 = new UseCase("UC11",
            "Right-click coords",
            "Getting right-click coordinates on the server");
    private static final UseCase UC12 = new UseCase("UC12",
            "Accessibility announce",
            "Announcing a save to screen-reader users");
    private static final UseCase UC13 = new UseCase("UC13",
            "Client-side filter",
            "Filtering a list without a request per keystroke");
    private static final UseCase UC14 = new UseCase("UC14", "Responsive cards",
            "Fitting card columns to the container width");
    private static final UseCase UC15 = new UseCase("UC15", "Live size readout",
            "Showing an element's size as it resizes");
    private static final UseCase UC16 = new UseCase("UC16", "Pointer tracker",
            "Following the pointer without flooding the server");
    private static final UseCase UC17 = new UseCase("UC17", "Atomic reset",
            "Resetting several fields at once in the browser");
    private static final UseCase UC18 = new UseCase("UC18", "Auto-save signal",
            "Saving text into a signal on every keystroke");
    private static final UseCase UC19 = new UseCase("UC19",
            "Dynamic responsive styling",
            "Applying user-chosen colors per breakpoint");
    private static final UseCase UC20 = new UseCase("UC20",
            "Double-click → new tab",
            "Opening a row in a new tab on double-click");
    private static final UseCase UC21 = new UseCase("UC21", "Shortcut download",
            "Starting a file download from a keyboard shortcut");
    private static final UseCase UC22 = new UseCase("UC22", "Key event log",
            "Showing which keys and modifiers were pressed");
    private static final UseCase UC23 = new UseCase("UC23", "Konami code",
            "Reacting only to a complete key sequence");

    private static final List<UseCase> USE_CASES = List.of(UC1, UC2, UC3, UC4,
            UC5, UC6, UC7, UC8, UC9, UC10, UC11, UC12, UC13, UC14, UC15, UC16,
            UC17, UC18, UC19, UC20, UC21, UC22, UC23);

    @Test
    void cardsAreGroupedByKindOfProblemInMenuOrder() {
        HomeView home = navigate(HomeView.class);

        List<Group> groups = home.getChildren()
                .filter(Section.class::isInstance)
                .map(section -> new Group(
                        find(H2.class).from(section).single().getText(),
                        find(Card.class).from(section).all().stream()
                                .map(HomeViewTest::useCaseOf).toList()))
                .toList();

        assertEquals(List.of(
                new Group("Browser APIs that need a gesture",
                        List.of(UC1, UC3, UC4, UC20, UC21)),
                new Group("Instant feedback in the browser",
                        List.of(UC2, UC9, UC10, UC12, UC13, UC17)),
                new Group("Custom gestures and live input",
                        List.of(UC8, UC11, UC16, UC18, UC22, UC23)),
                new Group("Adapting to the available size",
                        List.of(UC14, UC15, UC19)),
                new Group("Watching the browser's state",
                        List.of(UC5, UC6, UC7))),
                groups);
    }

    private static UseCase useCaseOf(Card card) {
        return new UseCase(card.getHeaderPrefix().getElement().getText(),
                card.getTitleAsText(),
                card.getChildren().filter(Paragraph.class::isInstance)
                        .map(p -> ((Paragraph) p).getText()).findFirst()
                        .orElse(null));
    }

    @Test
    void navItemsKeepTheShortNameAndShowTheProblemAsTooltip() {
        navigate(HomeView.class);

        Map<String, SideNavItem> items = find(SideNavItem.class).all().stream()
                .collect(Collectors.toMap(SideNavItem::getLabel,
                        Function.identity()));

        USE_CASES.forEach(useCase -> assertEquals(useCase.problem(),
                items.get(useCase.tag() + " — " + useCase.title()).getTooltip()
                        .getText(),
                "the nav tooltip and the home card read the same text"));
    }
}
