package com.example.home;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.example.uc1.ShareThisPageView;
import com.example.uc2.CopyLinkFallbackView;
import com.example.uc3.CustomMessageView;
import com.example.uc4.ShareListItemsView;
import com.example.uc5.ShareFeedbackView;
import com.example.uc6.ShareInviteLinkView;
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
@ViewPackages(classes = { HomeView.class, ShareThisPageView.class,
        CopyLinkFallbackView.class, CustomMessageView.class,
        ShareListItemsView.class, ShareFeedbackView.class,
        ShareInviteLinkView.class })
class HomeViewTest extends SpringBrowserlessTest {

    private record UseCase(String tag, String title, String problem) {
    }

    private record Group(String heading, List<UseCase> useCases) {
    }

    private static final UseCase UC1 = new UseCase("UC1", "Share this page",
            "Sharing the current page through the native share sheet");
    private static final UseCase UC2 = new UseCase("UC2", "Copy-link fallback",
            "Falling back to copy-link where sharing is unsupported");
    private static final UseCase UC3 = new UseCase("UC3",
            "Share a custom message",
            "Sharing a title, text and link the user fills in");
    private static final UseCase UC4 = new UseCase("UC4", "Per-item share",
            "Sharing one item from a list");
    private static final UseCase UC5 = new UseCase("UC5", "Completion feedback",
            "Reacting to a completed, cancelled or failed share");
    private static final UseCase UC6 = new UseCase("UC6", "Share invite link",
            "Sharing a freshly generated invite link");

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

        assertEquals(List.of(
                new Group("Sharing what's on screen", List.of(UC1, UC4)),
                new Group("Sharing content the app builds", List.of(UC3, UC6)),
                new Group("Fallbacks and feedback", List.of(UC2, UC5))),
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
