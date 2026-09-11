import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@tanstack/react-query';
import { Button } from '@/shared/components/ui/button';
import { Input } from '@/shared/components/ui/input';
import { Label } from '@/shared/components/ui/label';
import { authApi, readUserIdFromToken } from '@/features/auth/api/authApi';
import { useAuthStore } from '@/features/auth/store/authStore';
import { loginSchema, type LoginInput } from '@/shared/utils/validators';
import { getErrorMessage } from '@/shared/utils/errorHandler';
import { ApiError } from '@/shared/lib/api-response';
import { FieldError, FormError } from '@/features/auth/components/FormMessages';

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const setSession = useAuthStore((s) => s.setSession);
  const setCurrentUser = useAuthStore((s) => s.setCurrentUser);
  const [needsVerification, setNeedsVerification] = useState<string | null>(null);

  const form = useForm<LoginInput>({
    resolver: zodResolver(loginSchema),
    defaultValues: { username: '', password: '' },
  });

  const login = useMutation({
    mutationFn: (values: LoginInput) => authApi.login(values),
    onSuccess: async (tokens) => {
      setSession({
        accessToken: tokens.access_token,
        refreshToken: tokens.refresh_token,
      });

      // The token carries no roles, so the user has to be fetched before the UI can tell a premium
      // reader from a free one.
      const userId = readUserIdFromToken(tokens.access_token) ?? tokens.user_id;
      try {
        setCurrentUser(await authApi.getCurrentUser(userId));
      } catch {
        // Signed in regardless; the bootstrap effect retries this on the next load.
      }

      const from = (location.state as { from?: string } | null)?.from;
      navigate(from ?? '/', { replace: true });
    },
    onError: (error) => {
      // "User not verified" is the one genuine 401 in the auth flow, and hitting it re-sends the
      // OTP email as a side effect — so this failure is really a redirect to the verify step, not
      // an error to apologise for.
      if (error instanceof ApiError && error.message.includes('User not verified')) {
        setNeedsVerification(form.getValues('username'));
      }
    },
  });

  if (needsVerification) {
    return (
      <div className="container max-w-sm py-20">
        <h1 className="font-serif text-3xl font-bold tracking-tight">Verify your email</h1>
        <p className="mt-3 text-sm text-muted-foreground">
          This account is not verified yet. We just sent a fresh six-digit code to its email
          address — enter it to finish signing in.
        </p>
        <Button asChild className="mt-6 w-full">
          <Link to="/verify">Enter the code</Link>
        </Button>
        <Button
          variant="ghost"
          className="mt-2 w-full"
          onClick={() => setNeedsVerification(null)}
        >
          Back to sign in
        </Button>
      </div>
    );
  }

  return (
    <div className="container max-w-sm py-20">
      <h1 className="font-serif text-3xl font-bold tracking-tight">Welcome back</h1>
      <p className="mt-2 text-sm text-muted-foreground">Sign in to keep reading and writing.</p>

      <form
        onSubmit={form.handleSubmit((values) => login.mutate(values))}
        className="mt-8 space-y-4"
        noValidate
      >
        <div className="space-y-2">
          <Label htmlFor="username">Username</Label>
          <Input id="username" autoComplete="username" {...form.register('username')} />
          <FieldError message={form.formState.errors.username?.message} />
        </div>

        <div className="space-y-2">
          <Label htmlFor="password">Password</Label>
          <Input
            id="password"
            type="password"
            autoComplete="current-password"
            {...form.register('password')}
          />
          <FieldError message={form.formState.errors.password?.message} />
        </div>

        {login.isError && <FormError message={getErrorMessage(login.error, 'login')} />}

        <Button type="submit" className="w-full" disabled={login.isPending}>
          {login.isPending ? 'Signing in…' : 'Sign in'}
        </Button>
      </form>

      <p className="mt-6 text-center text-sm text-muted-foreground">
        New here?{' '}
        <Link to="/signup" className="font-medium text-foreground underline underline-offset-4">
          Create an account
        </Link>
      </p>
    </div>
  );
}
