import hljs from 'highlight.js/lib/core';
import css from 'highlight.js/lib/languages/css';
import java from 'highlight.js/lib/languages/java';
import properties from 'highlight.js/lib/languages/properties';
import typescript from 'highlight.js/lib/languages/typescript';

// Only the languages BuiltWith shows, to keep the bundle small.
hljs.registerLanguage('css', css);
hljs.registerLanguage('java', java);
hljs.registerLanguage('properties', properties);
hljs.registerLanguage('typescript', typescript);

/**
 * Highlights a source shown by BuiltWith, in the language its
 * `language-<name>` class names.
 */
(window as any).highlightSource = (code: HTMLElement) => {
  hljs.highlightElement(code);
};
