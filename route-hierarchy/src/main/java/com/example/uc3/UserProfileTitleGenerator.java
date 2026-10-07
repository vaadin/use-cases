package com.example.uc3;

import com.vaadin.flow.router.PageTitleContext;
import com.vaadin.flow.router.PageTitleGenerator;

/**
 * Instance-free dynamic title for {@link UserProfileView}. It resolves the
 * person's name straight from the {@code :userId} route parameter, so the
 * breadcrumb leaf shows "Ada Lovelace" without the breadcrumb builder ever
 * instantiating the view.
 */
public class UserProfileTitleGenerator implements PageTitleGenerator {

    @Override
    public String generatePageTitle(PageTitleContext context) {
        String userId = context.routeParameters().get("userId").orElse("?");
        return Directory.nameOf(userId);
    }
}
