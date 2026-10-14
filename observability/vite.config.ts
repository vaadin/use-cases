import { UserConfigFn } from 'vite';
import { overrideVaadinConfig } from './vite.generated.ts';

const customConfig: UserConfigFn = (env) => ({
  // Here you can add custom Vite parameters
  // https://vitejs.dev/config/
  build: {
    // Production sourcemaps, for UC5. 'hidden' writes a .map next to every
    // chunk but leaves out the sourceMappingURL comment, so browsers do not
    // fetch them and the page is as minified as without; the maps travel in
    // the jar, where the server reads them to turn the minified frame of a
    // client-error insight back into a file, line and function (SourceMaps).
    // They are still served under /VAADIN/build/ like any other file there.
    sourcemap: 'hidden'
  }
});

export default overrideVaadinConfig(customConfig);
