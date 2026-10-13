package com.example.uc5;

import java.util.HashMap;
import java.util.Map;

import com.example.common.UseCaseDescription;
import com.example.documents.SampleDocuments;
import com.example.documents.SampleDocuments.ExpenseClaim;
import com.example.pdfviewer.PdfViewer;
import com.example.pdfviewer.PdfViewer.Zoom;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC5 — Check the claim against the receipt.
 * <p>
 * An approver compares what an expense claim says with the receipt that comes
 * with it. The receipt sits next to the claim, fitted into the viewer with
 * {@code Zoom.PAGE_FIT}, with the sidebar closed and the toolbar collapsed
 * behind the file name, so the small receipt gets all the room. On a phone the
 * two stack.
 * <p>
 * The first claim asks for more than the receipt shows; the point of the view
 * is that the approver can see that without leaving the form.
 */
@Route(value = "uc5", layout = MainLayout.class)
@PageTitle("UC5 — Check against the receipt")
@UseCaseDescription("Comparing an expense claim with its receipt side by side")
@Menu(order = 5, title = "UC5 — Check against the receipt")
@StyleSheet("uc5.css")
public class CheckReceiptView extends VerticalLayout {

    private final Select<ExpenseClaim> claims = new Select<>();
    private final TextField employee = readOnly("Employee");
    private final TextField merchant = readOnly("Merchant");
    private final TextField date = readOnly("Date");
    private final TextField amount = readOnly("Claimed amount");
    private final Span decision = new Span();
    private final PdfViewer viewer = new PdfViewer();
    private final Map<String, String> decisions = new HashMap<>();

    public CheckReceiptView() {
        addClassName("uc5-view");
        add(new H1("UC5 — Check against the receipt"));
        add(new Paragraph("Approve or send back each expense claim. The "
                + "receipt is next to the form, so you can compare the "
                + "amounts. Look closely at the first claim."));

        claims.setLabel("Claim");
        claims.setWidthFull();
        claims.setItems(SampleDocuments.EXPENSE_CLAIMS);
        claims.setItemLabelGenerator(
                claim -> claim.id() + " — " + claim.employee());
        claims.addValueChangeListener(event -> show(event.getValue()));

        Button approve = new Button("Approve", event -> decide("Approved"));
        approve.addThemeVariants(ButtonVariant.SUCCESS);
        Button sendBack = new Button("Send back", event -> decide(
                "Sent back to " + claims.getValue().employee()));
        sendBack.addThemeVariants(ButtonVariant.ERROR);
        decision.addClassName("uc5-decision");

        FormLayout form = new FormLayout(employee, merchant, date, amount);
        Div claim = new Div(claims, form,
                new HorizontalLayout(approve, sendBack), decision);
        claim.addClassName("uc5-claim");

        viewer.addClassName("uc5-viewer");
        viewer.setZoom(Zoom.PAGE_FIT);
        viewer.setSidebarOpened(false);
        viewer.setFileNameVisible(true);
        viewer.setToolbarCollapsed(true);

        Div layout = new Div(claim, viewer);
        layout.addClassNames("uc5-layout", "side-by-side");
        add(layout);
        claims.setValue(SampleDocuments.EXPENSE_CLAIMS.getFirst());
    }

    private void show(ExpenseClaim claim) {
        employee.setValue(claim.employee());
        merchant.setValue(claim.merchant());
        date.setValue(claim.date());
        amount.setValue(SampleDocuments.money(claim.claimed()));
        decision.setText(decisions.getOrDefault(claim.id(),
                "Waiting for " + "your decision"));
        viewer.setSrc(claim.receipt().handler());
    }

    private void decide(String text) {
        decisions.put(claims.getValue().id(), text);
        decision.setText(text);
    }

    private static TextField readOnly(String label) {
        TextField field = new TextField(label);
        field.setReadOnly(true);
        return field;
    }
}
