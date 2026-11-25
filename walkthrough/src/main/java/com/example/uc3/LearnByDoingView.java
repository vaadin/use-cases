package com.example.uc3;

import java.util.List;

import com.example.common.BuiltWith;
import com.example.common.UseCaseDescription;
import com.example.tour.Tour;
import com.example.tour.TourStep;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC3 — Learn by doing.
 * <p>
 * An interactive tutorial for a task list. The first three steps have no "Next"
 * button: the tour moves on when the user does what the step says — clicks "New
 * task", types a title, clicks "Save". The view's own listeners call
 * {@link Tour#next()} when the tour is at the matching step.
 */
@Route(value = "uc3", layout = MainLayout.class)
@PageTitle("UC3 — Learn by doing")
@UseCaseDescription("A tutorial that moves on when the user does what each step says")
@Menu(order = 3, title = "UC3 — Learn by doing")
public class LearnByDoingView extends VerticalLayout {

    public LearnByDoingView() {
        add(new H1("UC3 — Learn by doing"));
        add(new Paragraph("People learn faster by doing than by reading. "
                + "Start the tutorial: it asks you to create a task, and "
                + "moves on only when you have done each step."));

        Button newTask = new Button("New task", VaadinIcon.PLUS.create());
        newTask.addThemeVariants(ButtonVariant.PRIMARY);
        TextField title = new TextField("Title");
        Button save = new Button("Save");
        Div form = new Div(title, save);
        form.addClassName("uc3-form");
        form.setVisible(false);
        Paragraph empty = new Paragraph("No tasks yet.");
        UnorderedList tasks = new UnorderedList();
        tasks.addClassName("uc3-tasks");
        Div list = new Div(new H2("Your tasks"), empty, tasks);
        list.addClassName("uc3-list");
        Div app = new Div(newTask, form, list);
        app.addClassName("demo-app");

        TourStep clickNew = TourStep
                .of(newTask, "Create your first task", "Click \"New task\".")
                .waitingForAction();
        TourStep typeTitle = TourStep
                .of(title, "Give it a title", "Type a title, then press Enter.")
                .waitingForAction();
        TourStep clickSave = TourStep.of(save, "Save it", "Click \"Save\".")
                .waitingForAction();
        TourStep result = TourStep
                .of(list, "That's it",
                        "Your task is in the list. You are ready to go.")
                .at(PopoverPosition.TOP);
        Tour tour = new Tour(List.of(clickNew, typeTitle, clickSave, result));

        newTask.addClickListener(e -> {
            form.setVisible(true);
            if (tour.isAt(clickNew)) {
                tour.next();
            }
        });
        title.addValueChangeListener(e -> {
            if (!e.getValue().isBlank() && tour.isAt(typeTitle)) {
                tour.next();
            }
        });
        save.addClickListener(e -> {
            if (title.getValue().isBlank()) {
                title.setErrorMessage("Type a title first");
                title.setInvalid(true);
                return;
            }
            title.setInvalid(false);
            tasks.add(new ListItem(title.getValue()));
            empty.setVisible(false);
            title.clear();
            form.setVisible(false);
            if (tour.isAt(clickSave)) {
                tour.next();
            }
        });

        Span status = new Span();
        status.addClassName("tour-status");
        status.bindText(tour.state().map(Tour.State::describe));
        Button start = new Button("Start the tutorial", e -> tour.start());
        Div controls = new Div(new Span("Tutorial:"), status, start);
        controls.addClassName("tour-controls");

        add(controls, app);
        add(new BuiltWith("Steps that wait for the user").point(
                "A step marked as waiting for an action has no \"Next\" button.")
                .point("The listeners the app has anyway — the click on \"New task\", the "
                        + "title's value change, the click on \"Save\" — move the tour on.")
                .point("Tour.isAt(step) keeps them from moving the tour when it is at "
                        + "another step or not running.")
                .java("uc3/LearnByDoingView").java("tour/TourStep"));

        whenAttached(ui -> tour::reset);
    }
}
