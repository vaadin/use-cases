package com.example.uc7;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.example.uc1.SubcategoryView;
import com.example.uc2.OrdersView;
import com.example.uc5.SessionsView;
import com.example.uc6.TeamView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.breadcrumbs.Breadcrumbs;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.internal.menu.MenuRegistry;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouteReference;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinService;

/**
 * UC7 — Route-tree sitemap.
 * <p>
 * The same {@code getRouteHierarchy} walker that powers breadcrumbs is just as
 * useful for building a route <em>graph</em>. Here a set of leaf routes from
 * across the demo is expanded into root-to-leaf chains and the chains are
 * merged into a single tree, rendered as a nested list. No breadcrumb, no
 * current page — a sitemap / SEO-link-graph consumer, exactly the kind the PR
 * names beyond breadcrumbs.
 */
@Route(value = "uc7", layout = MainLayout.class)
@PageTitle("Sitemap")
@Menu(order = 7, title = "UC7 — Route-tree sitemap")
public class SitemapView extends VerticalLayout {

    public static final String ROOT_LIST_ID = "sitemap-root";

    /** Leaf routes whose ancestor chains are merged into the sitemap. */
    private static final List<Class<? extends Component>> LEAVES = List.of(
            SubcategoryView.class, SessionsView.class, TeamView.class,
            OrdersView.class);

    public SitemapView() {
        add(new Breadcrumbs());
        add(new H1("Sitemap"));
        add(new Paragraph("Every node below was produced by calling "
                + "getRouteHierarchy on a handful of leaf routes and merging "
                + "the returned chains into one tree. The walker that draws a "
                + "breadcrumb draws a sitemap just as well."));
        add(buildTree());
    }

    private static UnorderedList buildTree() {
        RouteRegistry registry = VaadinService.getCurrent().getRouter()
                .getRegistry();
        Set<Class<? extends Component>> roots = new LinkedHashSet<>();
        Map<Class<? extends Component>, Set<Class<? extends Component>>> children = new LinkedHashMap<>();

        for (Class<? extends Component> leaf : LEAVES) {
            List<RouteReference> chain = RouteUtil.getRouteHierarchy(registry,
                    leaf, RouteParameters.empty());
            for (int i = 0; i < chain.size(); i++) {
                Class<? extends Component> node = chain.get(i)
                        .navigationTarget();
                if (i == 0) {
                    roots.add(node);
                } else {
                    children.computeIfAbsent(
                            chain.get(i - 1).navigationTarget(),
                            key -> new LinkedHashSet<>()).add(node);
                }
            }
        }

        UnorderedList tree = renderLevel(roots, children);
        tree.setId(ROOT_LIST_ID);
        return tree;
    }

    private static UnorderedList renderLevel(
            Set<Class<? extends Component>> nodes,
            Map<Class<? extends Component>, Set<Class<? extends Component>>> children) {
        UnorderedList list = new UnorderedList();
        list.addClassName("sitemap-tree");
        for (Class<? extends Component> node : nodes) {
            ListItem item = new ListItem();
            item.addClassName("sitemap-node");
            item.add(new RouterLink(
                    MenuRegistry.getTitle(node, RouteParameters.empty()),
                    node));
            Set<Class<? extends Component>> kids = children.get(node);
            if (kids != null && !kids.isEmpty()) {
                item.add(renderLevel(kids, children));
            }
            list.add(item);
        }
        return list;
    }
}
