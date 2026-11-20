package com.example.daterangepicker;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.datepicker.DatePicker.DatePickerI18n;

/**
 * The texts of {@link DateRangePicker}: everything of the date picker's texts,
 * plus the accessible names of the two inputs and the range roles that screen
 * readers announce for dates in the calendar. Texts left {@code null} keep the
 * web component's English default.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DateRangePickerI18n extends DatePickerI18n {

    private @Nullable String startAccessibleName;
    private @Nullable String endAccessibleName;
    private @Nullable String rangeAccessibleName;
    private @Nullable String rangeStart;
    private @Nullable String rangeEnd;
    private @Nullable String inRange;

    public @Nullable String getStartAccessibleName() {
        return startAccessibleName;
    }

    public DateRangePickerI18n setStartAccessibleName(
            @Nullable String startAccessibleName) {
        this.startAccessibleName = startAccessibleName;
        return this;
    }

    public @Nullable String getEndAccessibleName() {
        return endAccessibleName;
    }

    public DateRangePickerI18n setEndAccessibleName(
            @Nullable String endAccessibleName) {
        this.endAccessibleName = endAccessibleName;
        return this;
    }

    public @Nullable String getRangeAccessibleName() {
        return rangeAccessibleName;
    }

    public DateRangePickerI18n setRangeAccessibleName(
            @Nullable String rangeAccessibleName) {
        this.rangeAccessibleName = rangeAccessibleName;
        return this;
    }

    public @Nullable String getRangeStart() {
        return rangeStart;
    }

    public DateRangePickerI18n setRangeStart(@Nullable String rangeStart) {
        this.rangeStart = rangeStart;
        return this;
    }

    public @Nullable String getRangeEnd() {
        return rangeEnd;
    }

    public DateRangePickerI18n setRangeEnd(@Nullable String rangeEnd) {
        this.rangeEnd = rangeEnd;
        return this;
    }

    public @Nullable String getInRange() {
        return inRange;
    }

    public DateRangePickerI18n setInRange(@Nullable String inRange) {
        this.inRange = inRange;
        return this;
    }
}
