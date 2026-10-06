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
 * UC6 — Copy via a context-menu item.
 * <p>
 * Any {@link com.vaadin.flow.component.Component} that implements
 * {@link com.vaadin.flow.component.ClickNotifier} can be the source of a
 * {@link Clipboard#onClick} binding, including a {@link MenuItem}, so the same
 * client-side gesture-safe path applies to context-menu triggers.
 */
@Route(value = "uc6", layout = MainLayout.class)
@PageTitle("UC6 — Copy via context menu")
@UseCaseDescription("Offering copy as a context-menu item")
@Menu(order = 6, title = "UC6 — Context menu")
@StyleSheet("uc6.css")
public class CopyFromContextMenuView extends VerticalLayout {

    public CopyFromContextMenuView() {
        addClassName("uc6-view");
        add(new H1("UC6 — Copy via a context-menu item"));
        add(new Paragraph(
                "Right-click (or long-press) the box below and pick \"Copy "
                        + "value\". The same Clipboard.onClick API works on a "
                        + "menu item — no JavaScript needed."));

        String value = "secret-token-9f8e7a6b";

        Pre target = new Pre(value);
        target.addClassName("copy-target");

        ContextMenu menu = new ContextMenu(target);
        MenuItem copyItem = menu.addItem("Copy value");
        Clipboard.onClick(copyItem).writeText(value,
                written -> Notification.show("Token copied"),
                error -> Notification.show("Copy failed: " + error.message()));

        add(target);
    }
}
