package com.example.usecase29;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = UseCase29View.class)
@WithMockUser
class UseCase29ViewTest extends SpringBrowserlessTest {

    @Test
    void initialRenderHasEmptyAuditLog() {
        navigate(UseCase29View.class);
        runPendingSignalsTasks();

        UseCase29View view = (UseCase29View) getCurrentView();
        assertEquals(0, view.auditLog.peek().size(),
                "Audit log should be empty before any edit");
    }

    @Test
    void editingProfileAppendsAuditEntryStampedWithCurrentAdmin() {
        navigate(UseCase29View.class);
        runPendingSignalsTasks();

        UseCase29View view = (UseCase29View) getCurrentView();

        TextField nameField = $view(TextField.class).all().stream()
                .filter(f -> "Display name".equals(f.getLabel())).findFirst()
                .orElseThrow();
        test(nameField).setValue("Bob Brown II");
        runPendingSignalsTasks();

        assertEquals(1, view.auditLog.peek().size(),
                "Edit should produce one audit entry");
        assertEquals("alice", view.auditLog.peek().get(0).peek().admin(),
                "Initial admin alice should be on the entry");
    }

    @Test
    @SuppressWarnings("unchecked")
    void switchingAdminAloneDoesNotAppendAudit() {
        navigate(UseCase29View.class);
        runPendingSignalsTasks();

        UseCase29View view = (UseCase29View) getCurrentView();
        Select<String> adminSelect = (Select<String>) $view(Select.class)
                .single();

        test(adminSelect).selectItem("bob");
        runPendingSignalsTasks();

        assertEquals(0, view.auditLog.peek().size(),
                "Switching admin alone must NOT replay history"
                        + " — untracked admin read must not retrigger the save effect");
    }

    @Test
    @SuppressWarnings("unchecked")
    void editAfterAdminSwitchUsesNewAdmin() {
        navigate(UseCase29View.class);
        runPendingSignalsTasks();

        UseCase29View view = (UseCase29View) getCurrentView();

        // First edit as alice
        TextField nameField = $view(TextField.class).all().stream()
                .filter(f -> "Display name".equals(f.getLabel())).findFirst()
                .orElseThrow();
        test(nameField).setValue("Edit 1");
        runPendingSignalsTasks();

        // Hand over to carol
        Select<String> adminSelect = (Select<String>) $view(Select.class)
                .single();
        test(adminSelect).selectItem("carol");
        runPendingSignalsTasks();

        // Second edit
        test(nameField).setValue("Edit 2");
        runPendingSignalsTasks();

        assertEquals(2, view.auditLog.peek().size());
        // List is reverse-chronological (insertFirst), so position 0 is newest
        assertEquals("carol", view.auditLog.peek().get(0).peek().admin(),
                "Newer edit must be attributed to carol");
        assertEquals("alice", view.auditLog.peek().get(1).peek().admin(),
                "Earlier edit must remain attributed to alice");
    }

    @Test
    void unboundShipperIsInitializedOnAttach() {
        navigate(UseCase29View.class);
        runPendingSignalsTasks();

        UseCase29View view = (UseCase29View) getCurrentView();
        assertTrue(view.shipperRuns.get() >= 1,
                "Unbound shipper should have done its initial run");
        assertEquals(0, view.shippedToRemote.peek(),
                "Nothing shipped yet — audit log is empty");
    }
}
