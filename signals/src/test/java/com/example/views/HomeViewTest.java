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
            "Enabling and disabling a button from the state of a form");
    private static final UseCase UC2 = new UseCase("UC2", "Nested Conditions",
            "Revealing form sections step by step from nested conditions");
    private static final UseCase UC3 = new UseCase("UC3",
            "Interactive SVG Shape Editor",
            "Editing SVG shapes interactively with state kept in signals");
    private static final UseCase UC4 = new UseCase("UC4", "Filtered Data Grid",
            "Filtering and sorting a grid reactively from signal values");
    private static final UseCase UC5 = new UseCase("UC5", "Cascading Selector",
            "Keeping dependent location selectors in sync as the parent changes");
    private static final UseCase UC6 = new UseCase("UC6", "Shopping Cart",
            "Keeping cart line items and totals up to date as quantities change");
    private static final UseCase UC8 = new UseCase("UC8", "Multi-Step Wizard",
            "Guiding users through a multi-step wizard with per-step validation");
    private static final UseCase UC9 = new UseCase("UC9", "Binder Integration",
            "Combining Binder validation with signal-driven form state");
    private static final UseCase UC10 = new UseCase("UC10", "Events to Signals",
            "Turning DOM and browser events into signals");
    private static final UseCase UC11 = new UseCase("UC11", "Responsive Layout",
            "Adapting a layout to its container size instead of the window");
    private static final UseCase UC13 = new UseCase("UC13",
            "Real-Time Active Users",
            "Showing who is online across sessions without polling");
    private static final UseCase UC14 = new UseCase("UC14",
            "Async Data Loading",
            "Loading data asynchronously with loading, error and retry states");
    private static final UseCase UC15 = new UseCase("UC15", "Debounced Search",
            "Searching only after the user stops typing and cancelling stale requests");
    private static final UseCase UC16 = new UseCase("UC16",
            "URL State Integration",
            "Keeping search state and the URL in sync for shareable links");
    private static final UseCase UC17 = new UseCase("UC17",
            "PC Builder (70 signals)",
            "Managing a large form with dozens of dependent, validated signals");
    private static final UseCase UC18 = new UseCase("UC18", "LLM Task List",
            "Letting an LLM chat edit a task list through signals");
    private static final UseCase UC19 = new UseCase("UC19", "Parallel Loading",
            "Loading several items in parallel with per-item progress and retry");
    private static final UseCase UC20 = new UseCase("UC20", "User Preferences",
            "Keeping user preferences in a session-scoped signal across views");
    private static final UseCase UC21 = new UseCase("UC21",
            "Signals-Based i18n",
            "Switching the UI language reactively without rebuilding views");
    private static final UseCase UC22 = new UseCase("UC22",
            "Two-Way Mapped Signals",
            "Editing one value through a two-way mapped view of another signal");
    private static final UseCase UC23 = new UseCase("UC23",
            "Real-time Dashboard",
            "Updating a live service-health dashboard from background data");
    private static final UseCase UC24 = new UseCase("UC24",
            "VirtualList Notifications",
            "Feeding a virtual list from a signal of incoming notifications");
    private static final UseCase UC25 = new UseCase("UC25", "Stock Ticker",
            "Streaming price changes into a live ticker");
    private static final UseCase UC26 = new UseCase("UC26", "Lazy Creation",
            "Creating expensive components only when they are first needed");
    private static final UseCase UC27 = new UseCase("UC27",
            "Router State Signal",
            "Reacting to navigation and route parameters through a router state signal");
    private static final UseCase MUC1 = new UseCase("MUC1", "Shared Chat",
            "Chatting between users with a shared, append-only message list");
    private static final UseCase MUC2 = new UseCase("MUC2",
            "Collaborative Cursors",
            "Showing every user's cursor position to all others in real time");
    private static final UseCase MUC3 = new UseCase("MUC3", "Click Race Game",
            "Resolving who wins when several users click at the same time");
    private static final UseCase MUC4 = new UseCase("MUC4",
            "Collaborative Editing",
            "Showing who is editing which field and locking it for others");
    private static final UseCase MUC6 = new UseCase("MUC6", "Shared Task List",
            "Editing a shared task list together with live statistics");
    private static final UseCase MUC7 = new UseCase("MUC7",
            "Shared LLM Task List",
            "Letting several users manage one task list through a shared LLM chat");
    private static final UseCase MUC8 = new UseCase("MUC8",
            "Broadcast Announcement",
            "Broadcasting an announcement to all users without a custom broadcaster");

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
