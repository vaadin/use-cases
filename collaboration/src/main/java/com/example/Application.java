package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.aura.Aura;

/**
 * Every use case in this module shows one user's writes arriving in another
 * user's UI, so server-initiated updates have to reach the browser without a
 * round trip: {@code @Push} is not optional here.
 */
@SpringBootApplication
@StyleSheet(Aura.STYLESHEET)
@StyleSheet("styles.css")
@Push
public class Application implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
