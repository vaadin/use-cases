package com.example.uc4;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.breadcrumbs.Breadcrumbs;
import com.vaadin.flow.component.breadcrumbs.BreadcrumbsItem;
import com.vaadin.flow.component.breadcrumbs.BreadcrumbsTester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = ProjectsView.class)
class ParameterPreservingTrailTest extends SpringBrowserlessTest {

    @Test
    void deepLeafBuildsFullFourLevelTrail() {
        navigate(TaskDetailView.class,
                Map.of("projectId", "apollo", "taskId", "2"));

        // The Project ancestor crumb is itself dynamic ("Project Apollo" from
        // the :projectId); the leaf uses the task's PageTitleGenerator label.
        assertEquals(
                List.of("Projects", "Project Apollo", "Tasks",
                        "Wire the backend"),
                crumbs());
    }

    @Test
    void ancestorLinksCarryOnlyTheParametersTheirTemplateNeeds() {
        navigate(TaskDetailView.class,
                Map.of("projectId", "apollo", "taskId", "2"));

        // getItemTexts() gives only labels; this case is about the link hrefs,
        // so it reads each ancestor's resolved path from BreadcrumbsItem.
        List<String> paths = ancestorLinkPaths();
        String projectsHref = paths.get(0);
        String projectHref = paths.get(1);
        String tasksHref = paths.get(2);

        // Root "uc4" has no parameters at all.
        assertFalse(projectsHref.contains("apollo"),
                "Projects root link must not carry :projectId: "
                        + projectsHref);

        // The two middle links keep :projectId so they stay in this project...
        assertTrue(projectHref.contains("apollo"), projectHref);
        assertTrue(tasksHref.contains("apollo"), tasksHref);

        // ...but neither leaks the deeper :taskId into the ancestor URLs.
        assertFalse(projectHref.endsWith("/2") || projectHref.contains("/2/"),
                "Project link must not carry :taskId: " + projectHref);
        assertTrue(tasksHref.endsWith("tasks"),
                "Tasks link must stop at the tasks segment: " + tasksHref);
    }

    @Test
    void midLevelProjectShowsTwoCrumbs() {
        navigate(ProjectView.class, Map.of("projectId", "zephyr"));
        assertEquals(List.of("Projects", "Project Zephyr"), crumbs());
    }

    private List<String> crumbs() {
        BreadcrumbsTester<Breadcrumbs> tester = test(
                find(Breadcrumbs.class).single());
        return tester.getItemTexts();
    }

    /**
     * The resolved {@code href} of each linked (non-current) crumb, in order.
     * Not available from the tester — see the note in the README about a
     * {@code getItemPaths()} (or similar) addition to {@code BreadcrumbsTester}.
     */
    private List<String> ancestorLinkPaths() {
        return find(Breadcrumbs.class).single().getChildren()
                .filter(BreadcrumbsItem.class::isInstance)
                .map(BreadcrumbsItem.class::cast)
                .map(BreadcrumbsItem::getPath)
                .filter(path -> path != null && !path.isEmpty())
                .toList();
    }
}
