package com.example.home;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.example.uc1.ImageLightboxView;
import com.example.uc2.SlideshowView;
import com.example.uc3.DistractionFreeEditorView;
import com.example.uc4.ReactiveLayoutView;
import com.example.uc5.KioskExitDetectionView;
import com.example.uc6.ChartExpandView;
import com.example.uc7.AppFullscreenView;
import com.example.uc8.OverlaysFullscreenView;
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
@ViewPackages(classes = { HomeView.class, ImageLightboxView.class,
        SlideshowView.class, DistractionFreeEditorView.class,
        ReactiveLayoutView.class, KioskExitDetectionView.class,
        ChartExpandView.class, AppFullscreenView.class,
        OverlaysFullscreenView.class })
class HomeViewTest extends SpringBrowserlessTest {

    private record UseCase(String tag, String title, String problem) {
    }

    private record Group(String heading, List<UseCase> useCases) {
    }

    private static final UseCase UC1 = new UseCase("UC1", "Image lightbox",
            "Showing a clicked image fullscreen");
    private static final UseCase UC2 = new UseCase("UC2", "Slideshow",
            "Presenting slides fullscreen without the app around them");
    private static final UseCase UC3 = new UseCase("UC3",
            "Distraction-free editor",
            "Giving a writer the whole screen to write in");
    private static final UseCase UC4 = new UseCase("UC4", "Reactive layout",
            "Adapting the layout when the user goes fullscreen");
    private static final UseCase UC5 = new UseCase("UC5", "Kiosk",
            "Locking a kiosk screen and catching unexpected exits");
    private static final UseCase UC6 = new UseCase("UC6", "Chart expand",
            "Expanding one chart of a dashboard to fullscreen");
    private static final UseCase UC7 = new UseCase("UC7", "View app fullscreen",
            "Showing the whole app without the browser chrome");
    private static final UseCase UC8 = new UseCase("UC8",
            "Overlays in fullscreen",
            "Keeping menus and popups working in fullscreen");

    private static final List<UseCase> USE_CASES = List.of(UC1, UC2, UC3, UC4,
            UC5, UC6, UC7, UC8);

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
                new Group("Showing one thing big", List.of(UC1, UC2, UC3, UC6)),
                new Group("Taking over the screen", List.of(UC5, UC7)),
                new Group("Staying usable in fullscreen", List.of(UC4, UC8))),
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
