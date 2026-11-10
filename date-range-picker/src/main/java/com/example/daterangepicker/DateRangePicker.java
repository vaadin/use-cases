package com.example.daterangepicker;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HasHelper;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.datepicker.DateRange;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.shared.Registration;

/**
 * A Flow field for the prototype {@code <vaadin-date-range-picker>} web
 * component.
 * <p>
 * Flow Components has no date range picker yet (API-GAPS.md #1), so this module
 * builds the one it needs. The value is Flow's own {@link DateRange}, so the
 * field works with {@code Binder} and {@code bindValue} like any other field. A
 * range with only one end, which the web component allows, has no
 * {@code DateRange}: the value is then {@code null}, and {@link #getStart()}
 * and {@link #getEnd()} still report the end that is set (API-GAPS.md #3).
 * <p>
 * Locale-based formatting and disabled dates reuse the connector of Flow's
 * {@code DatePicker}, which works on the range picker because both share the
 * same {@code i18n} and {@code isDateDisabled} API (API-GAPS.md #2). Validation
 * is left to the server ({@code manualValidation}), as in other Flow fields.
 * <p>
 * Migrate by deleting this package and switching to the Flow component once it
 * exists.
 */
@Tag("vaadin-date-range-picker")
@NpmPackage(value = "@vaadin/date-picker", version = DateRangePicker.PREVIEW_URL)
@JsModule("@vaadin/date-picker/vaadin-date-range-picker.js")
@JsModule("./vaadin-date-picker/datepickerConnector.ts")
public class DateRangePicker extends AbstractField<DateRangePicker, DateRange>
        implements HasLabel, HasHelper, HasValidation, HasSize, HasStyle,
        Focusable<DateRangePicker> {

    /**
     * The pkg.pr.new preview of {@code @vaadin/date-picker}, at the commit
     * whose web components the Flow Components snapshot of this module's pom
     * uses. Flow's {@code DatePicker} declares the same URL; declaring it here
     * too keeps the range picker working in a view without a date picker.
     */
    static final String PREVIEW_URL = "https://pkg.pr.new/vaadin/web-components/@vaadin/date-picker@dcd878f1286c04a77ca05219d530dcaa802a41dd";

    private final Set<LocalDate> disabledDates = new LinkedHashSet<>();
    private final Set<DayOfWeek> disabledWeekdays = EnumSet
            .noneOf(DayOfWeek.class);
    private @Nullable Locale locale;
    private @Nullable DateRangePickerI18n i18n;

    /**
     * Creates an empty range picker.
     */
    public DateRangePicker() {
        super(null);
        getElement().setProperty("manualValidation", true);
        // The values change in the browser while the user picks; the change
        // event says the user committed a range. Synchronize both properties
        // on it, and take the new value from the same event.
        getElement().addPropertyChangeListener("startValue", "change",
                event -> {
                });
        getElement().addPropertyChangeListener("endValue", "change", event -> {
        });
        getElement().addEventListener("change",
                event -> setModelValue(toRange(getStart(), getEnd()), true));
        getElement().addPropertyChangeListener("opened", "opened-changed",
                event -> {
                });
    }

    /**
     * Creates a range picker with a label.
     *
     * @param label
     *            the label
     */
    public DateRangePicker(String label) {
        this();
        setLabel(label);
    }

    @Override
    protected void setPresentationValue(@Nullable DateRange range) {
        getElement().setProperty("startValue",
                range == null ? "" : range.start().toString());
        getElement().setProperty("endValue",
                range == null ? "" : range.end().toString());
    }

    /**
     * Gets the start date, also while the range has no end.
     *
     * @return the start date, or {@code null} when it is not set
     */
    public @Nullable LocalDate getStart() {
        return parse(getElement().getProperty("startValue", ""));
    }

    /**
     * Gets the end date, also while the range has no start.
     *
     * @return the end date, or {@code null} when it is not set
     */
    public @Nullable LocalDate getEnd() {
        return parse(getElement().getProperty("endValue", ""));
    }

    /**
     * Sets the earliest date that can be picked.
     *
     * @param min
     *            the earliest date, or {@code null} for no limit
     */
    public void setMin(@Nullable LocalDate min) {
        getElement().setProperty("min", min == null ? "" : min.toString());
    }

    /**
     * Sets the latest date that can be picked.
     *
     * @param max
     *            the latest date, or {@code null} for no limit
     */
    public void setMax(@Nullable LocalDate max) {
        getElement().setProperty("max", max == null ? "" : max.toString());
    }

    /**
     * Sets dates that can not be picked, such as fully booked days.
     *
     * @param dates
     *            the dates
     */
    public void setDisabledDates(Collection<LocalDate> dates) {
        disabledDates.clear();
        disabledDates.addAll(dates);
        if (isAttached()) {
            updateDateMetadata();
        }
    }

    /**
     * Sets weekdays that can not be picked, such as weekends.
     *
     * @param weekdays
     *            the weekdays
     */
    public void setDisabledWeekdays(Collection<DayOfWeek> weekdays) {
        disabledWeekdays.clear();
        disabledWeekdays.addAll(weekdays);
        if (isAttached()) {
            updateDateMetadata();
        }
    }

    /**
     * Sets the locale that dates are formatted and parsed in. Month and day
     * names come from {@link #setI18n(DateRangePickerI18n)}.
     *
     * @param locale
     *            the locale
     */
    public void setLocale(Locale locale) {
        this.locale = Objects.requireNonNull(locale);
        if (isAttached()) {
            updateI18n();
        }
    }

    /**
     * Sets the texts of the picker: month and day names, the first day of the
     * week, and the accessible names of the range.
     *
     * @param i18n
     *            the texts
     */
    public void setI18n(DateRangePickerI18n i18n) {
        this.i18n = Objects.requireNonNull(i18n);
        if (isAttached()) {
            updateI18n();
        }
    }

    public void setStartPlaceholder(String placeholder) {
        getElement().setProperty("startPlaceholder", placeholder);
    }

    public void setEndPlaceholder(String placeholder) {
        getElement().setProperty("endPlaceholder", placeholder);
    }

    public void setClearButtonVisible(boolean clearButtonVisible) {
        getElement().setProperty("clearButtonVisible", clearButtonVisible);
    }

    public void setShowWeekNumbers(boolean showWeekNumbers) {
        getElement().setProperty("showWeekNumbers", showWeekNumbers);
    }

    /**
     * Makes a click on the start or the end input pick only that date, instead
     * of the whole range.
     *
     * @param separateDatePicking
     *            whether each input picks its own date
     */
    public void setSeparateDatePicking(boolean separateDatePicking) {
        getElement().setProperty("separateDatePicking", separateDatePicking);
    }

    /**
     * Shows the range in one text input, such as "3/10/2026 – 3/15/2026".
     *
     * @param singleInput
     *            whether to use one input
     */
    public void setSingleInput(boolean singleInput) {
        getElement().setProperty("singleInput", singleInput);
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        getElement().setProperty("readonly", readOnly);
    }

    @Override
    public boolean isReadOnly() {
        return getElement().getProperty("readonly", false);
    }

    @Override
    public void setRequiredIndicatorVisible(boolean required) {
        getElement().setProperty("required", required);
    }

    @Override
    public boolean isRequiredIndicatorVisible() {
        return getElement().getProperty("required", false);
    }

    @Override
    public void setErrorMessage(@Nullable String errorMessage) {
        getElement().setProperty("errorMessage",
                errorMessage == null ? "" : errorMessage);
    }

    @Override
    public @Nullable String getErrorMessage() {
        return getElement().getProperty("errorMessage");
    }

    @Override
    public void setInvalid(boolean invalid) {
        getElement().setProperty("invalid", invalid);
    }

    @Override
    public boolean isInvalid() {
        return getElement().getProperty("invalid", false);
    }

    /**
     * Adds a listener for when the user commits a change, also one that does
     * not change the value: picking only a start leaves the value {@code null},
     * as a range with one end has no {@link DateRange}.
     *
     * @param listener
     *            the listener
     * @return a registration for removing the listener
     */
    public Registration addChangeListener(
            ComponentEventListener<ChangeEvent> listener) {
        return addListener(ChangeEvent.class, listener);
    }

    /** Fired when the user commits a change of either end. */
    @DomEvent("change")
    public static class ChangeEvent extends ComponentEvent<DateRangePicker> {

        public ChangeEvent(DateRangePicker source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /** Opens the calendar. */
    public void open() {
        getElement().setProperty("opened", true);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // A new client element has no connector, texts or disabled dates. The
        // setters only send them while attached, so that no call reaches the
        // connector before it exists.
        getElement().executeJs(
                "window.Vaadin.Flow.datepickerConnector.initLazy(this)");
        if (locale != null || i18n != null) {
            updateI18n();
        }
        if (!disabledDates.isEmpty() || !disabledWeekdays.isEmpty()) {
            updateDateMetadata();
        }
    }

    private void updateI18n() {
        ObjectNode json = null;
        if (i18n != null) {
            json = JacksonUtils.beanToJson(i18n);
            // Dates have to be ISO strings for the connector.
            json.remove("referenceDate");
            if (i18n.getReferenceDate() != null) {
                json.put("referenceDate", i18n.getReferenceDate()
                        .format(DateTimeFormatter.ISO_DATE));
            }
        }
        getElement().callJsFunction("$connector.updateI18n",
                locale == null ? null : locale.toLanguageTag(), json);
    }

    private void updateDateMetadata() {
        ObjectNode config = JacksonUtils.createObjectNode();
        ArrayNode dates = config.putArray("disabledDates");
        // The connector takes [year, 0-based month, day], like the web
        // component passes dates to isDateDisabled.
        disabledDates.forEach(date -> dates.addArray().add(date.getYear())
                .add(date.getMonthValue() - 1).add(date.getDayOfMonth()));
        ArrayNode weekdays = config.putArray("disabledWeekdays");
        disabledWeekdays.forEach(weekday -> weekdays.add(weekday.getValue()));
        config.put("hasProvider", false);
        getElement().callJsFunction("$connector.setDateMetadataConfig", config);
    }

    private static @Nullable LocalDate parse(String value) {
        return value.isEmpty() ? null : LocalDate.parse(value);
    }

    /**
     * The range of two dates, or {@code null} when one is missing or the start
     * is after the end.
     */
    static @Nullable DateRange toRange(@Nullable LocalDate start,
            @Nullable LocalDate end) {
        if (start == null || end == null || start.isAfter(end)) {
            return null;
        }
        return new DateRange(start, end);
    }
}
