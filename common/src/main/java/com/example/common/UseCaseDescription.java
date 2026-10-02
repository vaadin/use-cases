package com.example.common;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The problem a use-case view helps solve, phrased from the developer's point
 * of view. Keeps the {@code @Menu} title short enough for the side navigation
 * while still saying what the view shows: {@link BaseMainLayout} uses it as the
 * nav item's tooltip and {@link BaseHomeView#addMenuCards()} as the home card's
 * description, so both read the same text.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface UseCaseDescription {

    String value();
}
