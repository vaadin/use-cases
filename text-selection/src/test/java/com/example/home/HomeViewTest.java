package com.example.home;

import java.util.List;
import java.util.stream.Stream;

import com.example.uc1.SelectAllOnFocusView;
import com.example.uc2.FormatAndSelectView;
import com.example.uc3.FindAndHighlightView;
import com.example.uc4.ValidationJumpView;
import com.example.uc5.InsertTemplateView;
import com.example.uc6.LiveSelectionInfoView;
import com.example.uc7.SelectionToolbarView;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.RouterLink;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = { HomeView.class, SelectAllOnFocusView.class,
        FormatAndSelectView.class, FindAndHighlightView.class,
        ValidationJumpView.class, InsertTemplateView.class,
        LiveSelectionInfoView.class, SelectionToolbarView.class })
class HomeViewTest extends SpringBrowserlessTest {

    private record UseCase(String tag, String title, String problem,
            String href) {
    }

    @Test
    void cardsListTheUseCasesInMenuOrder() {
        navigate(HomeView.class);

        assertEquals(List.of(
                new UseCase("UC1", "Select all on focus",
                        "Selecting a field's value when it gets focus", "uc1"),
                new UseCase("UC2", "Post-transform select-all",
                        "Selecting a value after the server reformats it",
                        "uc2"),
                new UseCase("UC3", "Find & highlight",
                        "Stepping through search matches in a text area",
                        "uc3"),
                new UseCase("UC4", "Jump to validation error",
                        "Pointing the user to the exact invalid part of a value",
                        "uc4"),
                new UseCase("UC5", "Insert template at cursor",
                        "Inserting a snippet at the cursor or over a selection",
                        "uc5"),
                new UseCase("UC6", "Live selection info",
                        "Showing live details about the selected text", "uc6"),
                new UseCase("UC7", "Selection toolbar",
                        "Enabling toolbar actions that transform the selection",
                        "uc7")),
                findInView(Card.class).all().stream()
                        .map(HomeViewTest::useCaseOf).toList());
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
