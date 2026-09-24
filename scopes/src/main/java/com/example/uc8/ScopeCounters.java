package com.example.uc8;

import com.example.MissingAPI.BrowserTabScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.SessionScope;

import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;

/**
 * One {@link VisitCounter} per scope for the UC8 playground.
 */
@Configuration(proxyBeanMethods = false)
public class ScopeCounters {

    @Bean
    public VisitCounter applicationCounter() {
        return new VisitCounter();
    }

    @Bean
    @SessionScope
    public VisitCounter httpSessionCounter() {
        return new VisitCounter();
    }

    @Bean
    @VaadinSessionScope
    public VisitCounter vaadinSessionCounter() {
        return new VisitCounter();
    }

    @Bean
    @BrowserTabScope
    public VisitCounter browserTabCounter() {
        return new VisitCounter();
    }

    @Bean
    @UIScope
    public VisitCounter uiCounter() {
        return new VisitCounter();
    }

    /**
     * Without {@code @RouteScopeOwner} the owner is the route target that first
     * asked for the bean — here the playground view itself.
     */
    @Bean
    @RouteScope
    public VisitCounter routeCounter() {
        return new VisitCounter();
    }
}
