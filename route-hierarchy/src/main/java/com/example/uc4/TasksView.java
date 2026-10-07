package com.example.uc4;

import java.util.Map;

import com.example.views.MainLayout;

import com.vaadin.flow.component.breadcrumbs.Breadcrumbs;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;

/**
 * UC4 — Parameter-preserving links (tasks, {@code uc4/:projectId/tasks}).
 * <p>
 * Its own template carries {@code :projectId}, so when it appears as an
 * <em>ancestor</em> of a task page the breadcrumb must hand it back exactly
 * that parameter — not the deeper {@code :taskId}.
 */
@Route(value = "uc4/:projectId/tasks", layout = MainLayout.class)
@PageTitle("Tasks")
public class TasksView extends VerticalLayout implements BeforeEnterObserver {

    private final H1 heading = new H1();
    private final VerticalLayout taskLinks = new VerticalLayout();

    public TasksView() {
        setPadding(false);
        add(new Breadcrumbs());
        add(heading);
        add(new Paragraph("Each task link below opens a task page nested two "
                + "levels under Projects. The breadcrumb there keeps both the "
                + "Project and Tasks links pinned to this project."));
        taskLinks.setPadding(false);
        add(taskLinks);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RouteParameters parameters = event.getRouteParameters();
        String projectId = parameters.get("projectId").orElse("?");
        heading.setText("Tasks — " + ProjectData.projectName(projectId));

        taskLinks.removeAll();
        for (String taskId : ProjectData.taskIds()) {
            taskLinks.add(new RouterLink(ProjectData.taskName(taskId),
                    TaskDetailView.class, new RouteParameters(
                            Map.of("projectId", projectId, "taskId", taskId))));
        }
    }
}
