package com.example.home;

import java.util.List;
import java.util.stream.Stream;

import com.example.uc1.PresenceView;
import com.example.uc10.ConflictsView;
import com.example.uc11.ComputedStateView;
import com.example.uc12.RoomsView;
import com.example.uc13.PendingStateView;
import com.example.uc2.OptInPresenceView;
import com.example.uc3.CustomUserListView;
import com.example.uc4.ChatView;
import com.example.uc5.CollaborativeFormView;
import com.example.uc6.FormEventsView;
import com.example.uc7.FormAccessView;
import com.example.uc8.CollaborativeGridView;
import com.example.uc9.CollaborativeGridProView;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
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
        assertEquals(
                List.of("See who else is here", "Chat and edit a form together",
                        "Edit a grid together", "Keep shared data correct"),
                findInView(H2.class).all().stream().map(H2::getText).toList(),
                "the cards should be grouped by kind of problem");
        assertEquals(List.of(
                new UseCase("UC1", "Show who is here",
                        "Showing everybody who is looking at the same thing",
                        "presence"),
                new UseCase("UC2", "Join and leave on purpose",
                        "Letting users decide when they count as present",
                        "presence-opt-in"),
                new UseCase("UC3", "Render presence my own way",
                        "Rendering the list of present users with my own components",
                        "presence-custom"),
                new UseCase("UC4", "Chat in real time",
                        "Sending messages that every participant sees at once",
                        "chat"),
                new UseCase("UC5", "Edit a form together",
                        "Several users editing the same form, with field highlights",
                        "form"),
                new UseCase("UC6", "Log who changed what",
                        "Reacting to what other users do in a shared form",
                        "form-events"),
                new UseCase("UC7", "Control who may edit",
                        "Letting only some users edit a shared form",
                        "form-access"),
                new UseCase("UC8", "Collaborate in a Grid",
                        "Showing which row each user is looking at in a Grid",
                        "grid"),
                new UseCase("UC9", "Collaborate in a Grid Pro",
                        "Editing a Grid Pro together without overwriting each other",
                        "grid-pro"),
                new UseCase("UC10", "Make concurrent edits safe",
                        "Keeping concurrent edits from losing updates",
                        "conflicts"),
                new UseCase("UC11", "Keep totals always in step",
                        "Deriving shared totals instead of storing them",
                        "computed"),
                new UseCase("UC12", "Rooms created on demand",
                        "Creating a shared topic per room only when someone opens it",
                        "rooms"),
                new UseCase("UC13", "Know if a write went through",
                        "Telling the user whether their change was accepted",
                        "pending")),
                findInView(Card.class).all().stream()
                        .map(HomeViewTest::useCaseOf).toList());
    }

    @Test
    void everyUseCaseIsHeadedByItsMenuTitle() {
        // The heading, the side navigation and the browser tab name a use case
        // the same way, so a reader can tell where they are.
        for (Class<? extends Component> view : List.of(PresenceView.class,
                OptInPresenceView.class, CustomUserListView.class,
                ChatView.class, CollaborativeFormView.class,
                FormEventsView.class, FormAccessView.class,
                CollaborativeGridView.class, CollaborativeGridProView.class,
                ConflictsView.class, ComputedStateView.class, RoomsView.class,
                PendingStateView.class)) {
            navigate(view);
            String title = view.getAnnotation(Menu.class).title();
            assertEquals(List.of(title), findInView(H1.class).all().stream()
                    .map(H1::getText).toList(), view.getSimpleName());
            assertEquals(title, view.getAnnotation(PageTitle.class).value(),
                    view.getSimpleName());
        }
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
