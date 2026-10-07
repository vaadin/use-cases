package com.example.home;

import java.util.List;
import java.util.stream.Stream;

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
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.RouterLink;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = { HomeView.class, DamageReportView.class,
        AttachDocumentsView.class, ProfilePictureView.class,
        PhotoAlbumView.class, LargeFileView.class, ImportCsvView.class })
class HomeViewTest extends SpringBrowserlessTest {

    private record UseCase(String tag, String title, String problem,
            String href) {
    }

    @Test
    void cardsListTheUseCasesInMenuOrder() {
        navigate(HomeView.class);

        assertEquals(List.of(new UseCase("UC1", "Report damage",
                "Photographing a problem on the spot and sending it with its location",
                "uc1"),
                new UseCase("UC2", "Attach documents",
                        "Sending documents together with the rest of a form",
                        "uc2"),
                new UseCase("UC3", "Profile picture",
                        "Replacing a single picture and resizing it on the server",
                        "uc3"),
                new UseCase("UC4", "Photo album",
                        "Adding many photos at once and browsing them as thumbnails",
                        "uc4"),
                new UseCase("UC5", "Large file",
                        "Sending a large file with progress, cancel and a checksum",
                        "uc5"),
                new UseCase("UC6", "Import a file",
                        "Checking every row of an uploaded spreadsheet before importing it",
                        "uc6")),
                findInView(Card.class).all().stream()
                        .map(HomeViewTest::useCaseOf).toList());
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
