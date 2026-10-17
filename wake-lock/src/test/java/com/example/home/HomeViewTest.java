package com.example.home;

import java.util.List;
import java.util.stream.Stream;

import com.example.uc1.ManualToggleView;
import com.example.uc2.RecipeView;
import com.example.uc3.SlideshowView;
import com.example.uc4.WorkoutTimerView;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.RouterLink;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = { HomeView.class, ManualToggleView.class,
        RecipeView.class, SlideshowView.class, WorkoutTimerView.class })
class HomeViewTest extends SpringBrowserlessTest {

    private record UseCase(String tag, String title, String problem,
            String href) {
    }

    private static final List<UseCase> USE_CASES = List.of(
            new UseCase("UC1", "Keep-awake toggle",
                    "Letting users switch keep-screen-awake on and off", "uc1"),
            new UseCase("UC2", "Step-by-step recipe",
                    "Keeping the screen on while someone follows step-by-step instructions",
                    "uc2"),
            new UseCase("UC3", "Presentation slideshow",
                    "Keeping the screen awake only while a presentation is running",
                    "uc3"),
            new UseCase("UC4", "Workout timer",
                    "Keeping the screen awake only while a timer is running",
                    "uc4"));

    @Test
    void cardsListTheUseCasesInMenuOrder() {
        navigate(HomeView.class);

        assertEquals(USE_CASES, findInView(Card.class).all().stream()
                .map(HomeViewTest::useCaseOf).toList());
    }

    @Test
    void navItemsKeepTheShortNameAndShowTheProblemAsTooltip() {
        navigate(HomeView.class);

        assertEquals(
                USE_CASES.stream()
                        .map(useCase -> useCase.tag() + " — " + useCase.title()
                                + ": " + useCase.problem())
                        .toList(),
                find(SideNavItem.class).all().stream()
                        .filter(item -> !"Home".equals(item.getLabel()))
                        .map(item -> item.getLabel() + ": "
                                + item.getTooltip().getText())
                        .toList(),
                "the nav tooltip and the home card read the same text");
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
