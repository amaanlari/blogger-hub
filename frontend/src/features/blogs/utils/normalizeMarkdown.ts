/**
 * Prepares stored post content for rendering, repairing the two ways real posts in this app end up
 * displaying as raw markdown.
 *
 * Both are data problems rather than renderer problems, which is why they are fixed here and not by
 * loosening the markdown pipeline itself.
 */
export function normalizeMarkdown(content: string | null | undefined): string {
  if (!content) return '';

  // CRLF and lone CR would otherwise leave stray characters inside code blocks, where remark
  // preserves whitespace verbatim.
  const unified = content.replace(/\r\n?/g, '\n');

  return relaxAuthoringSlips(unescapeLineBreaks(unified));
}

/**
 * Restores line breaks that were stored escaped rather than literal.
 *
 * The backend applies no validation or normalisation to `content` — it stores whatever string it is
 * handed. A client that escapes newlines by hand (Postman with a typed-out backslash-n, a
 * double-escaped shell heredoc, a seeded fixture) stores two characters instead of an actual line
 * break. Markdown then has nothing to work with: the whole post becomes a single paragraph with the
 * escape sequences visible in the prose, and no headings, lists or code blocks at all.
 *
 * Deliberately conservative: it fires only when the content has **no real line breaks whatsoever**,
 * the signature of a wholly-escaped document. Content that genuinely mixes both — a post explaining
 * escape sequences, say — keeps at least one real newline and is left exactly as written.
 */
function unescapeLineBreaks(content: string): string {
  if (content.includes('\n')) return content;
  if (!/\\r\\n|\\n/.test(content)) return content;

  return content
    .replace(/\\r\\n/g, '\n')
    .replace(/\\n/g, '\n')
    .replace(/\\t/g, '\t');
}

/** Matches a fenced code block, or an inline code span. Fences first, so a fence containing
 *  backticks is not mis-split by the inline alternative. */
const CODE_REGION = /(?:^|\n)(```|~~~)[\s\S]*?(?:\n\1|$)|`[^`\n]*`/g;

/** A character prose cannot contain, used to mask code while the prose rules run. */
const SENTINEL = String.fromCharCode(0);

/**
 * Forgives the two markdown mistakes people actually make when writing in a plain textarea.
 *
 * CommonMark is stricter than most authors expect and rejects both of these silently — the text
 * just renders as literal markup, which reads to the author as "the renderer is broken":
 *
 * - `#Heading` with no space after the hashes is **not** a heading, only a paragraph that begins
 *   with a hash. The space is required.
 * - `**bold text **` cannot close its emphasis, because a closing delimiter may not be preceded by
 *   whitespace. It renders with the asterisks showing.
 *
 * Neither slip has a plausible intentional reading in prose, so both are repaired. Code is exempt:
 * fenced blocks and inline spans are lifted out first and restored untouched, because inside them
 * the literal characters are the entire point — a shell shebang or a C pointer expression has to
 * survive verbatim.
 */
function relaxAuthoringSlips(content: string): string {
  const preserved: string[] = [];
  const masked = content.replace(CODE_REGION, (match) => {
    preserved.push(match);
    return `${SENTINEL}${preserved.length - 1}${SENTINEL}`;
  });

  const relaxed = masked
    // `#Heading` -> `# Heading`, at line start only. The lookahead skips `# spaced` (already valid)
    // and a bare `###` rule, neither of which needs touching.
    .replace(/^(#{1,6})(?=[^\s#])/gm, '$1 ')
    // Strip the padding that stops emphasis closing: `**text **` and `** text**` both become
    // `**text**`. The \S guard leaves an empty `** **` alone rather than collapsing it.
    .replace(/(\*\*|__)[ \t]*(\S[^\n]*?)[ \t]+\1/g, '$1$2$1')
    .replace(/(\*\*|__)[ \t]+(\S[^\n]*?)[ \t]*\1/g, '$1$2$1');

  return relaxed.replace(
    new RegExp(`${SENTINEL}(\\d+)${SENTINEL}`, 'g'),
    (_, index: string) => preserved[Number(index)],
  );
}
