import { useRef, useState, type FormEvent } from 'react';
import { useMutation } from '@tanstack/react-query';
import { ImagePlus, Loader2 } from 'lucide-react';
import { Button } from '@/shared/components/ui/button';
import { Label } from '@/shared/components/ui/label';
import { Textarea } from '@/shared/components/ui/textarea';
import { Input } from '@/shared/components/ui/input';
import { Avatar, AvatarFallback, AvatarImage } from '@/shared/components/ui/avatar';
import { FieldError, FormError } from '@/features/auth/components/FormMessages';
import { bustCache, profileApi } from '@/features/profile/api/profileApi';
import { useAuthStore } from '@/features/auth/store/authStore';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { useLogout } from '@/features/auth/hooks/useLogout';
import { authApi } from '@/features/auth/api/authApi';
import { profileSchema } from '@/shared/utils/validators';
import { getErrorMessage } from '@/shared/utils/errorHandler';
import { initials } from '@/shared/utils/formatting';

export default function SettingsPage() {
  const { currentUser } = useAuth();
  const setCurrentUser = useAuthStore((s) => s.setCurrentUser);
  const logout = useLogout();

  const [bio, setBio] = useState(currentUser?.bio ?? '');
  const [bioError, setBioError] = useState<string | undefined>();
  const [saved, setSaved] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const save = useMutation({
    mutationFn: async (nextBio: string) => {
      await profileApi.update(currentUser!.id, { bio: nextBio });
      // The update returns only a success message, so refetch to keep the store honest rather than
      // assuming the write landed exactly as sent.
      return authApi.getCurrentUser(currentUser!.id);
    },
    onSuccess: (user) => {
      setCurrentUser(user);
      setSaved(true);
    },
  });

  const uploadAvatar = useMutation({
    mutationFn: async (file: File) => {
      await profileApi.uploadAvatar(currentUser!.id, file);
      return authApi.getCurrentUser(currentUser!.id);
    },
    onSuccess: (user) => {
      setCurrentUser({ ...user, profile_picture: bustCache(user.profile_picture) });
    },
  });

  const removeAvatar = useMutation({
    mutationFn: async () => {
      await profileApi.removeAvatar(currentUser!.id);
      return authApi.getCurrentUser(currentUser!.id);
    },
    onSuccess: (user) => {
      setCurrentUser({ ...user, profile_picture: bustCache(user.profile_picture) });
    },
  });

  if (!currentUser) return null;

  function handleSave(event: FormEvent) {
    event.preventDefault();
    setSaved(false);
    setBioError(undefined);

    const parsed = profileSchema.safeParse({ bio });
    if (!parsed.success) {
      setBioError(parsed.error.issues[0]?.message);
      return;
    }
    save.mutate(parsed.data.bio);
  }

  return (
    <div className="container max-w-lg py-12">
      <h1 className="font-serif text-3xl font-bold tracking-tight">Settings</h1>

      <section className="mt-10">
        <h2 className="text-sm font-medium">Profile picture</h2>
        <div className="mt-3 flex items-center gap-4">
          <Avatar className="h-16 w-16">
            <AvatarImage src={currentUser.profile_picture} alt="" />
            <AvatarFallback className="text-lg">
              {initials(currentUser.username)}
            </AvatarFallback>
          </Avatar>

          <div className="flex gap-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={uploadAvatar.isPending}
              onClick={() => fileInputRef.current?.click()}
            >
              {uploadAvatar.isPending ? (
                <Loader2 className="mr-1.5 h-3.5 w-3.5 animate-spin" />
              ) : (
                <ImagePlus className="mr-1.5 h-3.5 w-3.5" />
              )}
              Change
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              disabled={removeAvatar.isPending}
              onClick={() => removeAvatar.mutate()}
            >
              Remove
            </Button>
          </div>

          <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
            className="hidden"
            onChange={(event) => {
              const file = event.target.files?.[0];
              if (file) uploadAvatar.mutate(file);
              event.target.value = '';
            }}
          />
        </div>
        {uploadAvatar.isError && (
          <div className="mt-3">
            <FormError message={getErrorMessage(uploadAvatar.error)} />
          </div>
        )}
        {removeAvatar.isError && (
          <div className="mt-3">
            <FormError message={getErrorMessage(removeAvatar.error)} />
          </div>
        )}
      </section>

      <form onSubmit={handleSave} className="mt-10 space-y-4" noValidate>
        <div className="space-y-2">
          <Label htmlFor="username">Username</Label>
          {/* Changing a username or email needs PUT, which overwrites every field including the
              password — so it is not offered here rather than made quietly destructive. */}
          <Input id="username" value={currentUser.username} disabled />
          <p className="text-xs text-muted-foreground">Usernames cannot be changed.</p>
        </div>

        <div className="space-y-2">
          <Label htmlFor="email">Email</Label>
          <Input id="email" value={currentUser.email} disabled />
        </div>

        <div className="space-y-2">
          <Label htmlFor="bio">Bio</Label>
          <Textarea
            id="bio"
            value={bio}
            onChange={(event) => {
              setBio(event.target.value);
              setSaved(false);
            }}
            rows={4}
            placeholder="Tell readers who you are"
          />
          <FieldError message={bioError} />
        </div>

        {save.isError && <FormError message={getErrorMessage(save.error)} />}
        {saved && <p className="text-sm text-muted-foreground">Saved.</p>}

        <Button type="submit" disabled={save.isPending}>
          {save.isPending ? 'Saving…' : 'Save changes'}
        </Button>
      </form>

      <section className="mt-16 border-t pt-8">
        <Button variant="ghost" className="text-muted-foreground" onClick={() => logout()}>
          Sign out
        </Button>
      </section>
    </div>
  );
}
