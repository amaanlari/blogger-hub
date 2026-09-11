import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@tanstack/react-query';
import { Button } from '@/shared/components/ui/button';
import { Input } from '@/shared/components/ui/input';
import { Label } from '@/shared/components/ui/label';
import { Textarea } from '@/shared/components/ui/textarea';
import { authApi, readUserIdFromToken } from '@/features/auth/api/authApi';
import { useAuthStore } from '@/features/auth/store/authStore';
import { signupSchema, type SignupInput } from '@/shared/utils/validators';
import { getErrorMessage } from '@/shared/utils/errorHandler';
import { FieldError, FormError } from '@/features/auth/components/FormMessages';

export default function SignupPage() {
  const navigate = useNavigate();
  const setSession = useAuthStore((s) => s.setSession);
  const setCurrentUser = useAuthStore((s) => s.setCurrentUser);

  const form = useForm<SignupInput>({
    resolver: zodResolver(signupSchema),
    defaultValues: { username: '', email: '', password: '', bio: '' },
  });

  const signup = useMutation({
    mutationFn: (values: SignupInput) => authApi.signup(values),
    onSuccess: async (tokens, values) => {
      // Signup returns usable tokens immediately — the account is signed in before it is verified,
      // and only a later sign-in is gated on verification. So take the session now and send the
      // user to enter the code they were just emailed.
      setSession({
        accessToken: tokens.access_token,
        refreshToken: tokens.refresh_token,
      });

      const userId = readUserIdFromToken(tokens.access_token) ?? tokens.user_id;
      try {
        setCurrentUser(await authApi.getCurrentUser(userId));
      } catch {
        // Non-fatal — bootstrap retries on next load.
      }

      navigate('/verify', { replace: true, state: { email: values.email } });
    },
  });

  return (
    <div className="container max-w-sm py-20">
      <h1 className="font-serif text-3xl font-bold tracking-tight">Create your account</h1>
      <p className="mt-2 text-sm text-muted-foreground">
        It takes about a minute, and the first post is the hardest.
      </p>

      <form
        onSubmit={form.handleSubmit((values) => signup.mutate(values))}
        className="mt-8 space-y-4"
        noValidate
      >
        <div className="space-y-2">
          <Label htmlFor="username">Username</Label>
          <Input id="username" autoComplete="username" {...form.register('username')} />
          <FieldError message={form.formState.errors.username?.message} />
        </div>

        <div className="space-y-2">
          <Label htmlFor="email">Email</Label>
          <Input id="email" type="email" autoComplete="email" {...form.register('email')} />
          <FieldError message={form.formState.errors.email?.message} />
        </div>

        <div className="space-y-2">
          <Label htmlFor="password">Password</Label>
          <Input
            id="password"
            type="password"
            autoComplete="new-password"
            {...form.register('password')}
          />
          <FieldError message={form.formState.errors.password?.message} />
        </div>

        <div className="space-y-2">
          <Label htmlFor="bio">
            Bio <span className="text-muted-foreground">(optional)</span>
          </Label>
          <Textarea id="bio" rows={3} {...form.register('bio')} />
          <FieldError message={form.formState.errors.bio?.message} />
        </div>

        {signup.isError && <FormError message={getErrorMessage(signup.error, 'signup')} />}

        <Button type="submit" className="w-full" disabled={signup.isPending}>
          {signup.isPending ? 'Creating account…' : 'Create account'}
        </Button>
      </form>

      <p className="mt-6 text-center text-sm text-muted-foreground">
        Already have an account?{' '}
        <Link to="/login" className="font-medium text-foreground underline underline-offset-4">
          Sign in
        </Link>
      </p>
    </div>
  );
}
