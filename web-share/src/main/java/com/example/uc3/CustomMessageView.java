package com.example.uc3;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.webshare.ShareContent;
import com.vaadin.flow.component.webshare.WebShare;
import com.vaadin.flow.component.webshare.WebShareSupport;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC3 — Share a custom message.
 * <p>
 * A small form lets the user fill in the three payload fields ({@code title},
 * {@code text}, {@code url}) and shows a live JSON preview of the data that
 * will be handed to the native share sheet. The share is bound once with
 * {@link WebShare#onClick} to the three fields, so each click shares whatever
 * the form holds at that moment. An empty field is still sent, as an empty
 * string; the browser resolves an empty URL against the page address, so it
 * shares this page (API-GAPS.md #5).
 */
@Route(value = "uc3", layout = MainLayout.class)
@PageTitle(CustomMessageView.TITLE)
@UseCaseDescription("Sharing a custom title, text and URL with a live preview")
@Menu(order = 3, title = CustomMessageView.TITLE)
@StyleSheet("uc3.css")
public class CustomMessageView extends VerticalLayout {

    static final String TITLE = "UC3 — Share a custom message";

    private static final ObjectMapper JSON = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT).build();

    private final TextField titleField = new TextField("Title");
    private final TextArea textField = new TextArea("Text");
    private final TextField urlField = new TextField("URL");
    private final Div preview = new Div();
    private final Button shareButton = new Button("Share",
            VaadinIcon.SHARE.create());

    public CustomMessageView() {
        addClassName("uc3-view");
        add(new H1(TITLE));
        add(new Paragraph("Fill in the three fields and click Share. The "
                + "preview shows what the button hands to the share sheet: "
                + "the values of the fields at the moment of the click. An "
                + "empty field is sent as an empty string. The browser "
                + "reads an empty URL as the address of this page."));

        titleField.setPlaceholder("e.g. Look at this!");
        titleField.setValue("Look at this!");
        textField.setPlaceholder("Optional body text");
        textField.setValue("Found this while reading docs.");
        urlField.setPlaceholder("https://example.com/article/42");
        urlField.setValue("https://vaadin.com/");
        titleField.setWidthFull();
        textField.setWidthFull();
        urlField.setWidthFull();

        preview.addClassName("uc3-preview");
        shareButton.addThemeVariants(ButtonVariant.PRIMARY);

        titleField.setValueChangeMode(ValueChangeMode.EAGER);
        textField.setValueChangeMode(ValueChangeMode.EAGER);
        urlField.setValueChangeMode(ValueChangeMode.EAGER);
        titleField.addValueChangeListener(e -> updatePreview());
        textField.addValueChangeListener(e -> updatePreview());
        urlField.addValueChangeListener(e -> updatePreview());

        add(titleField, textField, urlField, new H2("Payload preview"), preview,
                shareButton);
        updatePreview();

        shareButton.bindEnabled(WebShare.supportSignal()
                .map(s -> s == WebShareSupport.SUPPORTED));

        // Bound once: each click reads the field values in the browser and
        // shares whatever the form holds at that moment.
        WebShare.onClick(shareButton).share(
                ShareContent.create().title(titleField).text(textField)
                        .url(urlField),
                () -> Notification.show("Shared with the previewed payload",
                        2000, Notification.Position.BOTTOM_START),
                err -> Notification.show("Share failed: " + err.message(), 2000,
                        Notification.Position.BOTTOM_START));
    }

    private void updatePreview() {
        try {
            ObjectNode node = JSON.createObjectNode();
            node.put("title", titleField.getValue());
            node.put("text", textField.getValue());
            node.put("url", urlField.getValue());
            preview.setText(JSON.writeValueAsString(node));
        } catch (Exception ex) {
            preview.setText("(preview error: " + ex.getMessage() + ")");
        }
    }
}
