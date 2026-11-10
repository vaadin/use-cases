package com.example.home;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.example.AiTestConfiguration;
import com.example.uc1.OrderAssistantView;
import com.example.uc2.InvoiceReaderView;
import com.example.uc3.HandbookView;
import com.example.uc4.DocsAssistantView;
import com.example.uc5.VoiceOverView;
import com.example.uc6.PresenterVideoView;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.sidenav.SideNavItem;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Import(AiTestConfiguration.class)
@ViewPackages(classes = { HomeView.class, OrderAssistantView.class,
        InvoiceReaderView.class, HandbookView.class, DocsAssistantView.class,
        VoiceOverView.class, PresenterVideoView.class })
class HomeViewTest extends SpringBrowserlessTest {

    private record UseCase(String tag, String title, String problem) {
    }

    private record Group(String heading, List<UseCase> useCases) {
    }

    private static final UseCase UC1 = new UseCase("UC1",
            "Ask about your orders",
            "Answering questions about business data in plain language, through the app's own Java methods");
    private static final UseCase UC2 = new UseCase("UC2", "Read an invoice",
            "Filling in a form from an uploaded invoice, with the app's validation and a human approving the result");
    private static final UseCase UC3 = new UseCase("UC3", "Employee handbook",
            "Answering employees' questions from an internal handbook, with the sections the answer came from");
    private static final UseCase UC4 = new UseCase("UC4",
            "Answers from product docs",
            "Answering how-to questions from current product documentation served by an MCP server");
    private static final UseCase UC5 = new UseCase("UC5",
            "Voice-over from text",
            "Turning written text into narrated audio for announcements and training");
    private static final UseCase UC6 = new UseCase("UC6", "Presenter video",
            "Generating a presenter video from a script as a background job that takes minutes");

    private static final List<UseCase> USE_CASES = List.of(UC1, UC2, UC3, UC4,
            UC5, UC6);

    @Test
    void cardsAreGroupedByKindOfHelpInMenuOrder() {
        HomeView home = navigate(HomeView.class);

        List<Group> groups = home.getChildren()
                .filter(Section.class::isInstance)
                .map(section -> new Group(
                        find(H2.class).from(section).single().getText(),
                        find(Card.class).from(section).all().stream()
                                .map(HomeViewTest::useCaseOf).toList()))
                .toList();

        assertEquals(List.of(
                new Group("Answers from your own data and documents",
                        List.of(UC1, UC3, UC4)),
                new Group("Work taken off the user's hands",
                        List.of(UC2, UC5, UC6))),
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
