package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;

@SpringBootApplication
// The theme itself (Aura or Lumo) is not loaded here: an annotation-loaded
// stylesheet cannot be removed again, and UC9 switches between the two at
// runtime. AppearanceSetup adds it to every UI instead.
@StyleSheet("styles.css")
// An appearance change made in one tab is applied to every open tab of the
// session, which only shows up there through server push.
@Push
public class Application implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
