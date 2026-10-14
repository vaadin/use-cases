/**
 * Browser half of MissingAPI: wraps the Notifications API and reports back to
 * the server through custom DOM events on the UI element. Event names are
 * passed in by the server so both halves use the same constants.
 */

type Permission = NotificationPermission | 'unsupported';

interface Events {
  permission: string;
  click: string;
  close: string;
  error: string;
}

interface Body extends HTMLElement {
  __webNotificationEvents?: Events;
  __webNotificationPermission?: Permission;
  __webNotifications?: Map<string, Notification>;
}

interface Trigger extends HTMLElement {
  __webNotificationRequest?: () => void;
}

function supported(): boolean {
  return 'Notification' in window && window.isSecureContext;
}

function currentPermission(): Permission {
  return supported() ? Notification.permission : 'unsupported';
}

/** Reports the permission; unless forced, only when it has changed. */
function report(body: Body, force: boolean): void {
  const events = body.__webNotificationEvents;
  if (!events) {
    return;
  }
  const permission = currentPermission();
  if (!force && body.__webNotificationPermission === permission) {
    return;
  }
  body.__webNotificationPermission = permission;
  body.dispatchEvent(new CustomEvent(events.permission, { detail: { permission } }));
}

/**
 * Installed once per UI: reports the current permission now, and again
 * whenever it may have changed. Safari doesn't fire the Permissions API
 * change event reliably, so visibility and focus changes re-check too.
 */
function install(body: Body, permission: string, click: string, close: string, error: string): void {
  body.__webNotificationEvents = { permission, click, close, error };
  report(body, true);
  if (navigator.permissions?.query) {
    navigator.permissions
      .query({ name: 'notifications' })
      .then((status) => {
        status.onchange = () => report(body, false);
      })
      .catch(() => {});
  }
  document.addEventListener('visibilitychange', () => report(body, false));
  window.addEventListener('focus', () => report(body, false));
}

/**
 * Calls requestPermission() from the element's native click listener, so it
 * runs inside the user gesture. Handles both the Promise form and the legacy
 * callback form (older Safari).
 */
function requestOnClick(trigger: Trigger, body: Body): void {
  if (trigger.__webNotificationRequest) {
    return;
  }
  trigger.__webNotificationRequest = () => {
    if (!supported() || Notification.permission !== 'default') {
      report(body, true);
      return;
    }
    new Promise<unknown>((resolve) => {
      const result = Notification.requestPermission(resolve);
      result?.then(resolve, resolve);
    }).then(() => report(body, true));
  };
  trigger.addEventListener('click', trigger.__webNotificationRequest);
}

function removeRequestOnClick(trigger: Trigger): void {
  if (trigger.__webNotificationRequest) {
    trigger.removeEventListener('click', trigger.__webNotificationRequest);
    delete trigger.__webNotificationRequest;
  }
}

function show(body: Body, id: string, title: string, options: NotificationOptions): void {
  const events = body.__webNotificationEvents!;
  const fire = (type: string, extra: object = {}) =>
    body.dispatchEvent(new CustomEvent(type, { detail: { id, ...extra } }));
  if (!supported()) {
    fire(events.error, { reason: 'unsupported' });
    return;
  }
  if (Notification.permission !== 'granted') {
    fire(events.error, { reason: `permission-${Notification.permission}` });
    return;
  }
  let notification: Notification;
  try {
    notification = new Notification(title, options);
  } catch (e) {
    // e.g. Chrome on Android: "Illegal constructor", only
    // ServiceWorkerRegistration.showNotification() is allowed there.
    fire(events.error, { reason: String((e as Error)?.name ?? e) });
    return;
  }
  const registry = (body.__webNotifications ??= new Map());
  registry.set(id, notification);
  notification.onclick = (e) => {
    e.preventDefault();
    window.focus();
    notification.close();
    fire(events.click);
  };
  notification.onclose = () => {
    registry.delete(id);
    fire(events.close);
  };
  notification.onerror = () => {
    registry.delete(id);
    fire(events.error, { reason: 'error' });
  };
}

/** Closes the notification with the given id, or every one with the tag. */
function close(body: Body, id: string | null, tag: string | null): void {
  body.__webNotifications?.forEach((notification, key) => {
    if (key === id || (tag !== null && notification.tag === tag)) {
      notification.close();
    }
  });
}

(window as any).webNotifications = {
  install,
  report: (body: Body) => report(body, true),
  requestOnClick,
  removeRequestOnClick,
  show,
  close
};
