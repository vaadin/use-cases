package com.example.uc6;

import com.vaadin.flow.router.PageTitleContext;
import com.vaadin.flow.router.PageTitleGenerator;

/**
 * Instance-free dynamic title for {@link MemberView}: resolves the member name
 * from the {@code :member} route parameter, so the layout-wide breadcrumb leaf
 * shows the person without instantiating the view.
 */
public class MemberTitleGenerator implements PageTitleGenerator {

    @Override
    public String generatePageTitle(PageTitleContext context) {
        String memberId = context.routeParameters().get("member").orElse("?");
        return TeamMembers.nameOf(memberId);
    }
}
