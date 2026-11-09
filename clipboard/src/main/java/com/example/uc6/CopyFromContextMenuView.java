package com.example.uc6;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.clipboard.Clipboard;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC6 — Copy from a context menu.
 * <p>
 * Any {@link com.vaadin.flow.component.Component} that implements
 * {@link com.vaadin.flow.component.ClickNotifier} can be the source of a
 * {@link Clipboard#onClick} binding. A {@link MenuItem} of a
 * {@link ContextMenu} is one, so {@code Clipboard.onClick(menuItem)} copies
 * inside the browser's click handler, as a button does. The menu item of a
 * grid's context menu is not; see {@code API-GAPS.md} #3.
 */
@Route(value = "uc6", layout = MainLayout.class)
@PageTitle("UC6 — Copy from a context menu")
@UseCaseDescription("Offering copy as a context-menu item")
@Menu(order = 6, title = "UC6 — Copy from a context menu")
@StyleSheet("uc6.css")
public class CopyFromContextMenuView extends VerticalLayout {

    private static final String TOKEN = "secret-token-9f8e7a6b";

    // A closed context menu is not part of the UI tree, so the test reaches
    // it through the view.
    final ContextMenu menu;

    public CopyFromContextMenuView() {
        addClassName("uc6-view");
        add(new H1("UC6 — Copy from a context menu"));
        add(new Paragraph(
                "Right-click (or long-press) the box below and pick \"Copy "
                        + "value\". Clipboard.onClick works on a menu item "
                        + "as it does on a button, with no JavaScript."));

        Pre target = new Pre(TOKEN);
        target.addClassName("uc6-copy-target");

        menu = new ContextMenu(target);
        MenuItem copyItem = menu.addItem("Copy value");
        Clipboard.onClick(copyItem).writeText(TOKEN,
                written -> Notification.show("Token copied"),
                error -> Notification.show("Copy failed: " + error.message()));

        add(target);
    }
}
