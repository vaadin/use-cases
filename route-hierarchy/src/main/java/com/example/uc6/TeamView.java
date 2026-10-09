package com.example.uc6;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;

/**
 * UC6 — Layout-wide auto breadcrumbs (team, {@code uc6/team}).
 */
@Route(value = "uc6/team", layout = TeamLayout.class)
@PageTitle("Team")
@Menu(order = 14, title = "Team")
public class TeamView extends VerticalLayout {

    public TeamView() {
        add(new H1("Team"));
        add(new Paragraph("Pick a member. The shared breadcrumb in the layout "
                + "will read Dashboard › Team › <name>."));
        TeamMembers.MEMBERS.forEach((id, name) -> add(new RouterLink(name,
                MemberView.class, new RouteParameters("member", id))));
    }
}
