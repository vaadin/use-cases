package com.example.uc6;

import java.util.List;

import com.example.MissingAPI;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.tabs.TabSheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = DeferredSectionsView.class)
class DeferredSectionsViewTest extends SpringBrowserlessTest {

    @Test
    void onlyTheSummaryIsBuiltUpFront() {
        DeferredSectionsView view = navigate(DeferredSectionsView.class);

        assertEquals("UC6 — Build sections when needed",
                findInView(H1.class).single().getText());
        assertEquals(List.of(), view.builtSections());
        assertTrue(findInView(Grid.class).all().isEmpty(),
                "the order history grid must not exist before it is needed");
    }

    @Test
    void tabsAndDetailsBuildTheirContentOnFirstUse() {
        DeferredSectionsView view = navigate(DeferredSectionsView.class);
        TabSheet tabs = findInView(TabSheet.class).single();

        tabs.setSelectedIndex(1);
        tabs.setSelectedIndex(0);
        tabs.setSelectedIndex(1);
        assertEquals(List.of("order history"), view.builtSections(),
                "a revisited tab is built only once");
        assertEquals(DeferredSectionsView.HISTORY_ROWS,
                test(findInView(Grid.class).single()).size());

        findInView(Details.class).single().setOpened(true);
        assertEquals(List.of("order history", "audit log"),
                view.builtSections());
    }

    @Test
    void heatmapIsBuiltWhenItScrollsIntoView() {
        DeferredSectionsView view = navigate(DeferredSectionsView.class);
        Div slot = findInView(Div.class).all().stream()
                .filter(div -> div.getClassNames().contains("heatmap-slot"))
                .findFirst().orElseThrow();

        // What the browser's IntersectionObserver sends, twice.
        ComponentUtil.fireEvent(slot,
                new MissingAPI.BecameVisibleEvent(slot, true));
        ComponentUtil.fireEvent(slot,
                new MissingAPI.BecameVisibleEvent(slot, true));

        assertEquals(List.of("heatmap"), view.builtSections());
        assertEquals(DeferredSectionsView.HEATMAP_WEEKS * 7,
                slot.getComponentAt(0).getChildren().count());
    }
}
