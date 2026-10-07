package com.example.uc8;

import java.util.List;

import com.example.views.MainLayout;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.fullscreen.Fullscreen;
import com.vaadin.flow.component.fullscreen.FullscreenState;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;

/**
 * UC8 — Overlays in fullscreen.
 * <p>
 * The whole point of {@code Component#requestFullscreen()} (here through
 * {@link Fullscreen#onClick(com.vaadin.flow.component.Component)
 * Fullscreen.onClick(enter).enter(panel)}) over a page-level fullscreen is that
 * it wraps a single component <em>together with the UI's overlay container</em>.
 * The browser only paints the fullscreen element and its descendants, so an
 * overlay attached to {@code document.body} would vanish the moment you go
 * fullscreen. Because Vaadin moves the overlay container inside the fullscreen
 * wrapper, every flyout — {@link MenuBar} sub-menus, a {@link Popover}, a
 * right-click {@link ContextMenu}, a {@link Select} dropdown, tooltips — keeps
 * opening on top of the fullscreened panel.
 * <p>
 * This view fullscreens a {@code panel} that hosts one of each overlay-opening
 * component. Enter fullscreen, then open each control: they all appear. Every
 * overlay action writes to a shared {@code lastAction} span so the wiring is
 * observable (and testable) without a real browser.
 */
@Route(value = "uc8", layout = MainLayout.class)
@Menu(order = 8, title = "UC8 — Overlays in fullscreen")
@StyleSheet("uc8.css")
public class OverlaysFullscreenView extends VerticalLayout {

    private final Span stateBadge = new Span();
    private final Span lastAction = new Span("nothing yet");
    private final Div panel = new Div();

    public OverlaysFullscreenView() {
        addClassName("uc8-view");
        add(new H1("UC8 — Overlays in fullscreen"));
        add(new Paragraph(
                "Component#requestFullscreen() wraps a single component along "
                        + "with the UI's overlay container, so flyouts stay "
                        + "visible in fullscreen — unlike page-level "
                        + "fullscreen, where an overlay rendered on the body "
                        + "would be clipped away. Enter fullscreen, then open "
                        + "the menu, popover, dropdown, right-click context "
                        + "menu and tooltip: each one renders on top of the "
                        + "fullscreened panel."));

        stateBadge.addClassName("status-badge");
        add(stateBadge);

        Button enter = new Button("Enter fullscreen");
        enter.addThemeVariants(ButtonVariant.PRIMARY);
        // Fullscreen needs the click's user gesture, so bind the request to the
        // button's click trigger. The panel — and the overlays anchored to its
        // contents — is what goes fullscreen.
        Fullscreen.onClick(enter).enter(panel);
        Button exit = new Button("Exit fullscreen", e -> Fullscreen.exit());
        add(new HorizontalLayout(enter, exit));

        panel.addClassName("overlay-panel");
        panel.add(buildToolbar());

        Paragraph status = new Paragraph("Last overlay action: ");
        lastAction.addClassName("last-action");
        status.add(lastAction);
        panel.add(status);

        add(panel);
    }

    private HorizontalLayout buildToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.addClassName("overlay-toolbar");

        toolbar.add(menuBar(), popoverTrigger(), select(), contextMenuTarget(),
                tooltipButton());
        return toolbar;
    }

    private MenuBar menuBar() {
        MenuBar menuBar = new MenuBar();
        var file = menuBar.addItem("File");
        var fileSub = file.getSubMenu();
        fileSub.addItem("New", e -> record("MenuBar: File ▸ New"));
        fileSub.addItem("Open", e -> record("MenuBar: File ▸ Open"));
        fileSub.addItem("Save", e -> record("MenuBar: File ▸ Save"));
        return menuBar;
    }

    private Button popoverTrigger() {
        Button trigger = new Button("Popover");

        Popover popover = new Popover();
        VerticalLayout content = new VerticalLayout(new Span("Inside a popover"),
                new Button("Action", e -> record("Popover: Action")));
        content.setSpacing(false);
        content.setPadding(false);
        popover.add(content);
        popover.setTarget(trigger);
        return trigger;
    }

    private Select<String> select() {
        Select<String> select = new Select<>();
        select.setPlaceholder("Pick a color");
        select.setItems(List.of("Red", "Green", "Blue"));
        select.addValueChangeListener(e -> record("Select: " + e.getValue()));
        return select;
    }

    private Button contextMenuTarget() {
        Button target = new Button("Right-click me");

        ContextMenu contextMenu = new ContextMenu();
        contextMenu.setTarget(target);
        contextMenu.addItem("Copy", e -> record("ContextMenu: Copy"));
        contextMenu.addItem("Paste", e -> record("ContextMenu: Paste"));
        return target;
    }

    private Button tooltipButton() {
        Button button = new Button("Hover for tooltip");
        button.setTooltipText("Tooltips are overlays too");
        return button;
    }

    private void record(String action) {
        lastAction.setText(action);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        Signal<FullscreenState> fs = Fullscreen.stateSignal();
        stateBadge.bindText(fs.map(OverlaysFullscreenView::badgeText));
        stateBadge.bindClassName("fullscreen",
                fs.map(s -> s == FullscreenState.FULLSCREEN));
        stateBadge.bindClassName("unsupported",
                fs.map(s -> s == FullscreenState.UNSUPPORTED));
        panel.bindClassName("fullscreen",
                fs.map(s -> s == FullscreenState.FULLSCREEN));
    }

    private static String badgeText(FullscreenState state) {
        // The panel (and this badge lives outside it) is what goes fullscreen,
        // so a FULLSCREEN-specific message wouldn't be visible. Keep the idle
        // prompt instead.
        return switch (state) {
        case FULLSCREEN, NOT_FULLSCREEN -> "Enter fullscreen, then open a "
                + "menu, popover or dropdown";
        case UNSUPPORTED -> "Fullscreen not supported in this browser";
        case UNKNOWN -> "Detecting fullscreen support…";
        };
    }
}
