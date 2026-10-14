package com.example.home;

import java.util.List;
import java.util.stream.Stream;

import com.example.uc1.OneShotOnClickView;
import com.example.uc2.TrackingView;
import com.example.uc3.AutoFetchView;
import com.example.uc4.DenialView;
import com.example.uc5.DetailedDataView;
import com.example.uc6.OptionsView;
import com.example.uc7.FormFieldView;
import com.example.uc8.DbTrackingView;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.RouterLink;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = { HomeView.class, OneShotOnClickView.class,
        TrackingView.class, AutoFetchView.class, DenialView.class,
        DetailedDataView.class, OptionsView.class, FormFieldView.class,
        DbTrackingView.class })
class HomeViewTest extends SpringBrowserlessTest {

    private record UseCase(String tag, String title, String problem,
            String href) {
    }

    @Test
    void cardsListTheUseCasesInMenuOrder() {
        navigate(HomeView.class);

        assertEquals(List.of(
                new UseCase("UC1", "One-shot request",
                        "Asking for the user's location with a button", "uc1"),
                new UseCase("UC2", "Tracking",
                        "Following the user's position as it changes", "uc2"),
                new UseCase("UC3", "Auto-fetch on attach",
                        "Locating returning visitors without a surprise prompt",
                        "uc3"),
                new UseCase("UC4", "Denial & unavailability",
                        "Handling a denied, failed or unavailable location",
                        "uc4"),
                new UseCase("UC5", "Detailed data",
                        "Reading altitude, heading, speed and accuracy", "uc5"),
                new UseCase("UC6", "Tuning options",
                        "Trading precision and freshness against battery",
                        "uc6"),
                new UseCase("UC7", "Form field",
                        "Capturing a location as part of a form", "uc7"),
                new UseCase("UC8", "DB-backed tracking",
                        "Storing a tracked route in a database", "uc8")),
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
