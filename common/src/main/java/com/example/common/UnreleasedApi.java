package com.example.common;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a use-case view that uses API which is not released yet but comes from
 * the feature snapshot of an open pull request. {@link BaseMainLayout} shows a
 * banner above such a view that links to the pull request. Remove the
 * annotation when the module switches back to the mainline snapshot.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface UnreleasedApi {

    /**
     * The URL of the pull request that adds the API, such as
     * {@code https://github.com/vaadin/flow/pull/26495}.
     */
    String value();
}
