package com.example.uc4;

import com.vaadin.flow.router.PageTitleContext;
import com.vaadin.flow.router.PageTitleGenerator;

/**
 * Instance-free dynamic title for {@link ProjectView}. Because the breadcrumb
 * resolves every crumb's label with the {@code RouteParameters} of
 * <em>that</em> crumb ({@code getRouteHierarchy} carries the per-ancestor
 * subset), a dynamic <strong>ancestor</strong> label is possible: the Project
 * crumb reads "Project Apollo" from the {@code :projectId} it inherited, not a
 * static "Project".
 */
public class ProjectTitleGenerator implements PageTitleGenerator {

    @Override
    public String generatePageTitle(PageTitleContext context) {
        String projectId = context.routeParameters().get("projectId")
                .orElse("?");
        return ProjectData.projectName(projectId);
    }
}
