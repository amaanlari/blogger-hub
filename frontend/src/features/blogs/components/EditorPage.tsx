import { useEffect, useRef, useState, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import MDEditor from '@uiw/react-md-editor';
import { ImagePlus, Loader2 } from 'lucide-react';
import { Button } from '@/shared/components/ui/button';
import { Input } from '@/shared/components/ui/input';
import { Label } from '@/shared/components/ui/label';
import { Textarea } from '@/shared/components/ui/textarea';
import { FieldError, FormError } from '@/features/auth/components/FormMessages';
import { blogsApi, type BlogPostInput } from '@/features/blogs/api/blogsApi';
import { blogKeys, useBlogDetail } from '@/features/blogs/hooks/useBlogs';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { postSchema } from '@/shared/utils/validators';
import { getErrorMessage } from '@/shared/utils/errorHandler';
import { FullPageSpinner } from '@/shared/components/FullPageSpinner';
import { useMediaQuery } from '@/shared/hooks/useMediaQuery';
import { normalizeMarkdown } from '@/features/blogs/utils/normalizeMarkdown';

export default function EditorPage() {
  const { id } = useParams<{ id: string }>();
  const isEditing = Boolean(id);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { currentUser } = useAuth();
  const isDesktop = useMediaQuery('(min-width: 640px)');

  const existing = useBlogDetail(id);

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [bannerImageUrl, setBannerImageUrl] = useState('');
  const [content, setContent] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [uploadError, setUploadError] = useState<string | null>(null);

  const bannerInputRef = useRef<HTMLInputElement>(null);
  const inlineInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (!existing.data) return;
    setTitle(existing.data.title ?? '');
    setDescription(existing.data.description ?? '');
    setBannerImageUrl(existing.data.banner_image_url ?? '');
    setContent(existing.data.content ?? '');
  }, [existing.data]);

  const upload = useMutation({
    mutationFn: (file: File) => blogsApi.uploadImage(file),
    onError: (error) => setUploadError(getErrorMessage(error)),
  });

  const save = useMutation({
    mutationFn: (body: BlogPostInput) =>
      isEditing ? blogsApi.update(id!, body) : blogsApi.create(body),
    onSuccess: async () => {
      queryClient.invalidateQueries({ queryKey: blogKeys.all });

      if (isEditing) {
        navigate(`/posts/${id}`, { replace: true });
        return;
      }

      // Creating a post returns only a success message — no ID, no body. The only way to find what
      // was just created is to re-list the author's posts and take the newest one.
      try {
        const posts = await blogsApi.listByUsername(currentUser!.username);
        const newest = [...posts].sort((a, b) =>
          b.created_at.localeCompare(a.created_at),
        )[0];
        navigate(newest ? `/posts/${newest.blog_post_id}` : '/', { replace: true });
      } catch {
        navigate('/', { replace: true });
      }
    },
  });

  if (isEditing && existing.isPending) return <FullPageSpinner />;

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setFieldErrors({});

    const parsed = postSchema.safeParse({
      title,
      description,
      banner_image_url: bannerImageUrl,
      content,
    });

    if (!parsed.success) {
      const errors: Record<string, string> = {};
      for (const issue of parsed.error.issues) {
        const key = String(issue.path[0]);
        if (!errors[key]) errors[key] = issue.message;
      }
      setFieldErrors(errors);
      return;
    }

    save.mutate({
      title: parsed.data.title,
      description: parsed.data.description,
      banner_image_url: parsed.data.banner_image_url ?? '',
      // Store the repaired form rather than the raw keystrokes, so what lands in the database is
      // already valid markdown. The reader-side normaliser then only has to carry legacy rows
      // written before this, instead of compensating for every new post forever.
      content: normalizeMarkdown(parsed.data.content),
    });
  }

  async function handleBannerFile(file: File) {
    setUploadError(null);
    const result = await upload.mutateAsync(file).catch(() => null);
    if (result) setBannerImageUrl(result.secure_url);
  }

  async function handleInlineFile(file: File) {
    setUploadError(null);
    const result = await upload.mutateAsync(file).catch(() => null);
    if (result) {
      const alt = file.name.replace(/\.[^.]+$/, '');
      setContent((current) => `${current}\n\n![${alt}](${result.secure_url})\n`);
    }
  }

  return (
    <div className="container max-w-3xl py-10">
      <h1 className="font-serif text-3xl font-bold tracking-tight">
        {isEditing ? 'Edit post' : 'Write a post'}
      </h1>

      <form onSubmit={handleSubmit} className="mt-8 space-y-6" noValidate>
        <div className="space-y-2">
          <Label htmlFor="title">Title</Label>
          <Input
            id="title"
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            placeholder="A title worth clicking"
            className="h-12 font-serif text-lg"
          />
          <FieldError message={fieldErrors.title} />
        </div>

        <div className="space-y-2">
          <Label htmlFor="description">Description</Label>
          <Textarea
            id="description"
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            rows={2}
            placeholder="The one-line summary readers see in the feed"
          />
          <FieldError message={fieldErrors.description} />
        </div>

        <div className="space-y-2">
          <Label htmlFor="banner">Banner image</Label>
          <div className="flex gap-2">
            <Input
              id="banner"
              value={bannerImageUrl}
              onChange={(event) => setBannerImageUrl(event.target.value)}
              placeholder="https://…"
            />
            <Button
              type="button"
              variant="outline"
              disabled={upload.isPending}
              onClick={() => bannerInputRef.current?.click()}
            >
              {upload.isPending ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <ImagePlus className="h-4 w-4" />
              )}
            </Button>
            <input
              ref={bannerInputRef}
              type="file"
              accept="image/*"
              className="hidden"
              onChange={(event) => {
                const file = event.target.files?.[0];
                if (file) void handleBannerFile(file);
                event.target.value = '';
              }}
            />
          </div>
          {bannerImageUrl && (
            <img
              src={bannerImageUrl}
              alt=""
              className="mt-2 max-h-48 w-full rounded-md object-cover"
              onError={(event) => {
                event.currentTarget.style.display = 'none';
              }}
            />
          )}
          <FieldError message={fieldErrors.banner_image_url} />
        </div>

        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <Label htmlFor="content">Content</Label>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              disabled={upload.isPending}
              onClick={() => inlineInputRef.current?.click()}
            >
              <ImagePlus className="mr-1.5 h-3.5 w-3.5" />
              Insert image
            </Button>
            <input
              ref={inlineInputRef}
              type="file"
              accept="image/*"
              className="hidden"
              onChange={(event) => {
                const file = event.target.files?.[0];
                if (file) void handleInlineFile(file);
                event.target.value = '';
              }}
            />
          </div>

          {/* Side-by-side preview on desktop, because without it an author has no way to notice
              that their markdown is not parsing — a heading written `#Like this`, or bold written
              `**like this **`, renders as literal text and the mistake only surfaces after
              publishing. Below `sm` two columns are unreadable, so the preview becomes a toggle in
              the toolbar instead. */}
          <div data-color-mode="light">
            <MDEditor
              value={content}
              onChange={(value) => setContent(value ?? '')}
              height={isDesktop ? 480 : 360}
              preview={isDesktop ? 'live' : 'edit'}
            />
          </div>
          <FieldError message={fieldErrors.content} />
        </div>

        {uploadError && <FormError message={uploadError} />}
        {save.isError && <FormError message={getErrorMessage(save.error)} />}

        <div className="flex justify-end gap-2">
          <Button type="button" variant="ghost" onClick={() => navigate(-1)}>
            Cancel
          </Button>
          <Button type="submit" disabled={save.isPending}>
            {save.isPending ? 'Saving…' : isEditing ? 'Save changes' : 'Publish'}
          </Button>
        </div>
      </form>
    </div>
  );
}
