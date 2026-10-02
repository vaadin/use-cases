package com.example.views;

import java.util.List;
import java.util.stream.Stream;

import com.example.common.BaseMainLayout;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;

@PageTitle("Route Hierarchy Use Cases")
public class MainLayout extends BaseMainLayout {

    public MainLayout() {
        super("route-hierarchy", "Route Hierarchy Use Cases");
    }

    /**
     * Keep the flat main navigation to top-level use cases by reading the menu
     * as a tree — {@link MenuConfiguration#getMenuEntriesTree()} nests by the
     * route hierarchy, so depth is a property of the entry rather than
     * something to re-derive from URL paths (a logical parent need not share a
     * URL prefix). Every use-case root sits under the landing page, so the
     * roots plus their direct children are Home and UC1–UC8; the deeper nested
     * {@code @Menu} views (Electronics, Security, Team, ...) stay out of the
     * flat nav and surface only in UC8's tree.
     */
    @Override
    protected List<MenuEntry> mainNavEntries() {
        return MenuConfiguration.getMenuEntriesTree().stream()
                .flatMap(root -> Stream.concat(Stream.of(root),
                        root.children().stream()))
                .toList();
    }
}
