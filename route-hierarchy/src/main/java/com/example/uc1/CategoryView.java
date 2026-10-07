package com.example.uc1;

import com.example.views.MainLayout;

import com.vaadin.flow.component.breadcrumbs.Breadcrumbs;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

/**
 * UC1 — URL-prefix trail (category level, {@code uc1/electronics}).
 * <p>
 * Its parent {@code uc1} is found by stripping the {@code /electronics} segment
 * and looking up the shorter URL — no annotation involved.
 */
@Route(value = "uc1/electronics", layout = MainLayout.class)
@PageTitle("Electronics")
public class CategoryView extends VerticalLayout {

    public CategoryView() {
        add(new Breadcrumbs());
        add(new H1("Electronics"));
        add(new Paragraph(
                "The breadcrumb above now reads Catalog › Electronics. The "
                        + "\"Catalog\" crumb is a real link resolved from "
                        + "the uc1 route class; \"Electronics\" is the current "
                        + "page and is not a link."));
        add(new RouterLink("Browse Laptops →", SubcategoryView.class));
    }
}
