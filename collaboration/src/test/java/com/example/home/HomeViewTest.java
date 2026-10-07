package com.example.home;

import java.util.List;
import java.util.stream.Stream;

import com.example.uc1.PresenceView;
import com.example.uc13.PendingStateView;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.RouterLink;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The home page is the module's index, and its cards are generated from the
 * {@code @Menu} and {@code @UseCaseDescription} annotations rather than written
 * out — so this test is really about those: a use case that forgets its
 * annotation disappears from the app without any other test noticing.
 */
@SpringBootTest
@ViewPackages(packages = "com.example")
class HomeViewTest extends SpringBrowserlessTest {

    private record UseCase(String tag, String title, String problem,
            String href) {
    }

    @Test
    void cardsListTheUseCasesInMenuOrder() {
        navigate(HomeView.class);

        assertEquals(
                1, findInView(H1.class).withText("Collaboration — use cases")
                        .all().size(),
                "the home view should render its heading");
        assertEquals(List.of(
                new UseCase("UC1", "Show who is here",
                        "Showing everybody who is looking at the same thing",
                        "presence"),
                new UseCase("UC2", "Join and leave",
                        "Letting users decide when they count as present",
                        "presence-opt-in"),
                new UseCase("UC3", "Custom user list",
                        "Rendering the list of present users with my own components",
                        "presence-custom"),
                new UseCase("UC4", "Real-time chat",
                        "Sending messages that every participant sees at once",
                        "chat"),
                new UseCase("UC5", "Collaborative form",
                        "Several users editing the same form, with field highlights",
                        "form"),
                new UseCase("UC6", "Collaboration events",
                        "Reacting to what other users do in a shared form",
                        "form-events"),
                new UseCase("UC7", "Write access",
                        "Letting only some users edit a shared form",
                        "form-access"),
                new UseCase("UC8", "Collaborative Grid",
                        "Showing which row each user is looking at in a Grid",
                        "grid"),
                new UseCase("UC9", "Collaborative Grid Pro",
                        "Editing a Grid Pro together without overwriting each other",
                        "grid-pro"),
                new UseCase("UC10", "Conflicts & atomicity",
                        "Keeping concurrent edits from losing updates",
                        "conflicts"),
                new UseCase("UC11", "Computed shared state",
                        "Deriving shared totals instead of storing them",
                        "computed"),
                new UseCase("UC12", "Rooms on demand",
                        "Creating a shared topic per room only when someone opens it",
                        "rooms"),
                new UseCase("UC13", "Pending vs confirmed",
                        "Telling a local, unconfirmed change from a confirmed one",
                        "pending")),
                findInView(Card.class).all().stream()
                        .map(HomeViewTest::useCaseOf).toList());
    }

    @Test
    void reachesTheFirstAndLastUseCase() {
        // Both ends of the range, because the numbering is hand-written in the
        // @Menu order and a duplicate order silently reorders the index.
        navigate(PresenceView.class);
        assertEquals(1, findInView(H1.class).all().size());
        navigate(PendingStateView.class);
        assertEquals(1, findInView(H1.class).all().size());
    }

    private static UseCase useCaseOf(Card card) {
        return new UseCase(card.getHeader().getElement().getText(),
                card.getTitle().getElement().getText(),
                card.getChildren().filter(Paragraph.class::isInstance)
                        .map(p -> ((Paragraph) p).getText()).findFirst()
                        .orElse(null),
                Stream.of(card.getFooterComponents())
                        .filter(RouterLink.class::isInstance)
                        .map(link -> ((RouterLink) link).getHref()).findFirst()
                        .orElse(null));
    }
}
