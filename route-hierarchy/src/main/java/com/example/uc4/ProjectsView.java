package com.example.uc4;

import com.example.views.MainLayout;

import com.vaadin.flow.component.breadcrumbs.Breadcrumbs;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;

/**
 * UC4 — Parameter-preserving links (root, {@code uc4}).
 * <p>
 * The hierarchy is deep and parameterised: {@code uc4} → {@code uc4/:projectId}
 * → {@code uc4/:projectId/tasks} → {@code uc4/:projectId/tasks/:taskId}.
 * {@code getRouteHierarchy} walks it by stripping segments and pairs each
 * ancestor with the {@code RouteParameters} subset its own template needs, so
 * every ancestor link keeps the live {@code :projectId} without the caller
 * re-deriving it.
 */
@Route(value = "uc4", layout = MainLayout.class)
@PageTitle("Projects")
@Menu(order = 4, title = "UC4 — Parameter-preserving links")
public class ProjectsView extends VerticalLayout {

    public ProjectsView() {
        add(new Breadcrumbs());
        add(new H1("Projects"));
        add(new Paragraph(
                "Open a project, then its tasks, then a single task. On the "
                        + "deepest page the breadcrumb's Project and Tasks "
                        + "links still point at the right project because the "
                        + ":projectId is carried onto each ancestor link."));
        ProjectData.PROJECTS.forEach((id,
                name) -> add(new RouterLink("Open " + name, ProjectView.class,
                        new RouteParameters("projectId", id))));
    }
}
