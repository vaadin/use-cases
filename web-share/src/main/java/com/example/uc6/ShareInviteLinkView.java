package com.example.uc6;

import java.security.SecureRandom;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.webshare.ShareContent;
import com.vaadin.flow.component.webshare.WebShare;
import com.vaadin.flow.component.webshare.WebShareSupport;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC6 — Share invite link.
 * <p>
 * An "invite a friend" flow: <em>Generate invite</em> makes a random
 * 8-character code on the server and builds a join URL from it. The URL is
 * shown in a read-only field, and <em>Share invite</em> is bound once with
 * {@link WebShare#onClick} to that field, so each click shares the most recent
 * URL. The field is the carrier because {@link ShareContent} takes a fixed
 * string or a field, not a signal (API-GAPS.md #4). The demo does not store the
 * codes; a real app would save them to resolve the invite later.
 * <p>
 * <em>Share invite</em> is enabled only when the browser supports
 * {@code navigator.share} and a code has been generated.
 */
@Route(value = "uc6", layout = MainLayout.class)
@PageTitle(ShareInviteLinkView.TITLE)
@UseCaseDescription("Sharing a freshly generated invite link")
@Menu(order = 6, title = ShareInviteLinkView.TITLE)
@StyleSheet("uc6.css")
public class ShareInviteLinkView extends VerticalLayout {

    static final String TITLE = "UC6 — Share invite link";

    private static final SecureRandom RNG = new SecureRandom();
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final ValueSignal<String> inviteUrl = new ValueSignal<>("");

    public ShareInviteLinkView() {
        addClassName("uc6-view");
        add(new H1(TITLE));
        add(new Paragraph("Click Generate invite to make a new join code on "
                + "the server. Share invite then hands the join link to the "
                + "native share sheet, so you can send it by chat, mail or "
                + "AirDrop. Each click on Generate invite makes a new code."));

        TextField urlField = new TextField("Invite link");
        urlField.setReadOnly(true);
        urlField.setPlaceholder("Generate an invite first");
        urlField.addClassName("uc6-url");
        urlField.bindValue(inviteUrl, inviteUrl::set);

        Button generateButton = new Button("Generate invite",
                VaadinIcon.REFRESH.create(),
                e -> inviteUrl.set(inviteUrl(generateCode())));
        generateButton.addThemeVariants(ButtonVariant.PRIMARY);

        Button shareButton = new Button("Share invite",
                VaadinIcon.SHARE.create());
        Signal<WebShareSupport> support = WebShare.supportSignal();
        shareButton.bindEnabled(
                Signal.computed(() -> support.get() == WebShareSupport.SUPPORTED
                        && !inviteUrl.get().isEmpty()));
        // Bound once to the field: each click shares the URL it holds then.
        WebShare.onClick(shareButton).share(
                ShareContent.create().title("Join me on the demo")
                        .url(urlField),
                () -> Notification.show("Shared invite link", 2500,
                        Notification.Position.BOTTOM_START),
                err -> Notification.show("Share failed: " + err.message(), 2500,
                        Notification.Position.BOTTOM_START));

        Div buttons = new Div(generateButton, shareButton);
        buttons.addClassName("uc6-buttons");
        add(urlField, buttons);
    }

    private static String generateCode() {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(CHARS.charAt(RNG.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private static String inviteUrl(String code) {
        return "https://example.com/join/" + code;
    }
}
