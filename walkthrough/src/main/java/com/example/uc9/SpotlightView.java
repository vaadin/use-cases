package com.example.uc9;

import java.util.List;

import com.example.common.BuiltWith;
import com.example.common.UseCaseDescription;
import com.example.tour.Tour;
import com.example.tour.TourStep;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ScrollIntoViewOption;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC9 — Spotlight.
 * <p>
 * A tour that dims everything but the current target and blocks the rest of the
 * page. Two of its targets are not on screen at first: the app's side
 * navigation, which is in a closed drawer on a phone or when the user closed
 * it, and the "Save changes" button at the end of a long form. The tour opens
 * the drawer (and closes it again on the next step) and scrolls the button into
 * view before it points at them.
 */
@Route(value = "uc9", layout = MainLayout.class)
@PageTitle("UC9 — Spotlight")
@UseCaseDescription("Dimming all but the target, and revealing targets that are off screen")
@Menu(order = 9, title = "UC9 — Spotlight")
public class SpotlightView extends VerticalLayout {

    private final Tour tour = new Tour(List.of()).withSpotlight();
    private final EmailField email = new EmailField("Email");
    private final Button save = new Button("Save changes");
    private @Nullable AppLayout openedDrawer;

    public SpotlightView() {
        add(new H1("UC9 — Spotlight"));
        add(new Paragraph("A spotlight makes sure the user looks at the "
                + "right place. Start the tour: the page dims around each "
                + "target. The first target is the app menu — on a phone, or "
                + "when you closed it, it is hidden in a drawer, which the "
                + "tour opens. The last "
                + "target is at the end of the long form below; the tour "
                + "scrolls to it."));

        Span status = new Span();
        status.addClassName("tour-status");
        status.bindText(tour.state().map(Tour.State::describe));
        Button start = new Button("Start the tour", e -> tour.start());
        Div controls = new Div(new Span("Tour:"), status, start);
        controls.addClassName("tour-controls");

        save.addThemeVariants(ButtonVariant.PRIMARY);
        Div form = new Div(new TextField("Name"), email, new TextField("Phone"),
                new TextField("Street"), new TextField("City"),
                new TextField("Country"), new TextArea("Notes"), save);
        form.addClassName("uc9-form");

        add(controls, form);
        add(new BuiltWith("A modal popover and a shadow around the target")
                .point("The popover is modal without a backdrop, so the rest of the page "
                        + "can not be clicked; the target's own large shadow dims the page.")
                .point("The step for the menu opens the drawer when it is closed, and "
                        + "the next step closes it again.")
                .point("The step for the button scrolls it into view first.")
                .java("uc9/SpotlightView").java("tour/TourPopover")
                .css("styles.css"));

        tour.addEndListener(state -> closeDrawerIfOpened());
        whenAttached(ui -> {
            // The menu is in the app layout, which is only known once the
            // view is attached to it.
            AppLayout layout = findAncestor(AppLayout.class);
            if (layout != null) {
                tour.setSteps(steps(layout));
            }
            return () -> {
                tour.reset();
                closeDrawerIfOpened();
            };
        });
    }

    private List<TourStep> steps(AppLayout layout) {
        Component menu = layout.getChildren().filter(SideNav.class::isInstance)
                .findFirst().orElseThrow();
        return List.of(TourStep
                .of(menu, "The app menu",
                        "All use cases are here. On a phone, the menu "
                                + "button in the top bar opens it.")
                .at(PopoverPosition.END).beforeShow(() -> openDrawer(layout)),
                TourStep.of(email, "Your login",
                        "Changing your email changes how you sign in.")
                        .beforeShow(this::closeDrawerIfOpened),
                TourStep.of(save, "Save when you are done",
                        "Nothing is saved until you press this button.")
                        .at(PopoverPosition.TOP)
                        .beforeShow(() -> save.scrollIntoView(
                                ScrollIntoViewOption.Block.CENTER)));
    }

    private void openDrawer(AppLayout layout) {
        if (!layout.isDrawerOpened()) {
            layout.setDrawerOpened(true);
            openedDrawer = layout;
        }
    }

    private void closeDrawerIfOpened() {
        if (openedDrawer != null) {
            openedDrawer.setDrawerOpened(false);
            openedDrawer = null;
        }
    }
}
