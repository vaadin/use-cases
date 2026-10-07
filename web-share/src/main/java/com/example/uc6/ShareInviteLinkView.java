package com.example.uc6;

import java.security.SecureRandom;

import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.webshare.ShareContent;
import com.vaadin.flow.component.webshare.WebShare;
import com.vaadin.flow.component.webshare.WebShareSupport;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;

/**
 * UC6 — Share a generated invite link.
 * <p>
 * Models the "invite a friend" flow: clicking <em>Generate invite</em> produces
 * a fresh short code (so the server can later resolve invitee identities) and
 * renders it as a join URL. The <em>Share invite</em> button is bound once via
 * {@link WebShare#onClick} to the live invite URL, so each click hands the most
 * recently generated URL to the native share sheet. A new invite each click
 * gives a clean cross-session trail and proves the demo is not stuck on a
 * single hard-coded URL.
 */
@Route(value = "uc6", layout = MainLayout.class)
@Menu(order = 6, title = "UC6 — Share invite link")
@StyleSheet("uc6.css")
public class ShareInviteLinkView extends VerticalLayout {

    private static final SecureRandom RNG = new SecureRandom();
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final Span codeLabel = new Span("—");
    private final Anchor linkAnchor = new Anchor("", "—");
    // Non-visible holder for the live invite URL; the share binding reads its
    // value on each click so the most recently generated URL is shared.
    private final TextField urlHolder = new TextField();
    private final Button shareButton = new Button("Share invite",
            VaadinIcon.SHARE.create());
    private final Button generateButton = new Button("Generate invite",
            VaadinIcon.REFRESH.create());

    private @Nullable String currentCode;
    private boolean shareSupported;

    public ShareInviteLinkView() {
        addClassName("uc6-view");
        add(new H1("UC6 — Share an invite link"));
        add(new Paragraph("Generates a fresh join code (server-side) and "
                + "hands a https://example.com/join/<code> URL to the "
                + "native share sheet, so the invitee can tap a one-off "
                + "link in WhatsApp, mail, or AirDrop."));

        generateButton.addThemeVariants(ButtonVariant.PRIMARY);
        codeLabel.addClassName("invite-code");
        linkAnchor.setTarget("_blank");
        urlHolder.setVisible(false);
        add(urlHolder);

        HorizontalLayout codeRow = new HorizontalLayout(new Span("Code:"),
                codeLabel);
        codeRow.setAlignItems(Alignment.CENTER);
        HorizontalLayout linkRow = new HorizontalLayout(new Span("URL:"),
                linkAnchor);
        linkRow.setAlignItems(Alignment.CENTER);

        add(new H2("Invite"), codeRow, linkRow,
                new Div(generateButton, shareButton));

        generateButton.addClickListener(e -> regenerate());
        // Share button stays disabled until both (1) the support signal
        // reports SUPPORTED and (2) the user has clicked Generate at least
        // once.
        shareButton.setEnabled(false);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        Signal<WebShareSupport> support = WebShare.supportSignal();

        Signal.effect(this, () -> {
            shareSupported = support.get() == WebShareSupport.SUPPORTED;
            refreshShareButtonState();
        });

        // Bind the share once to the live invite URL held in urlHolder; the
        // button stays disabled until a code has been generated, so a click
        // can only fire with a real URL.
        WebShare.onClick(shareButton).share(
                ShareContent.create().title("Join me on the demo")
                        .url(urlHolder),
                () -> Notification.show("Shared invite link", 2500,
                        Notification.Position.BOTTOM_START),
                err -> Notification.show("Share failed: " + err.message(), 2500,
                        Notification.Position.BOTTOM_START));
    }

    private void regenerate() {
        currentCode = generateCode();
        codeLabel.setText(currentCode);
        String url = inviteUrl(currentCode);
        linkAnchor.setHref(url);
        linkAnchor.setText(url);
        urlHolder.setValue(url);
        refreshShareButtonState();
    }

    private void refreshShareButtonState() {
        shareButton.setEnabled(shareSupported && currentCode != null);
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

    // Package-private accessor for tests.
    @Nullable
    String currentCode() {
        return currentCode;
    }
}
