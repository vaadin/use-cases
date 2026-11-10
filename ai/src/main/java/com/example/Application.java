package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.aura.Aura;

@SpringBootApplication
@EnableScheduling
@StyleSheet(Aura.STYLESHEET)
@StyleSheet("styles.css")
// AI answers stream in token by token, and UC6 polls a render job: both
// update the page without a user action.
@Push
@PWA(name = "AI in Business Apps Use Cases", shortName = "AI")
public class Application implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
