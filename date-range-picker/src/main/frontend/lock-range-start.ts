/**
 * Makes the start of a <vaadin-date-range-picker> read-only, so the user can
 * only change the end. The picker has no such option (API-GAPS.md #9); this
 * relies on its current light DOM inputs and toggle button.
 *
 * - Typing, pasting and dropping into the start input do nothing, and the
 *   input is marked aria-readonly and left out of the tab order.
 * - Clicking the start input moves focus to the end input, which then decides
 *   what a pick in the calendar sets.
 * - The calendar button, which normally picks the whole range from the start,
 *   opens the calendar for the end only.
 *
 * Expects `separateDatePicking`, so a click on an input picks only that date,
 * and a `min` no earlier than the start, so no end before it can be picked.
 */
function lockRangeStart(picker: HTMLElement & { opened: boolean; open(): void; close(): void }): void {
  const start = picker.querySelector<HTMLInputElement>(':scope > input[slot="input"]');
  const end = picker.querySelector<HTMLInputElement>(':scope > input[slot="end-input"]');
  if (!start || !end || start.dataset.locked) {
    return;
  }
  start.dataset.locked = 'true';
  start.tabIndex = -1;
  start.setAttribute('aria-readonly', 'true');
  ['beforeinput', 'paste', 'drop'].forEach((type) => start.addEventListener(type, (event) => event.preventDefault()));
  start.addEventListener('focus', () => {
    // After the picker has handled this focus, so the end input wins.
    queueMicrotask(() => end.focus());
  });

  picker.shadowRoot!.addEventListener(
    'click',
    (event) => {
      const onToggle = event
        .composedPath()
        .some((node) => node instanceof Element && node.part?.contains('toggle-button'));
      if (!onToggle) {
        return;
      }
      event.stopImmediatePropagation();
      if (picker.opened) {
        picker.close();
      } else {
        end.focus();
        picker.open();
      }
    },
    true,
  );
}

(window as any).dateRangePickerLockStart = lockRangeStart;
