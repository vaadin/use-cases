package com.example.home;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.example.uc1.MillionRowGridView;
import com.example.uc10.VirtualScrollingView;
import com.example.uc11.GridSearchLoadingView;
import com.example.uc12.StructuredSkeletonView;
import com.example.uc13.BlurHashPreviewView;
import com.example.uc2.ParallelDashboardView;
import com.example.uc3.LongRunningJobView;
import com.example.uc4.OrderDetailView;
import com.example.uc5.LatestResponseWinsView;
import com.example.uc6.DeferredSectionsView;
import com.example.uc7.ThrottledFeedView;
import com.example.uc8.OptimisticSaveView;
import com.example.uc9.SlowRequestFeedbackView;
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
@ViewPackages(classes = { HomeView.class, MillionRowGridView.class,
        ParallelDashboardView.class, LongRunningJobView.class,
        OrderDetailView.class, LatestResponseWinsView.class,
        DeferredSectionsView.class, ThrottledFeedView.class,
        OptimisticSaveView.class, SlowRequestFeedbackView.class,
        VirtualScrollingView.class, GridSearchLoadingView.class,
        StructuredSkeletonView.class, BlurHashPreviewView.class })
class HomeViewTest extends SpringBrowserlessTest {

    private record UseCase(String tag, String title, String problem) {
    }

    private record Group(String heading, List<UseCase> useCases) {
    }

    private static final UseCase UC1 = new UseCase("UC1",
            "Browse a million rows",
            "Paging, sorting and filtering a large table in the backend");
    private static final UseCase UC2 = new UseCase("UC2", "Parallel dashboard",
            "Rendering the page at once and filling in each slow widget as it arrives");
    private static final UseCase UC3 = new UseCase("UC3", "Job with progress",
            "Reporting the progress of a background job and stopping it when no longer needed");
    private static final UseCase UC4 = new UseCase("UC4",
            "Detail that streams in",
            "Navigating at once and loading the route's data in the background");
    private static final UseCase UC5 = new UseCase("UC5",
            "Latest response wins",
            "Keeping a late, outdated answer from overwriting a newer one");
    private static final UseCase UC6 = new UseCase("UC6", "Build when needed",
            "Deferring heavy parts of a page until a tab, a details or the scroll position asks for them");
    private static final UseCase UC7 = new UseCase("UC7",
            "Throttle a live feed",
            "Merging a fast stream of updates into a few pushes per second");
    private static final UseCase UC8 = new UseCase("UC8", "Optimistic save",
            "Showing a change at once and rolling it back if the save fails");
    private static final UseCase UC9 = new UseCase("UC9",
            "Slow request feedback",
            "Disabling buttons, tuning the loading indicator and sending fewer requests while typing");
    private static final UseCase UC10 = new UseCase("UC10", "100,000 cards",
            "Rendering a huge list with a virtual list and a lightweight renderer");
    private static final UseCase UC11 = new UseCase("UC11",
            "Grid search loading",
            "Showing that a slow Grid search is running, has no results or failed");
    private static final UseCase UC12 = new UseCase("UC12",
            "Skeleton that fills in",
            "Showing the shape of a structured answer at once and filling it in part by part");
    private static final UseCase UC13 = new UseCase("UC13",
            "Slow images with a preview",
            "Showing a blurry preview with the right colours while a slow image loads");

    private static final List<UseCase> USE_CASES = List.of(UC1, UC2, UC3, UC4,
            UC5, UC6, UC7, UC8, UC9, UC10, UC11, UC12, UC13);

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

        assertEquals(
                List.of(new Group("Large data sets", List.of(UC1, UC10, UC11)),
                        new Group("Answering in the background",
                                List.of(UC2, UC3, UC4)),
                        new Group("Building only what the user sees",
                                List.of(UC6, UC12, UC13)),
                        new Group("Fewer, smarter round-trips",
                                List.of(UC5, UC7, UC8, UC9))),
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
