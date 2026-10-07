package com.example.uc4;

import com.example.views.MainLayout;

import com.vaadin.flow.component.breadcrumbs.Breadcrumbs;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.DynamicPageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;

/**
 * UC4 — Parameter-preserving links (project, {@code uc4/:projectId}).
 * <p>
 * When this view is an <em>ancestor</em> of a deeper page, its breadcrumb crumb
 * is still dynamic: {@link ProjectTitleGenerator} resolves "Project Apollo"
 * from the {@code :projectId} that {@code getRouteHierarchy} carries for this
 * crumb — a dynamic ancestor label, instance-free.
 */
@Route(value = "uc4/:projectId", layout = MainLayout.class)
@DynamicPageTitle(ProjectTitleGenerator.class)
public class ProjectView extends VerticalLayout implements BeforeEnterObserver {

    private final H1 heading = new H1();
    private final RouterLink tasksLink = new RouterLink();

    public ProjectView() {
        add(new Breadcrumbs());
        add(heading);
        add(new Paragraph(
                "The Projects crumb above is a parameterless ancestor; this "
                        + "Project page carries the :projectId that the deeper "
                        + "pages will keep in their breadcrumb links."));
        tasksLink.setText("View tasks →");
        add(tasksLink);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RouteParameters parameters = event.getRouteParameters();
        String projectId = parameters.get("projectId").orElse("?");
        heading.setText(ProjectData.projectName(projectId));
        tasksLink.setRoute(TasksView.class,
                new RouteParameters("projectId", projectId));
    }
}
