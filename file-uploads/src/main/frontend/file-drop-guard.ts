/**
 * Browser half of MissingAPI.acceptFileDropsOnlyOn: while files are dragged, a
 * drag that carries files is refused everywhere outside the drop zone, and a
 * drag that carries no files (a link or an image from another browser window)
 * is refused on the drop zone, so the drop zone lights up only for what it can
 * take.
 */

interface DropZone extends HTMLElement {
  __fileDropGuard?: () => void;
}

const types = ['dragenter', 'dragover', 'drop'];

function remove(zone: DropZone): void {
  zone.__fileDropGuard?.();
}

function install(zone: DropZone): void {
  remove(zone);
  const guard = (e: Event) => {
    const event = e as DragEvent;
    const carriesFiles = Array.from(event.dataTransfer?.items ?? []).some((item) => item.kind === 'file');
    const onZone = event.composedPath().includes(zone);
    if (carriesFiles !== onZone) {
      event.preventDefault();
      event.stopPropagation();
      if (event.type !== 'drop' && event.dataTransfer) {
        event.dataTransfer.dropEffect = 'none';
      }
    }
  };
  types.forEach((type) => window.addEventListener(type, guard, true));
  zone.__fileDropGuard = () => {
    types.forEach((type) => window.removeEventListener(type, guard, true));
    delete zone.__fileDropGuard;
  };
}

(window as any).fileDropGuard = { install, remove };
