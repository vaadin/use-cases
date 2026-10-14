package com.example.home;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.example.uc1.PermissionStateView;
import com.example.uc2.LongTaskView;
import com.example.uc3.ClickToOpenView;
import com.example.uc4.TaggedCounterView;
import com.example.uc5.HiddenTabOnlyView;
import com.example.uc6.DeniedFallbackView;
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
@ViewPackages(classes = { HomeView.class, PermissionStateView.class,
        LongTaskView.class, ClickToOpenView.class, TaggedCounterView.class,
        HiddenTabOnlyView.class, DeniedFallbackView.class })
class HomeViewTest extends SpringBrowserlessTest {

    private record UseCase(String tag, String title, String problem) {
    }

    private record Group(String heading, List<UseCase> useCases) {
    }

    private static final UseCase UC1 = new UseCase("UC1", "Ask for permission",
            "Asking for notification permission on a click and showing its state live");
    private static final UseCase UC2 = new UseCase("UC2", "Task finished",
            "Telling the user that a slow server job has finished while they work in another tab");
    private static final UseCase UC3 = new UseCase("UC3", "Click to open",
            "Opening the related item when the user clicks a notification");
    private static final UseCase UC4 = new UseCase("UC4",
            "Collapse repeated alerts",
            "Replacing one notification instead of stacking a new one for every message");
    private static final UseCase UC5 = new UseCase("UC5", "Only when hidden",
            "Notifying through the OS only when the user isn't looking at the tab");
    private static final UseCase UC6 = new UseCase("UC6", "Denied permission",
            "Explaining how to re-enable blocked notifications and falling back to in-app messages");

    private static final List<UseCase> USE_CASES = List.of(UC1, UC2, UC3, UC4,
            UC5, UC6);

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

        assertEquals(List.of(new Group("Getting permission", List.of(UC1, UC6)),
                new Group("Notifying at the right moment", List.of(UC2, UC5)),
                new Group("Clicking and replacing notifications",
                        List.of(UC3, UC4))),
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
