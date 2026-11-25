package com.example.daterangepicker;

import java.time.LocalDate;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;

/**
 * Simulates the user in browserless tests: picking or typing a range, which the
 * web component reports with the {@code change} event after updating
 * {@code startValue} and {@code endValue}.
 * <p>
 * Browserless tests have no tester for the range picker (API-GAPS.md #11).
 */
public class DateRangePickerTester extends ComponentTester<DateRangePicker> {

    public DateRangePickerTester(DateRangePicker picker) {
        super(picker);
    }

    /**
     * The user commits a range. Either end may be {@code null}, as when the
     * user picks a start and presses Escape.
     */
    public void pick(@Nullable LocalDate start, @Nullable LocalDate end) {
        setPropertyAsUser("startValue", start == null ? "" : start.toString());
        setPropertyAsUser("endValue", end == null ? "" : end.toString());
        fireDomEvent("change");
    }

    /**
     * The JavaScript calls waiting to be sent to the browser, in order. Reads,
     * and so consumes, them.
     */
    public static List<JavaScriptInvocation> pendingJavaScript() {
        UIInternals internals = UI.getCurrent().getInternals();
        internals.getStateTree().runExecutionsBeforeClientResponse();
        return internals.dumpPendingJavaScriptInvocations().stream()
                .map(invocation -> invocation.getInvocation()).toList();
    }
}
