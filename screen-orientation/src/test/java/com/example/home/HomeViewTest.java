package com.example.home;

import java.util.List;
import java.util.stream.Stream;

import com.example.uc1.AdaptiveLayoutView;
import com.example.uc2.OrientationViewerView;
import com.example.uc3.RotatePromptView;
import com.example.uc4.LockForVideoView;
import com.example.uc5.LockErrorView;
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
@ViewPackages(classes = { HomeView.class, AdaptiveLayoutView.class,
        OrientationViewerView.class, RotatePromptView.class,
        LockForVideoView.class, LockErrorView.class })
class HomeViewTest extends SpringBrowserlessTest {

    private record UseCase(String tag, String title, String problem,
            String href) {
    }

    private static final List<UseCase> USE_CASES = List.of(
            new UseCase("UC1", "Adaptive layout",
                    "Switching the layout between landscape and portrait",
                    "uc1"),
            new UseCase("UC2", "Orientation viewer",
                    "Showing the current orientation type and angle", "uc2"),
            new UseCase("UC3", "Rotate prompt",
                    "Asking the user to rotate to the required orientation",
                    "uc3"),
            new UseCase("UC4", "Lock for video",
                    "Locking landscape in fullscreen while a video plays",
                    "uc4"),
            new UseCase("UC5", "Lock error UX",
                    "Telling the user why an orientation lock failed", "uc5"));

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
                        .filter(item -> item.getTooltip() != null
                                && item.getTooltip().getText() != null)
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
