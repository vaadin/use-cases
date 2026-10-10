package com.example.uc3;

import java.util.Objects;
import java.util.Optional;

import com.example.Appearance;
import com.example.common.UseCaseDescription;
import com.example.views.ComponentShowcase;
import com.example.views.MainLayout;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;

/**
 * UC3 — Theme per customer.
 * <p>
 * An application sold to several companies shows each of them their own colors,
 * corners, font and logo. The styling is data: each {@link Tenant} stores a few
 * token values, and a stylesheet generated from them is loaded after the base
 * theme when the customer's user signs in (here: picked from the list, standing
 * in for a login or a subdomain). No rebuild, and adding a customer is adding a
 * row.
 * <p>
 * Customer data ends up in CSS, so it is validated before it gets there: hex
 * colors, a radius within bounds and a font from a fixed list.
 */
@Route(value = "uc3", layout = MainLayout.class)
@PageTitle("UC3 — Theme per customer")
@UseCaseDescription("Giving each customer of a multi-tenant application its own look, at runtime")
@Menu(order = 3, title = "UC3 — Theme per customer")
@StyleSheet("uc3.css")
public class TenantThemeView extends VerticalLayout {

    private final Select<String> tenantSelect = new Select<>();
    private final Pre css = new Pre();

    public TenantThemeView(Tenants tenants) {
        Appearance appearance = Appearance.current();
        addClassName("uc3-view");

        add(new H1("UC3 — Theme per customer"));
        add(new Paragraph("Sign in as one of the customers: their stylesheet, "
                + "generated from a few values stored for them, is loaded on "
                + "top of the theme. Every view follows until you sign out."));

        tenantSelect.setLabel("Signed in as");
        tenantSelect.setEmptySelectionAllowed(true);
        tenantSelect.setEmptySelectionCaption("No customer (default look)");
        tenantSelect.setItems(tenants.all().stream().map(Tenant::id).toList());
        tenantSelect.setItemLabelGenerator(id -> id == null ? ""
                : tenants.find(id).map(Tenant::name).orElse(id));
        tenantSelect.bindValue(appearance.tenant(), appearance::setTenant);

        Signal<Optional<Tenant>> tenant = appearance.tenant()
                .map(id -> id == null ? Optional.empty() : tenants.find(id));

        Span logo = new Span();
        logo.addClassName("tenant-logo");
        logo.bindText(tenant.map(t -> t.map(Tenant::initials).orElse("◆")));
        H2 portal = new H2();
        portal.bindText(tenant
                .map(t -> t.map(Tenant::name).orElse("Orders") + " portal"));
        Div header = new Div(logo, portal);
        header.addClassName("tenant-header");

        css.bindText(tenant.map(t -> t.map(Tenant::styleSheet)
                .orElse("/* No customer stylesheet */")));
        css.bindVisible(tenant.map(Optional::isPresent));

        add(tenantSelect, header, css, new ComponentShowcase());
    }

    // Package-private test seams.
    Select<String> tenantSelect() {
        return tenantSelect;
    }

    String css() {
        return Objects.requireNonNullElse(css.getText(), "");
    }
}
