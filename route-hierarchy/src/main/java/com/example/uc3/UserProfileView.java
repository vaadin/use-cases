package com.example.uc3;

import com.example.views.MainLayout;

import com.vaadin.flow.component.breadcrumbs.Breadcrumbs;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.DynamicPageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC3 — Dynamic leaf label (profile, {@code uc3/:userId}).
 * <p>
 * The breadcrumb leaf shows the resolved person name rather than a static title
 * — and it does so <em>without</em> a view instance. The label comes from
 * {@link UserProfileTitleGenerator}, declared via {@code @DynamicPageTitle};
 * the breadcrumb resolves it through
 * {@code MenuRegistry.getTitle(class, params)} purely from the class and the
 * {@code :userId} parameter. The parent {@code uc3} is found by stripping the
 * {@code :userId} segment.
 */
@Route(value = "uc3/:userId", layout = MainLayout.class)
@DynamicPageTitle(UserProfileTitleGenerator.class)
public class UserProfileView extends VerticalLayout
        implements BeforeEnterObserver {

    private final H1 heading = new H1();

    public UserProfileView() {
        add(new Breadcrumbs());
        add(heading);
        add(new Paragraph(
                "The breadcrumb leaf above matches this page's H1 — both are "
                        + "the person's name resolved from the :userId. The "
                        + "breadcrumb gets it from a PageTitleGenerator (no view "
                        + "instance, no HasDynamicTitle), so the same dynamic "
                        + "label also works when this view is only an ancestor "
                        + "of a deeper page. getRouteHierarchy resolves the Users "
                        + "ancestor by URL-prefix walking."));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String userId = event.getRouteParameters().get("userId").orElse("?");
        heading.setText(Directory.nameOf(userId));
    }
}
