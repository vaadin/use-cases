package com.example.uc4;

import com.vaadin.flow.router.PageTitleContext;
import com.vaadin.flow.router.PageTitleGenerator;

/**
 * Instance-free dynamic title for {@link TaskDetailView}: resolves the task
 * name from the {@code :taskId} route parameter, so the breadcrumb leaf is the
 * real task rather than a static "Task".
 */
public class TaskTitleGenerator implements PageTitleGenerator {

    @Override
    public String generatePageTitle(PageTitleContext context) {
        String taskId = context.routeParameters().get("taskId").orElse("?");
        return ProjectData.taskName(taskId);
    }
}
