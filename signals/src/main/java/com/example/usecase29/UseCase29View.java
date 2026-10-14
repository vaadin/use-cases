package com.example.usecase29;

import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;

import com.example.views.MainLayout;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Admin tool for editing a user's profile. Edits auto-save on every field
 * change; each save creates an audit entry stamped with the current admin.
 * <p>
 * The current admin is read via {@code Signal.untracked} inside the auto-save
 * effect: that way, switching admins ("admin handoff during a shift change")
 * does not retroactively replay every prior save under the new admin's name.
 * <p>
 * A second {@code Signal.unboundEffect} acts as the "remote audit shipper":
 * whenever new entries land in the local audit log, they're pushed to a remote
 * service (simulated by an in-memory counter). The shipper runs without the
 * session lock so a slow remote call doesn't block the operator's typing.
 */
@PageTitle("Use Case 29: Profile auto-save")
@Route(value = "use-case-29", layout = MainLayout.class)
@Menu(order = 29, title = "UC 29: Profile auto-save")
@AnonymousAllowed
public class UseCase29View extends VerticalLayout {

    final ValueSignal<UserProfile> profile = new ValueSignal<>(new UserProfile(
            "Bob Brown", "bob@example.com", "Backend engineer."));
    final ValueSignal<String> currentAdmin = new ValueSignal<>("alice");
    final ListSignal<AuditEntry> auditLog = new ListSignal<>();
    final ValueSignal<Integer> shippedToRemote = new ValueSignal<>(0);
    final AtomicInteger shipperRuns = new AtomicInteger();

    private final AtomicInteger remoteShipped = new AtomicInteger();
    private boolean firstAutosaveCall = true;

    public UseCase29View() {
        setSpacing(true);
        setPadding(true);

        add(new H2("Use Case 29: Profile auto-save"), new Paragraph(
                "Edits to the profile auto-save and write to an audit log,"
                        + " stamped with the current admin. Switching admins"
                        + " does NOT retroactively change history — the"
                        + " auto-save effect reads the admin via"
                        + " Signal.untracked, so it uses whatever admin is"
                        + " current at save time without subscribing to admin"
                        + " changes."));

        // Admin switcher
        Select<String> adminSelect = new Select<>();
        adminSelect.setLabel("Current admin (impersonation)");
        adminSelect.setItems("alice", "bob", "carol");
        adminSelect.bindValue(currentAdmin, currentAdmin::set);

        // Profile form
        TextField nameField = new TextField("Display name");
        nameField.bindValue(profile.map(UserProfile::name),
                profile.updater(UserProfile::withName));
        TextField emailField = new TextField("Email");
        emailField.bindValue(profile.map(UserProfile::email),
                profile.updater(UserProfile::withEmail));
        TextArea bioField = new TextArea("Bio");
        bioField.bindValue(profile.map(UserProfile::bio),
                profile.updater(UserProfile::withBio));

        HorizontalLayout topRow = new HorizontalLayout(adminSelect);
        topRow.setWidthFull();

        VerticalLayout form = new VerticalLayout(nameField, emailField,
                bioField);
        form.setPadding(false);
        form.setSpacing(false);
        form.getStyle().set("flex", "1");

        Div sidePanel = buildSidePanel();
        sidePanel.getStyle().set("flex", "1");

        HorizontalLayout columns = new HorizontalLayout(form, sidePanel);
        columns.setWidthFull();

        add(topRow, columns, buildExplanation());

        // Auto-save effect: tracks profile; reads admin untracked.
        Signal.effect(this, () -> {
            UserProfile current = profile.get();
            if (firstAutosaveCall) {
                // Skip the initial run — the form has just been opened.
                firstAutosaveCall = false;
                return;
            }
            String admin = Signal.untracked(currentAdmin::get);
            auditLog.insertFirst(new AuditEntry(LocalTime.now(), admin,
                    "name=" + current.name() + " email=" + current.email()));
        });

        // Remote audit shipper: runs without the session lock. Pushes new
        // audit entries to a remote service (simulated by an in-memory
        // counter).
        Registration shipper = Signal.unboundEffect(() -> {
            shipperRuns.incrementAndGet();
            int currentSize = auditLog.get().size();
            int alreadyShipped = remoteShipped.get();
            if (currentSize > alreadyShipped) {
                remoteShipped.set(currentSize);
                shippedToRemote.set(currentSize);
            }
        });
        addDetachListener(e -> shipper.remove());
    }

    private Div buildSidePanel() {
        Div panel = new Div();
        panel.getStyle().set("padding", "var(--lumo-space-m)")
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "8px");

        H3 saveStats = new H3("Audit");
        saveStats.getStyle().set("margin-top", "0");

        Span saveCount = new Span();
        saveCount.bindText(Signal.computed(
                () -> "Local audit entries: " + auditLog.get().size()));
        saveCount.getStyle().set("display", "block");

        Span shippedCount = new Span();
        shippedCount
                .bindText(shippedToRemote.map(n -> "Shipped to remote: " + n));
        shippedCount.getStyle().set("display", "block").set("color",
                "var(--lumo-secondary-text-color)");

        H3 logHeader = new H3("Recent saves");
        logHeader.getStyle().set("margin-top", "var(--lumo-space-m)");

        Div logList = new Div();
        logList.getStyle().set("font-family", "monospace")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("background-color", "var(--lumo-base-color)")
                .set("padding", "var(--lumo-space-s)")
                .set("border-radius", "4px").set("max-height", "240px")
                .set("overflow-y", "auto");
        logList.bindChildren(auditLog, entry -> {
            Div line = new Div();
            line.bindText(entry.map(a -> a.time().withNano(0) + " — admin "
                    + a.admin() + " — " + a.changeSummary()));
            return line;
        });

        panel.add(saveStats, saveCount, shippedCount, logHeader, logList);
        return panel;
    }

    private Div buildExplanation() {
        Div box = new Div();
        box.getStyle().set("background-color", "var(--lumo-contrast-5pct)")
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "8px");

        H3 title = new H3("Why these signal APIs?");
        title.getStyle().set("margin-top", "0");

        Paragraph p = new Paragraph(
                "Signal.untracked lets the auto-save effect READ the admin"
                        + " identity at save time without depending on it,"
                        + " so a shift handover (admin dropdown change) does"
                        + " not retroactively replay past saves. "
                        + "Signal.unboundEffect runs the remote audit shipper"
                        + " without holding the session lock, so a slow"
                        + " network round-trip wouldn't freeze the operator's"
                        + " typing.");

        box.add(title, p);
        return box;
    }
}
