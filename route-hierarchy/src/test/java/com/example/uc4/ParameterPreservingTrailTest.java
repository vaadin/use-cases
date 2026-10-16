package com.example.uc4;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.breadcrumbs.Breadcrumbs;
import com.vaadin.flow.component.breadcrumbs.BreadcrumbsTester;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = ProjectsView.class)
class ParameterPreservingTrailTest extends SpringBrowserlessTest {

    @Test
    void deepLeafBuildsFullFourLevelTrail() {
        navigate(TaskDetailView.class,
                Map.of("projectId", "apollo", "taskId", "2"));

        // The Project ancestor crumb is itself dynamic ("Project Apollo" from
        // the :projectId); the leaf uses the task's PageTitleGenerator label.
        assertEquals(List.of("Projects", "Project Apollo", "Tasks",
                "Wire the backend"), crumbs());
    }

    @Test
    void ancestorLinksCarryOnlyTheParametersTheirTemplateNeeds() {
        navigate(TaskDetailView.class,
                Map.of("projectId", "apollo", "taskId", "2"));

        // Each ancestor link resolves only the parameters its own template
        // needs: the root drops :projectId, the middle links keep it, and none
        // carries :taskId. The current page has no link, so its path is null.
        assertEquals(
                Arrays.asList("uc4", "uc4/apollo", "uc4/apollo/tasks", null),
                breadcrumbs().getItemPaths());
    }

    @Test
    void midLevelProjectShowsTwoCrumbs() {
        navigate(ProjectView.class, Map.of("projectId", "zephyr"));
        assertEquals(List.of("Projects", "Project Zephyr"), crumbs());
    }

    private List<String> crumbs() {
        return breadcrumbs().getItemTexts();
    }

    private BreadcrumbsTester<Breadcrumbs> breadcrumbs() {
        return test(find(Breadcrumbs.class).single());
    }

}
