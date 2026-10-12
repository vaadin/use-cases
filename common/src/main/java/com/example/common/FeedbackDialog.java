package com.example.common;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.example.common.SlackFeedback.Feedback;
import com.example.common.SlackFeedback.Kind;
import com.example.common.SlackFeedback.UseCase;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

/**
 * Asks for feedback on the use case that is open and posts it with
 * {@link SlackFeedback}. Opened from the "Feedback" button of
 * {@link BaseMainLayout}.
 */
class FeedbackDialog extends Dialog {

    private static final Logger LOGGER = Logger
            .getLogger(FeedbackDialog.class.getName());

    final RadioButtonGroup<Kind> kind = new RadioButtonGroup<>(
            "What is it about?");
    final TextArea message = new TextArea("Your feedback");
    final TextField contact = new TextField("Contact (optional)");
    final Button send = new Button("Send");

    FeedbackDialog(SlackFeedback slack, UseCase useCase) {
        setHeaderTitle("Feedback on " + useCase.title());
        setWidth("min(32em, 100vw)");

        kind.setItems(Kind.values());
        kind.setItemLabelGenerator(Kind::label);
        kind.setValue(Kind.WRONG);

        message.setWidthFull();
        message.setMinHeight("8em");
        message.setMaxLength(SlackFeedback.MAX_MESSAGE_LENGTH);
        message.setRequired(true);
        message.setValueChangeMode(ValueChangeMode.EAGER);

        contact.setWidthFull();
        contact.setMaxLength(SlackFeedback.MAX_CONTACT_LENGTH);
        contact.setHelperText(
                "Email or GitHub handle, if you would like an answer");

        VerticalLayout content = new VerticalLayout(new Paragraph(
                "Is this use case wrong, could it be better, or is something "
                        + "missing? Tell us, it goes straight to the team."),
                kind, message, contact);
        content.setPadding(false);
        add(content);

        send.addThemeVariants(ButtonVariant.PRIMARY);
        send.setEnabled(false);
        message.addValueChangeListener(e -> send.setEnabled(
                !e.getValue().isBlank() && kind.getValue() != null));
        send.addClickListener(e -> send(slack, new Feedback(useCase,
                kind.getValue(), message.getValue(), contact.getValue())));
        getFooter().add(new Button("Cancel", e -> close()), send);
    }

    private void send(SlackFeedback slack, Feedback feedback) {
        try {
            slack.send(feedback);
            close();
            Notification
                    .show("Thank you for your feedback!", 3000,
                            Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.SUCCESS);
        } catch (IOException e) {
            fail(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(e);
        }
    }

    private static void fail(Exception e) {
        LOGGER.log(Level.WARNING, "Could not send feedback", e);
        Notification
                .show("Sending the feedback failed, please try again.", 5000,
                        Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.ERROR);
    }
}
