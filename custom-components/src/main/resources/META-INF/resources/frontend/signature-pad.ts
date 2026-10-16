import { css, html, LitElement } from 'lit';
import { customElement, property, query } from 'lit/decorators.js';

/**
 * A canvas to sign on with the mouse, a pen or a finger (UC5).
 *
 * Properties: `value` (the signature as a PNG data URL, empty when blank),
 * `disabled`. Events: `value-changed` after each stroke, `signature-started`
 * when the first stroke of an empty pad begins. Method: `clear()`.
 * Styling: the `canvas` and `baseline` parts, and `--signature-pad-ink`.
 */
@customElement('signature-pad')
export class SignaturePad extends LitElement {
  static override styles = css`
    :host {
      display: inline-block;
      position: relative;
      width: 24rem;
      max-width: 100%;
      height: 8rem;
      border: 1px solid var(--vaadin-border-color, #ccc);
      border-radius: var(--vaadin-radius-m, 6px);
      background: var(--vaadin-background-container, #fff);
      touch-action: none;
      --signature-pad-ink: var(--vaadin-text-color, #1a1a1a);
    }
    :host([disabled]) {
      opacity: 0.6;
      pointer-events: none;
    }
    canvas {
      width: 100%;
      height: 100%;
      cursor: crosshair;
    }
    [part='baseline'] {
      position: absolute;
      inset-inline: 1rem;
      bottom: 1.75rem;
      border-bottom: 1px dashed var(--vaadin-border-color, #ccc);
      pointer-events: none;
    }
  `;

  @property({ type: String }) value = '';
  @property({ type: Boolean, reflect: true }) disabled = false;

  @query('canvas') private canvas!: HTMLCanvasElement;

  private drawing = false;

  override render() {
    return html`<canvas
        part="canvas"
        aria-label="Signature"
        @pointerdown=${this.start}
        @pointermove=${this.move}
        @pointerup=${this.end}
        @pointerleave=${this.end}
      ></canvas>
      <div part="baseline"></div>`;
  }

  override firstUpdated() {
    const ratio = window.devicePixelRatio || 1;
    const box = this.canvas.getBoundingClientRect();
    this.canvas.width = box.width * ratio;
    this.canvas.height = box.height * ratio;
    this.context().scale(ratio, ratio);
  }

  /**
   * Empties the pad. The server may call this before the element has
   * rendered its canvas, so it waits for the first render (API-GAPS.md #3,
   * vaadin/flow#6007).
   */
  async clear() {
    await this.updateComplete;
    const canvas = this.canvas;
    this.context().clearRect(0, 0, canvas.width, canvas.height);
    this.setValue('');
  }

  private context() {
    return this.canvas.getContext('2d')!;
  }

  private start(e: PointerEvent) {
    if (this.value === '') {
      this.dispatchEvent(new CustomEvent('signature-started'));
    }
    this.drawing = true;
    this.canvas.setPointerCapture(e.pointerId);
    const ctx = this.context();
    ctx.lineWidth = 2;
    ctx.lineCap = 'round';
    ctx.strokeStyle = getComputedStyle(this).getPropertyValue('--signature-pad-ink').trim() || '#1a1a1a';
    ctx.beginPath();
    ctx.moveTo(e.offsetX, e.offsetY);
  }

  private move(e: PointerEvent) {
    if (!this.drawing) {
      return;
    }
    const ctx = this.context();
    ctx.lineTo(e.offsetX, e.offsetY);
    ctx.stroke();
  }

  private end() {
    if (!this.drawing) {
      return;
    }
    this.drawing = false;
    this.setValue(this.canvas.toDataURL('image/png'));
  }

  private setValue(value: string) {
    this.value = value;
    this.dispatchEvent(new CustomEvent('value-changed', { detail: { value } }));
  }
}

declare global {
  interface HTMLElementTagNameMap {
    'signature-pad': SignaturePad;
  }
}
