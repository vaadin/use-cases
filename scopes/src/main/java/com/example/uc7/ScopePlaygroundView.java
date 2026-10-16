package com.example.uc7;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import org.springframework.beans.factory.annotation.Qualifier;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC7 — Scope playground.
 * <p>
 * Every time this page is opened, it counts one visit in one bean per scope. A
 * count of 1 means the scope gave this visit a new bean. Next to the counts,
 * the table says which user actions each scope is expected to survive, so the
 * reader can try an action and check the counts against the expectation.
 */
@Route(value = "uc7", layout = MainLayout.class)
@UseCaseDescription("Seeing which scope survives a reload, a new tab or another browser")
@PageTitle("UC7 — Scope playground")
@Menu(order = 7, title = "UC7 — Scope playground")
public class ScopePlaygroundView extends VerticalLayout {

    enum Action {
        RELOAD("Reload"),
        NEW_TAB("New tab"),
        DUPLICATED_TAB("Duplicated tab"),
        OTHER_BROWSER("Other browser"),
        AWAY_AND_BACK("Away and back");

        private final String label;

        Action(String label) {
            this.label = label;
        }
    }

    record ScopeRow(String scope, String annotation, int visits,
            Set<Action> keptAfter) {
    }

    public ScopePlaygroundView(
            @Qualifier("applicationCounter") VisitCounter application,
            @Qualifier("httpSessionCounter") VisitCounter httpSession,
            @Qualifier("vaadinSessionCounter") VisitCounter vaadinSession,
            @Qualifier("browserTabCounter") VisitCounter browserTab,
            @Qualifier("uiCounter") VisitCounter ui,
            @Qualifier("routeCounter") VisitCounter route) {
        add(new H1("UC7 — Scope playground"));
        add(new Paragraph("Each opening of this page counts one visit in a "
                + "bean of every scope. Do one of the actions in the table "
                + "and look at the counts: a scope marked ✓ keeps its bean "
                + "and counts on, a scope marked ✗ starts over at 1."));
        add(new Paragraph("Reload: the button below or F5. New tab: open "
                + "this page in a second tab. Duplicated tab: the browser's "
                + "\"Duplicate tab\". Other browser: a private window. Away "
                + "and back: go to Home in the menu and return."));
        add(new Button("Reload this page",
                e -> UI.getCurrentOrThrow().getPage().reload()));

        Div table = new Div();
        table.addClassName("scope-table");
        table.add(cell("", "group-header"), cell("", "group-header"));
        table.add(cell("Keeps its bean after…", "group-header", "actions"));
        table.add(cell("Scope", "header"), cell("Visits", "header"));
        for (Action action : Action.values()) {
            table.add(cell(action.label, "header"));
        }

        Set<Action> sessionKeeps = EnumSet.of(Action.RELOAD, Action.NEW_TAB,
                Action.DUPLICATED_TAB, Action.AWAY_AND_BACK);
        List.of(new ScopeRow("Application", "singleton", application.visit(),
                EnumSet.allOf(Action.class)),
                new ScopeRow("HTTP session", "@SessionScope",
                        httpSession.visit(), sessionKeeps),
                new ScopeRow("Vaadin session", "@VaadinSessionScope",
                        vaadinSession.visit(), sessionKeeps),
                new ScopeRow("Browser tab", "@BrowserTabScope",
                        browserTab.visit(),
                        EnumSet.of(Action.RELOAD, Action.DUPLICATED_TAB,
                                Action.AWAY_AND_BACK)),
                new ScopeRow("UI", "@UIScope", ui.visit(),
                        EnumSet.of(Action.AWAY_AND_BACK)),
                new ScopeRow("Route", "@RouteScope", route.visit(),
                        EnumSet.noneOf(Action.class)))
                .forEach(row -> addRow(table, row));

        Div scroller = new Div(table);
        scroller.addClassName("scope-table-scroller");
        add(scroller);

        Span note = new Span("A duplicated tab shares the browser tab bean "
                + "with the original, because the browser copies the tab id "
                + "to the duplicate.");
        note.addClassName("hint");
        add(note);
    }

    private static void addRow(Div table, ScopeRow row) {
        Span annotation = new Span(row.annotation());
        annotation.addClassName("hint");
        Div scope = new Div(new Span(row.scope()), annotation);
        scope.addClassNames("cell", "scope-name");

        Span visits = new Span(String.valueOf(row.visits()));
        visits.addClassName("scope-badge");
        if (row.visits() == 1) {
            visits.addClassName("started-over");
        }
        Div visitsCell = new Div(visits);
        visitsCell.addClassName("cell");

        table.add(scope, visitsCell);
        for (Action action : Action.values()) {
            boolean kept = row.keptAfter().contains(action);
            table.add(cell(kept ? "✓" : "✗", kept ? "kept" : "lost"));
        }
    }

    private static Div cell(String text, String... classNames) {
        Div cell = new Div(text);
        cell.addClassName("cell");
        cell.addClassNames(classNames);
        return cell;
    }
}
