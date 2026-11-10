package com.example.home;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.RouterLink;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(packages = "com.example")
class HomeViewTest extends SpringBrowserlessTest {

    private record UseCase(String tag, String title, String problem,
            String href) {
    }

    private static final List<UseCase> USE_CASES = List.of(
            new UseCase("UC1", "First-run tour",
                    "Introducing the main parts of the app when it opens",
                    "uc1"),
            new UseCase("UC2", "Show it only once",
                    "Remembering that a user has seen the tour, per account or per browser",
                    "uc2"),
            new UseCase("UC3", "Replay from help",
                    "Starting the tour again, or resuming it, from the help menu",
                    "uc3"),
            new UseCase("UC4", "What's new",
                    "Pointing only at the features added since the user's last visit",
                    "uc4"),
            new UseCase("UC8", "Hotspots",
                    "Hints next to new features that the user opens when they want",
                    "uc8"),
            new UseCase("UC5", "Tour of a busy view",
                    "A tour of one complex view, including parts hidden in panels and tabs",
                    "uc5"),
            new UseCase("UC6", "Learn by doing",
                    "A tutorial that moves on when the user does what each step says",
                    "uc6"),
            new UseCase("UC7", "Tour across views",
                    "A tour that continues on the next page", "uc7"),
            new UseCase("UC9", "Spotlight",
                    "Dimming all but the target, and revealing targets that are off screen",
                    "uc9"));

    @Test
    void cardsListTheUseCasesByGroup() {
        navigate(HomeView.class);

        assertEquals(
                List.of("Welcoming a new user", "Showing what changed",
                        "Explaining a view or a task"),
                findInView(H2.class).all().stream().map(H2::getText).toList());
        assertEquals(USE_CASES, findInView(Card.class).all().stream()
                .map(HomeViewTest::useCaseOf).toList());
    }

    @Test
    void navListsTheUseCasesInOrderWithTheProblemAsTooltip() {
        // The two other pages of UC7 are reached from UC7, not the menu.
        navigate(HomeView.class);

        assertEquals(
                USE_CASES.stream().sorted((a, b) -> a.tag().compareTo(b.tag()))
                        .map(useCase -> useCase.tag() + " — " + useCase.title()
                                + ": " + useCase.problem())
                        .toList(),
                find(SideNavItem.class).all().stream()
                        .filter(item -> !"Home".equals(item.getLabel()))
                        .map(item -> item.getLabel() + ": "
                                + item.getTooltip().getText())
                        .toList());
    }

    private static UseCase useCaseOf(Card card) {
        return new UseCase(card.getHeaderPrefix().getElement().getText(),
                card.getTitleAsText(),
                card.getChildren().filter(Paragraph.class::isInstance)
                        .map(p -> ((Paragraph) p).getText()).findFirst()
                        .orElse(null),
                Stream.of(card.getFooterComponents())
                        .filter(RouterLink.class::isInstance)
                        .map(link -> ((RouterLink) link).getHref()).findFirst()
                        .orElse(null));
    }
}
