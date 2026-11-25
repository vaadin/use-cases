/**
 * The stock chart and the stock sync on UC5's picking screen, and the two bugs
 * the pickers keep running into.
 *
 * Both live here, in a module of the application's own bundle, rather than in
 * a string of executeJs code, because that is where a real screen's broken
 * script lives, and because only code in the bundle is covered by the
 * production sourcemaps: executeJs code is compiled in the browser at run
 * time, so a frame inside it points at nothing a map can explain. A production
 * build minifies this file into a chunk under /VAADIN/build/, and the
 * client-error insight the kit retains names a column on the first line of
 * that chunk; UC5 maps it back to this file.
 */

/** One bin's stock level, as the warehouse API returns it. */
interface StockLevel {
  bin: string;
  onHand: number;
}

/** The warehouse API's stock response. */
interface StockResponse {
  levels: StockLevel[];
}

/** What the chart was written against: an older shape of the same response. */
interface LegacyStockResponse {
  bins: StockLevel[];
}

const SAMPLE: StockResponse = {
  levels: [
    { bin: 'A-12-3', onHand: 340 },
    { bin: 'B-04-1', onHand: 1200 },
    { bin: 'C-21-7', onHand: 90 },
    { bin: 'D-08-2', onHand: 410 },
  ],
};

function drawStockChart(response: LegacyStockResponse): string[] {
  // The bug: the API renamed `bins` to `levels`, and the chart still reads
  // `bins`, so this throws "Cannot read properties of undefined".
  const tallest = Math.max(...response.bins.map((level) => level.onHand));
  return response.bins.map((level) => '#'.repeat(Math.round((level.onHand / tallest) * 20)));
}

/**
 * Opens the stock chart. Drawn on the next task, the way a chart waits for its
 * data, so the error is uncaught and reaches window.onerror: thrown straight
 * out of an executeJs call it would be caught by Flow and handed back to the
 * server as a failed result instead.
 */
function showStockLevels(): void {
  setTimeout(() => drawStockChart(SAMPLE as unknown as LegacyStockResponse), 0);
}

async function fetchStock(): Promise<StockResponse> {
  throw new Error('GET /api/warehouse/stock failed: 503 Service Unavailable');
}

/**
 * Syncs the stock from the warehouse API. The failure is a rejection nobody
 * handles, so it reaches window.onunhandledrejection with its stack, and the
 * frame it reports is in this file too.
 */
function syncStock(): void {
  void fetchStock().then((response) => response.levels.length);
}

declare global {
  interface Window {
    acmeStock: { showStockLevels(): void; syncStock(): void };
  }
}

window.acmeStock = { showStockLevels, syncStock };

export {};
