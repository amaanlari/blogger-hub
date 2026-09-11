import { describe, expect, it } from 'vitest';
import { normalizeMarkdown } from '@/features/blogs/utils/normalizeMarkdown';

describe('escaped line breaks', () => {
  it('repairs a wholly-escaped document', () => {
    // What the API stores when a client escapes newlines by hand. Before this, the whole post
    // rendered as one paragraph with the escape sequences visible in the prose.
    const stored = '## Heading\\n\\nSome **bold** text.\\n\\n- one\\n- two';

    expect(normalizeMarkdown(stored)).toBe(
      '## Heading\n\nSome **bold** text.\n\n- one\n- two',
    );
  });

  it('does not touch escape sequences in content that has real line breaks', () => {
    // A post that legitimately discusses escape sequences. Repairing here would corrupt the
    // author's actual words, so any real newline disables the unescape entirely.
    const content = 'Use `\\n` for a newline.\n\nThat is the convention.';
    expect(normalizeMarkdown(content)).toBe(content);
  });

  it('normalises CRLF so code blocks keep no stray carriage returns', () => {
    expect(normalizeMarkdown('line one\r\nline two\rline three')).toBe(
      'line one\nline two\nline three',
    );
  });
});

describe('authoring slips that CommonMark rejects', () => {
  it('repairs the real "Test Blog 1" post', () => {
    // Copied verbatim out of Mongo. Both slips are present: a heading with no space after the
    // hash, and a bold run whose closing delimiter is preceded by a space. CommonMark renders
    // both as literal text, which is what "the markdown is not rendering" turned out to mean.
    const stored =
      '#Blog1\n\n**Well I am writing the first blog on blogger hub and it is kinda cool. Being able to work on this. **\n\nAlthough we cannot add the images in this as we can in medium cause the editor here is just a simple text area. \n\nWill improve it on next iteration. ';

    const result = normalizeMarkdown(stored);

    expect(result).toContain('# Blog1');
    expect(result).toContain(
      '**Well I am writing the first blog on blogger hub and it is kinda cool. Being able to work on this.**',
    );
  });

  it('adds the missing space after a heading hash', () => {
    expect(normalizeMarkdown('#One\n##Two\n###Three')).toBe('# One\n## Two\n### Three');
  });

  it('leaves well-formed headings alone', () => {
    const content = '# One\n\n## Two\n\nBody text.';
    expect(normalizeMarkdown(content)).toBe(content);
  });

  it('leaves a bare hash rule alone', () => {
    expect(normalizeMarkdown('###\n\nText')).toBe('###\n\nText');
  });

  it('trims padding that stops emphasis closing, on both sides', () => {
    expect(normalizeMarkdown('**bold **')).toBe('**bold**');
    expect(normalizeMarkdown('** bold**')).toBe('**bold**');
    expect(normalizeMarkdown('** bold **')).toBe('**bold**');
    expect(normalizeMarkdown('__under __')).toBe('__under__');
  });

  it('leaves correct emphasis untouched', () => {
    const content = 'Some **bold** and __other__ text.';
    expect(normalizeMarkdown(content)).toBe(content);
  });

  it('does not collapse an empty emphasis run', () => {
    expect(normalizeMarkdown('** **')).toBe('** **');
  });
});

describe('code is never rewritten', () => {
  it('leaves a shebang inside a fenced block alone', () => {
    // `#!/bin/sh` at line start would otherwise gain a space and stop being a shebang.
    const content = 'Intro\n\n```sh\n#!/bin/sh\necho hi\n```\n\nOutro';
    expect(normalizeMarkdown(content)).toBe(content);
  });

  it('leaves markdown examples inside a fenced block alone', () => {
    // A post teaching markdown must be able to show the broken form without it being repaired.
    const content = 'Watch out:\n\n```\n#NotAHeading\n**padded **\n```\n\nThat is the trap.';
    expect(normalizeMarkdown(content)).toBe(content);
  });

  it('leaves inline code spans alone', () => {
    const content = 'Write `#Blog1` or `**bold **` to see the problem.';
    expect(normalizeMarkdown(content)).toBe(content);
  });

  it('still repairs prose that sits around a code block', () => {
    const content = '#Title\n\n```\n#raw\n```\n\n**tail **';
    expect(normalizeMarkdown(content)).toBe('# Title\n\n```\n#raw\n```\n\n**tail**');
  });

  it('does not corrupt a post containing a bare number between spaces', () => {
    // Guards the code-masking placeholder: a readable marker would be destroyed by this input.
    const content = 'Step `one` then 0 then `two` and 1 more.';
    expect(normalizeMarkdown(content)).toBe(content);
  });
});

describe('degenerate input', () => {
  it('returns an empty string for null or undefined', () => {
    // `content` has no validation server-side and can legitimately be absent.
    expect(normalizeMarkdown(null)).toBe('');
    expect(normalizeMarkdown(undefined)).toBe('');
  });
});
