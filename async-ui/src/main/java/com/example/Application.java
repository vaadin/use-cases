package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.aura.Aura;

@SpringBootApplication
// Provides the TaskScheduler the simulated backend and the live feed use.
@EnableScheduling
@StyleSheet(Aura.STYLESHEET)
@StyleSheet("styles.css")
// Every result in this module arrives after the request that asked for it has
// returned, so it can only reach the browser through server push.
@Push
public class Application implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
