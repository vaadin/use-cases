/**
 * Opens a tour step's popover once its target is on screen.
 *
 * A popover closes itself when it opens while its target has no size. That
 * is the case while Flow attaches a view (its container is hidden until the
 * navigation is done), inside a closed panel, or on a tab that is not
 * selected yet. Opening from the server in the same round trip therefore
 * fails (API-GAPS.md #6), so the popover waits here, one animation frame at
 * a time, until its target has a size.
 *
 * Nor does a popover bring its target on screen (API-GAPS.md #3): a target
 * below the fold, such as a button further down a long view, is scrolled to
 * the middle of the screen first, so the step does not open out of sight.
 */
type TourPopover = HTMLElement & {
  target?: HTMLElement;
  opened: boolean;
  walkthroughShowing?: boolean;
};

/** Whether the rectangle is inside the window and not under the app's top bar. */
function isOnScreen(rect: DOMRect) {
  const layout = document.querySelector('vaadin-app-layout');
  const navbar = layout?.shadowRoot?.querySelector('[part~="navbar"]');
  const top = navbar ? navbar.getBoundingClientRect().bottom : 0;
  return rect.top >= top && rect.bottom <= window.innerHeight && rect.left >= 0 && rect.right <= window.innerWidth;
}

function showStep(popover: TourPopover) {
  popover.walkthroughShowing = true;
  const open = () => {
    if (!popover.walkthroughShowing || !popover.isConnected) {
      return;
    }
    const rect = popover.target?.getBoundingClientRect();
    if (rect && (rect.width > 0 || rect.height > 0)) {
      if (!isOnScreen(rect)) {
        popover.target!.scrollIntoView({ block: 'center', inline: 'center', behavior: 'smooth' });
      }
      popover.opened = true;
    } else {
      requestAnimationFrame(open);
    }
  };
  open();
}

function hideStep(popover: TourPopover) {
  popover.walkthroughShowing = false;
  popover.opened = false;
}

(window as any).walkthroughShowStep = showStep;
(window as any).walkthroughHideStep = hideStep;
