package com.example.home;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.example.uc1.DamageReportView;
import com.example.uc2.AttachDocumentsView;
import com.example.uc3.ProfilePictureView;
import com.example.uc4.PhotoAlbumView;
import com.example.uc5.LargeFileView;
import com.example.uc6.ImportCsvView;
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
@ViewPackages(classes = { HomeView.class, DamageReportView.class,
        AttachDocumentsView.class, ProfilePictureView.class,
        PhotoAlbumView.class, LargeFileView.class, ImportCsvView.class })
class HomeViewTest extends SpringBrowserlessTest {

    private record UseCase(String tag, String title, String problem) {
    }

    private record Group(String heading, List<UseCase> useCases) {
    }

    private static final UseCase UC1 = new UseCase("UC1", "Report damage",
            "Photographing a problem on the spot and sending it with its location");
    private static final UseCase UC2 = new UseCase("UC2", "Attach documents",
            "Sending documents and processing them together with the rest of a form");
    private static final UseCase UC3 = new UseCase("UC3", "Profile picture",
            "Replacing a single picture and resizing it on the server");
    private static final UseCase UC4 = new UseCase("UC4", "Photo album",
            "Adding many photos at once and browsing them as thumbnails");
    private static final UseCase UC5 = new UseCase("UC5", "Large file",
            "Sending a large file with progress, cancel and a checksum");
    private static final UseCase UC6 = new UseCase("UC6", "Import a file",
            "Checking every row of an uploaded spreadsheet before importing it");

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

        assertEquals(List
                .of(new Group("Files that come with a form", List.of(UC1, UC2)),
                        new Group("Pictures", List.of(UC3, UC4)), new Group(
                                "Large files and imports", List.of(UC5, UC6))),
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
