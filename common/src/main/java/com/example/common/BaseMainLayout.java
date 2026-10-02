package com.example.common;

import java.util.List;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;

/**
 * Shared {@link AppLayout} for the use-case demo apps. Renders a navbar with
 * the module title, a drawer holding the cross-app {@link AppCatalog} selector
 * and the auto-generated side navigation, and a fixed-position "View source"
 * overlay that links the current route's view class to its source on GitHub.
 * Subclasses only need to pass their module id and display title.
 */
public abstract class BaseMainLayout extends AppLayout
        implements BeforeEnterObserver {

    private final String moduleId;
    private final Anchor sourceCodeLink;

    protected BaseMainLayout(String moduleId, String title) {
        this.moduleId = moduleId;

        DrawerToggle toggle = new DrawerToggle();

        H1 h1 = new H1(title);
        h1.getStyle().set("font-size", "var(--aura-font-size-l)").set("margin",
                "0");

        addToNavbar(toggle, h1);

        sourceCodeLink = new Anchor("", "View source");
        getElement().appendChild(buildSourceCodeOverlay(sourceCodeLink));

        addToDrawer(AppCatalog.createSelector(moduleId));

        SideNav nav = new SideNav();
        mainNavEntries().forEach(entry -> nav
                .addItem(new SideNavItem(entry.title(), entry.path())));
        addToDrawer(nav);
    }

    /**
     * The menu entries listed in the main side navigation, in order. The
     * default is the flat {@link MenuConfiguration#getMenuEntries()}; a module
     * whose menu nests routes can override this to keep the deeper entries out
     * of the flat nav.
     */
    protected List<MenuEntry> mainNavEntries() {
        return MenuConfiguration.getMenuEntries();
    }

    private static Element buildSourceCodeOverlay(Anchor link) {
        Div container = new Div();
        container.getStyle().set("position", "fixed").set("top",
                "calc(var(--vaadin-app-layout-navbar-offset-top) + 0.5em)")
                .set("right", "1em").set("z-index", "100")
                .set("pointer-events", "auto");

        Icon codeIcon = VaadinIcon.CODE.create();
        codeIcon.setSize("16px");
        codeIcon.getStyle().set("color", "var(--vaadin-text-color-secondary)")
                .set("margin-right", "0.5em");

        link.setTarget("_blank");
        link.getStyle().set("display", "inline-flex")
                .set("align-items", "center")
                .set("background-color", "rgba(255, 255, 255, 0.95)")
                .set("padding", "0.5em 0.75em").set("border-radius", "4px")
                .set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.1)")
                .set("color", "var(--aura-accent-text-color)")
                .set("text-decoration", "none")
                .set("font-size", "var(--aura-font-size-s)")
                .set("transition", "box-shadow 0.2s");

        link.getElement()
                .addEventListener("mouseenter",
                        e -> link.getStyle().set("box-shadow",
                                "0 4px 8px rgba(0, 0, 0, 0.15)"))
                .addEventData("event.preventDefault");
        link.getElement()
                .addEventListener("mouseleave",
                        e -> link.getStyle().set("box-shadow",
                                "0 2px 4px rgba(0, 0, 0, 0.1)"))
                .addEventData("event.preventDefault");

        Span content = new Span(codeIcon);
        content.add("View source");
        link.removeAll();
        link.add(content);

        container.add(link);
        return container.getElement();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Class<?> viewClass = event.getNavigationTarget();
        if (viewClass == null) {
            return;
        }
        String className = viewClass.getSimpleName();
        String packagePath = viewClass.getPackageName().replace(".", "/");
        sourceCodeLink.setHref("https://github.com/vaadin/use-cases/tree/main/"
                + moduleId + "/src/main/java/" + packagePath + "/" + className
                + ".java");
        sourceCodeLink.setVisible(className.endsWith("View"));
    }
}
