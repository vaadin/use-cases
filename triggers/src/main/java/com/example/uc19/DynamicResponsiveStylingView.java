package com.example.uc19;

import com.example.views.MainLayout;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.trigger.internal.DomEventTrigger;
import com.vaadin.flow.component.trigger.internal.SizeTrigger;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC19 — Dynamic responsive styling.
 * <p>
 * Three native HTML colour pickers let the user choose the background colour to
 * apply at narrow, medium, and wide breakpoints. One
 * {@link ApplyResponsiveColorAction} is shared by four triggers:
 * <ul>
 * <li>a {@link SizeTrigger} on the view — fires on every resize</li>
 * <li>three {@link DomEventTrigger}s, one per picker, listening for
 * {@code input} — fires while the user drags the picker</li>
 * </ul>
 * The action reads the host's width and picks the matching picker's current
 * value, applying it as {@code style.backgroundColor}. Picking a new colour
 * shows up immediately even before the user closes the picker.
 * <p>
 * Pure CSS classes wouldn't reach this: the per-breakpoint values are
 * user-supplied, not preset in a stylesheet.
 */
@Route(value = "uc19", layout = MainLayout.class)
@PageTitle("UC19 — Dynamic responsive styling")
@Menu(order = 19, title = "UC19 — Dynamic responsive styling")
@StyleSheet("uc19.css")
public class DynamicResponsiveStylingView extends VerticalLayout {

    public DynamicResponsiveStylingView() {
        addClassName("uc19-view");
        add(new H1("UC19 — Dynamic responsive styling"));
        add(new Paragraph(
                "Pick a colour for each breakpoint. Resize the window: the "
                        + "target's background swaps between the three values "
                        + "you picked. The user-defined colours can't be baked "
                        + "into static CSS classes, so the action reads them "
                        + "live from the picker elements on every fire."));

        Input smallPicker = colorPicker("small", "#ffd166");
        Input mediumPicker = colorPicker("medium", "#06d6a0");
        Input largePicker = colorPicker("large", "#118ab2");

        Div target = new Div("Resize me");
        target.setId("target");
        target.addClassName("target");

        ApplyResponsiveColorAction apply = new ApplyResponsiveColorAction(
                target, smallPicker, mediumPicker, largePicker, 520, 900);

        // Same action instance wired to four triggers. Each trigger.triggers
        // call installs its own JsInitializer; the action's toJs is invoked
        // once per call but produces equivalent JsFunctions.
        new SizeTrigger(this).triggers(apply);
        new DomEventTrigger(smallPicker, "input").triggers(apply);
        new DomEventTrigger(mediumPicker, "input").triggers(apply);
        new DomEventTrigger(largePicker, "input").triggers(apply);

        HorizontalLayout pickers = new HorizontalLayout(
                pickerLabel("< 520 px", smallPicker),
                pickerLabel("520 – 899 px", mediumPicker),
                pickerLabel("≥ 900 px", largePicker));
        pickers.addClassName("pickers");

        add(pickers, target);
    }

    private static Input colorPicker(String id, String initial) {
        Input input = new Input();
        input.setId(id);
        input.setType("color");
        input.setValue(initial);
        input.addClassName("color-picker");
        return input;
    }

    private static Div pickerLabel(String label, Input picker) {
        Div wrap = new Div();
        wrap.addClassName("picker-label");
        Div title = new Div(label);
        title.addClassName("label");
        wrap.add(title, picker);
        return wrap;
    }
}
