package com.example.uc6;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.FullscreenTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.fullscreen.FullscreenState;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = ChartExpandView.class)
class ChartExpandViewTest extends SpringBrowserlessTest {

    @Test
    void onlyTheClickedCardLooksExpandedWhileFullscreen() {
        navigate(ChartExpandView.class);
        FullscreenTestSupport
                .setFullscreenState(FullscreenState.NOT_FULLSCREEN);
        runPendingSignalsTasks();

        clickExpandOf("Conversion");
        runPendingSignalsTasks();
        // Not fullscreen yet (or the request was rejected): no card changes.
        assertEquals(Set.of(), expandedCards());

        FullscreenTestSupport.setFullscreenState(FullscreenState.FULLSCREEN);
        runPendingSignalsTasks();
        assertEquals(Set.of("Conversion"), expandedCards());

        // Escape: the card goes back to its dashboard size.
        FullscreenTestSupport
                .setFullscreenState(FullscreenState.NOT_FULLSCREEN);
        runPendingSignalsTasks();
        assertEquals(Set.of(), expandedCards());
    }

    @Test
    void expandingAnotherCardMovesTheExpandedLook() {
        navigate(ChartExpandView.class);

        clickExpandOf("Visitors");
        FullscreenTestSupport.setFullscreenState(FullscreenState.FULLSCREEN);
        runPendingSignalsTasks();
        assertEquals(Set.of("Visitors"), expandedCards());

        FullscreenTestSupport
                .setFullscreenState(FullscreenState.NOT_FULLSCREEN);
        clickExpandOf("Revenue");
        FullscreenTestSupport.setFullscreenState(FullscreenState.FULLSCREEN);
        runPendingSignalsTasks();
        assertEquals(Set.of("Revenue"), expandedCards());
    }

    private void clickExpandOf(String title) {
        Div card = cards().stream().filter(c -> title.equals(titleOf(c)))
                .findFirst().orElseThrow();
        test(find(Button.class).from(card).single()).click();
    }

    private List<Div> cards() {
        return findInView(Div.class).all().stream()
                .filter(d -> d.hasClassName("uc6-card")).toList();
    }

    private Set<String> expandedCards() {
        return cards().stream().filter(c -> c.hasClassName("uc6-card-expanded"))
                .map(ChartExpandViewTest::titleOf).collect(Collectors.toSet());
    }

    private static String titleOf(Div card) {
        return card.getChildren().flatMap(c -> c.getChildren())
                .filter(Span.class::isInstance).map(c -> ((Span) c).getText())
                .findFirst().orElseThrow();
    }
}
