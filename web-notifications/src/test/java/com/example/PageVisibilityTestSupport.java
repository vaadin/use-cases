package com.example;

import java.lang.reflect.Method;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.page.PageVisibility;

/**
 * Shared helper for browserless tests that need to drive
 * {@link Page#pageVisibilitySignal()} transitions. The setter on Page is
 * package-private (only the JS bridge calls it in production), so tests reach
 * through reflection.
 */
public final class PageVisibilityTestSupport {

    private PageVisibilityTestSupport() {
    }

    public static void setPageVisibility(PageVisibility state) {
        try {
            Page page = UI.getCurrent().getPage();
            Method setter = Page.class.getDeclaredMethod("setPageVisibility",
                    String.class);
            setter.setAccessible(true);
            setter.invoke(page, state.name());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Failed to invoke Page#setPageVisibility reflectively", e);
        }
    }
}
