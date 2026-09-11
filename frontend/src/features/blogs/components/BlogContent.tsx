import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneDark } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { normalizeMarkdown } from '@/features/blogs/utils/normalizeMarkdown';

/**
 * Renders a post body as formatted markdown.
 *
 * Two things here are load-bearing:
 *
 * 1. Content is normalised first — see {@link normalizeMarkdown}. Posts stored with escaped rather
 *    than literal line breaks otherwise render as one long paragraph with `\n` showing in the text.
 * 2. There is no `rehype-raw`, deliberately. Post content is arbitrary text from any signed-in
 *    user and the backend sanitises nothing, so enabling raw HTML would hand every author a
 *    stored-XSS primitive. react-markdown escapes HTML by default — keep it that way unless a
 *    sanitiser goes in alongside it.
 */
export function BlogContent({ content }: { content: string | null | undefined }) {
  const markdown = normalizeMarkdown(content);

  if (!markdown.trim()) {
    return <p className="text-sm italic text-muted-foreground">This post has no content yet.</p>;
  }

  return (
    <div className="prose-article">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          code({ className, children, ...props }) {
            const language = /language-(\w+)/.exec(className ?? '')?.[1];
            const text = String(children).replace(/\n$/, '');

            // react-markdown v9 dropped the `inline` flag, but an inline span never spans a line
            // and never carries a language class — enough to tell the two apart.
            const isBlock = Boolean(language) || text.includes('\n');

            if (!isBlock) {
              return (
                <code className={className} {...props}>
                  {children}
                </code>
              );
            }

            return (
              <SyntaxHighlighter language={language ?? 'text'} style={oneDark} PreTag="div">
                {text}
              </SyntaxHighlighter>
            );
          },

          img({ src, alt }) {
            return (
              <img
                src={typeof src === 'string' ? src : undefined}
                alt={alt ?? ''}
                loading="lazy"
                onError={(event) => {
                  // Image URLs are arbitrary strings an author pasted; a dead one should vanish
                  // rather than leave a broken-image glyph mid-article.
                  event.currentTarget.style.display = 'none';
                }}
              />
            );
          },

          a({ href, children }) {
            return (
              <a href={href} target="_blank" rel="noopener noreferrer nofollow">
                {children}
              </a>
            );
          },

          // Tables come from remark-gfm and can be wider than the reading column, so each gets its
          // own horizontal scroll rather than forcing the whole page to scroll sideways.
          table({ children }) {
            return (
              <div className="overflow-x-auto">
                <table>{children}</table>
              </div>
            );
          },
        }}
      >
        {markdown}
      </ReactMarkdown>
    </div>
  );
}
