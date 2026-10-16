package com.example.uc9;

import com.example.common.BuiltWith;
import com.example.common.UseCaseDescription;
import com.example.components.RatingStars;
import com.example.components.SignaturePad;
import com.example.views.MainLayout;
import com.example.views.Sample;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC9 — Testing the components.
 * <p>
 * A component that others use should come with the means to test it:
 * <ul>
 * <li>a browserless tester ({@code RatingStarsTester}, registered for the
 * component with {@code @Tests}), so {@code test(rating).rate(4)} works in any
 * view test;</li>
 * <li>a TestBench element class ({@code RatingStarsElement}) for end-to-end
 * tests;</li>
 * <li>a real-browser check for what only exists on the client, such as drawing
 * on the signature pad ({@code SignaturePadPlaywrightIT}).</li>
 * </ul>
 * This view is what those tests open.
 */
@Route(value = "uc9", layout = MainLayout.class)
@PageTitle("UC9 — Testing the components")
@UseCaseDescription("Shipping a browserless tester and a TestBench element with a component")
@Menu(order = 9, title = "UC9 — Testing the components")
public class TestingView extends VerticalLayout {

    private final RatingStars rating = new RatingStars("Test rating");
    private final SignaturePad pad = new SignaturePad();
    private final Span ratingStatus = new Span("0");
    private final Span padStatus = new Span("blank");

    public TestingView() {
        add(new H1("UC9 — Testing the components"));
        add(new Paragraph("The browserless, TestBench and Playwright tests "
                + "of the components run against this page."));

        ratingStatus.setId("rating-status");
        padStatus.setId("pad-status");
        rating.addValueChangeListener(
                e -> ratingStatus.setText(String.valueOf(e.getValue())));
        pad.addValueChangeListener(e -> padStatus
                .setText(e.getValue().isEmpty() ? "blank" : "signed"));

        add(new Sample("Under test", rating, ratingStatus, pad, padStatus),
                new Div(new Span("Tests: components/RatingStarsTester, "
                        + "uc9/TestingViewTest (browserless), "
                        + "uc9/RatingStarsIT (TestBench), "
                        + "uc9/SignaturePadPlaywrightIT (Playwright). "
                        + "Run the browser tests with "
                        + "mvn -pl custom-components -am verify -Pit")));
        add(new BuiltWith("the component's test support",
                "Ships with the component, for the applications that use it")
                .point("RatingStarsTester, registered with @Tests, lets any browserless test write test(...).rate(4).")
                .point("RatingStarsElement, a TestBench element for <rating-stars>, lets end-to-end tests do the same in a real browser.")
                .point("Both hide how the stars are built, so the component can change without breaking its users' tests.")
                .testJava("components/RatingStarsTester")
                .testJava("components/RatingStarsElement"));
    }

    // Package-private test seams.
    RatingStars rating() {
        return rating;
    }

    String ratingStatus() {
        return ratingStatus.getText();
    }
}
