import { HexColorPicker } from 'react-colorful';
import { type RenderHooks, ReactAdapterElement } from 'Frontend/generated/flow/ReactAdapter';

/**
 * react-colorful's HexColorPicker as a custom element (UC7). The `color`
 * state is shared with the server: React renders it, a drag updates it, and
 * the server's ColorPicker reads and writes it.
 */
class ColorPickerElement extends ReactAdapterElement {
  protected override render(hooks: RenderHooks) {
    const [color, setColor] = hooks.useState<string>('color');
    return <HexColorPicker color={color ?? '#2563eb'} onChange={setColor} />;
  }
}

customElements.define('color-picker', ColorPickerElement);
