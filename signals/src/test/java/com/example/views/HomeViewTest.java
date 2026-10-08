package com.example.views;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.sidenav.SideNavItem;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(packages = "com.example")
@WithMockUser
class HomeViewTest extends SpringBrowserlessTest {

    private record UseCase(String tag, String title, String problem) {
    }

    private record Group(String heading, List<UseCase> useCases) {
    }

    private static final UseCase UC1 = new UseCase("UC1",
            "Dynamic Button State",
            "Enabling a submit button only when the form is valid");
    private static final UseCase UC2 = new UseCase("UC2", "Nested Conditions",
            "Showing form fields only when earlier answers need them");
    private static final UseCase UC3 = new UseCase("UC3",
            "Interactive SVG Shape Editor",
            "Binding SVG attributes to editable shape state");
    private static final UseCase UC4 = new UseCase("UC4", "Filtered Data Grid",
            "Filtering a grid from several search controls");
    private static final UseCase UC5 = new UseCase("UC5", "Cascading Selector",
            "Narrowing each dropdown by the previous selection");
    private static final UseCase UC6 = new UseCase("UC6", "Shopping Cart",
            "Keeping cart totals in sync with every change");
    private static final UseCase UC8 = new UseCase("UC8", "Multi-Step Wizard",
            "Unlocking wizard steps only after the current one validates");
    private static final UseCase UC9 = new UseCase("UC9", "Binder Integration",
            "Validating fields with rules that depend on other fields");
    private static final UseCase UC10 = new UseCase("UC10", "Events to Signals",
            "Turning uploads, shortcuts and dark mode into signals");
    private static final UseCase UC11 = new UseCase("UC11", "Responsive Layout",
            "Adapting content to the width of its container");
    private static final UseCase UC13 = new UseCase("UC13",
            "Real-Time Active Users",
            "Showing who is online and which view they are on");
    private static final UseCase UC14 = new UseCase("UC14",
            "Async Data Loading",
            "Showing loading, success and error states of a background task");
    private static final UseCase UC15 = new UseCase("UC15", "Debounced Search",
            "Searching as the user types without a call per keystroke");
    private static final UseCase UC16 = new UseCase("UC16",
            "URL State Integration",
            "Keeping search filters in the URL to share and go back");
    private static final UseCase UC17 = new UseCase("UC17",
            "PC Builder (70 signals)",
            "Keeping dozens of interdependent values consistent");
    private static final UseCase UC18 = new UseCase("UC18", "LLM Task List",
            "Letting an AI assistant edit a task list");
    private static final UseCase UC19 = new UseCase("UC19", "Parallel Loading",
            "Loading several items in parallel, each with its own spinner");
    private static final UseCase UC20 = new UseCase("UC20", "User Preferences",
            "Applying a user's preference across all views of the session");
    private static final UseCase UC21 = new UseCase("UC21",
            "Signals-Based i18n",
            "Switching the UI language without reloading the page");
    private static final UseCase UC22 = new UseCase("UC22",
            "Two-Way Mapped Signals",
            "Editing the fields of a record through two-way bindings");
    private static final UseCase UC23 = new UseCase("UC23",
            "Real-time Dashboard",
            "Refreshing a dashboard with data pushed from the server");
    private static final UseCase UC24 = new UseCase("UC24",
            "VirtualList Notifications",
            "Showing a filtered notification inbox in a virtual list");
    private static final UseCase UC25 = new UseCase("UC25", "Stock Ticker",
            "Flashing prices as they rise or fall in real time");
    private static final UseCase UC26 = new UseCase("UC26", "Lazy Creation",
            "Creating heavy components only when they are first shown");
    private static final UseCase UC27 = new UseCase("UC27",
            "Router State Signal",
            "Updating a breadcrumb whenever navigation completes");
    private static final UseCase MUC1 = new UseCase("MUC1", "Shared Chat",
            "Sharing chat messages with every user in real time");
    private static final UseCase MUC2 = new UseCase("MUC2",
            "Collaborative Cursors",
            "Showing where other users point in a shared area");
    private static final UseCase MUC3 = new UseCase("MUC3", "Click Race Game",
            "Deciding fairly which user clicked first");
    private static final UseCase MUC4 = new UseCase("MUC4",
            "Collaborative Editing", "Showing who is editing which form field");
    private static final UseCase MUC6 = new UseCase("MUC6", "Shared Task List",
            "Editing one task list together with other users");
    private static final UseCase MUC7 = new UseCase("MUC7",
            "Shared LLM Task List",
            "Letting an AI assistant edit a task list shared by all users");
    private static final UseCase MUC8 = new UseCase("MUC8",
            "Broadcast Announcement",
            "Broadcasting an announcement to all active users");

    private static final List<Group> GROUPS = List.of(
            new Group("Reactive UI basics",
                    List.of(UC1, UC2, UC3, UC10, UC11, UC26)),
            new Group("Forms and derived state",
                    List.of(UC5, UC8, UC9, UC17, UC22)),
            new Group("Lists and live data",
                    List.of(UC4, UC6, UC23, UC24, UC25)),
            new Group("Async loading and AI", List.of(UC14, UC15, UC18, UC19)),
            new Group("Session, URL and app-wide state",
                    List.of(UC13, UC16, UC20, UC21, UC27)),
            new Group("Multi-user collaboration",
                    List.of(MUC1, MUC2, MUC3, MUC4, MUC6, MUC7, MUC8)));

    @Test
    void cardsAreGroupedByKindOfProblemInMenuOrder() {
        assertEquals(GROUPS, groupsOf(navigate(HomeView.class)));
    }

    @Test
    @WithAnonymousUser
    void guestsSeeTheSameCards() {
        assertEquals(GROUPS, groupsOf(navigate(HomeView.class)));
    }

    private List<Group> groupsOf(HomeView home) {
        return home.getChildren().filter(Section.class::isInstance)
                .map(section -> new Group(
                        find(H2.class).from(section).single().getText(),
                        find(Card.class).from(section).all().stream()
                                .map(HomeViewTest::useCaseOf).toList()))
                .toList();
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

        GROUPS.stream().flatMap(group -> group.useCases().stream())
                .forEach(useCase -> assertEquals(useCase.problem(),
                        items.get(useCase.tag() + " — " + useCase.title())
                                .getTooltip().getText(),
                        "the nav tooltip and the home card read the same text"));
    }
}
