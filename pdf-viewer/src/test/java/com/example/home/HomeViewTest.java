package com.example.home;

import java.util.List;
import java.util.stream.Stream;

import com.example.uc1.ReviewBeforeSendingView;
import com.example.uc2.ReadBeforeAcceptingView;
import com.example.uc3.DocumentLibraryView;
import com.example.uc4.ManualDeepLinkView;
import com.example.uc5.CheckReceiptView;
import com.example.uc6.PrintLabelView;
import com.example.uc7.JumpToClauseView;
import com.example.uc8.LocalizedViewerView;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.RouterLink;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = { HomeView.class, ReviewBeforeSendingView.class,
        ReadBeforeAcceptingView.class, DocumentLibraryView.class,
        ManualDeepLinkView.class, CheckReceiptView.class, PrintLabelView.class,
        JumpToClauseView.class, LocalizedViewerView.class })
class HomeViewTest extends SpringBrowserlessTest {

    private record UseCase(String tag, String title, String problem,
            String href) {
    }

    private record Group(String heading, List<UseCase> useCases) {
    }

    private static final UseCase UC1 = new UseCase("UC1",
            "Check before sending",
            "Showing a generated invoice inside the app before it is sent",
            "uc1");
    private static final UseCase UC2 = new UseCase("UC2",
            "Read before accepting",
            "Letting the user accept terms only after scrolling through them",
            "uc2");
    private static final UseCase UC3 = new UseCase("UC3", "Document library",
            "Previewing the files of a list, and explaining the ones that do not open",
            "uc3");
    private static final UseCase UC4 = new UseCase("UC4",
            "Open the manual at a page",
            "Linking into a long user guide at the page that answers a question",
            "uc4");
    private static final UseCase UC5 = new UseCase("UC5",
            "Check against the receipt",
            "Comparing an expense claim with its receipt side by side", "uc5");
    private static final UseCase UC6 = new UseCase("UC6",
            "Print a shipping label",
            "Printing a document from a button in the app's own UI", "uc6");
    private static final UseCase UC7 = new UseCase("UC7", "Jump to a clause",
            "Opening a contract at the clause that answers a question", "uc7");
    private static final UseCase UC8 = new UseCase("UC8",
            "Viewer in your language",
            "Showing the viewer's controls and messages in the user's language",
            "uc8");

    private static final List<UseCase> USE_CASES = List.of(UC1, UC2, UC3, UC4,
            UC5, UC6, UC7, UC8);

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
                new Group("Show the right document", List.of(UC1, UC3, UC5)),
                new Group("Guide the reader", List.of(UC2, UC4, UC7)),
                new Group("Fit the app", List.of(UC6, UC8))), groups);
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
