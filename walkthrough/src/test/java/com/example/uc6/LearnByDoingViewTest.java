package com.example.uc6;

import com.example.TourTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.ComponentQuery;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = LearnByDoingView.class)
class LearnByDoingViewTest extends TourTestSupport {

    @Test
    void eachStepWaitsForTheUserToDoIt() {
        navigate(LearnByDoingView.class);
        clickInView("Start the tutorial");

        assertEquals("Create your first task", stepTitle());
        assertFalse(stepHasButton("Next"), "the step waits for a click");

        clickInView("New task");
        assertEquals("Give it a title", stepTitle());
        assertFalse(stepHasButton("Next"));

        test(findInView(TextField.class).single()).setValue("Call the bank");
        assertEquals("Save it", stepTitle());

        clickInView("Save");
        assertEquals("That's it", stepTitle());
        assertEquals("Call the bank", tasks().single().getText());

        clickInStep("Done");
        assertEquals("Finished", status());
    }

    @Test
    void anActionAtAnotherStepDoesNotMoveTheTour() {
        navigate(LearnByDoingView.class);
        clickInView("Start the tutorial");
        clickInView("New task");
        clickInStep("Back");

        // Saving is the third step's action, not the first's.
        test(findInView(TextField.class).single()).setValue("Call the bank");
        clickInView("Save");

        assertEquals("Create your first task", stepTitle());
    }

    @Test
    void withoutTheTutorialTheAppWorksAsUsual() {
        navigate(LearnByDoingView.class);

        clickInView("New task");
        test(findInView(TextField.class).single()).setValue("Call the bank");
        clickInView("Save");

        assertTrue(openPopovers().isEmpty());
        assertTrue(tasks().exists());
        assertEquals("Not started", status());
    }

    @Test
    void savingWithoutATitleAsksForOne() {
        navigate(LearnByDoingView.class);
        clickInView("New task");

        test(findInView(Button.class).withText("Save").single()).click();

        assertTrue(findInView(TextField.class).single().isInvalid());
        assertFalse(tasks().exists());
    }

    private ComponentQuery<ListItem> tasks() {
        return find(ListItem.class).from(findInView(UnorderedList.class)
                .withClassName("uc6-tasks").single());
    }
}
