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

function showStep(popover: TourPopover) {
  popover.walkthroughShowing = true;
  const open = () => {
    if (!popover.walkthroughShowing || !popover.isConnected) {
      return;
    }
    const rect = popover.target?.getBoundingClientRect();
    if (rect && (rect.width > 0 || rect.height > 0)) {
      if (rect.top < 0 || rect.bottom > window.innerHeight) {
        popover.target!.scrollIntoView({ block: 'center', behavior: 'smooth' });
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
