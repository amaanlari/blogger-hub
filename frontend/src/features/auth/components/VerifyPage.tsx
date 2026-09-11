import { useLocation, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@tanstack/react-query';
import { Button } from '@/shared/components/ui/button';
import { Input } from '@/shared/components/ui/input';
import { Label } from '@/shared/components/ui/label';
import { authApi } from '@/features/auth/api/authApi';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { otpSchema, type OtpInput } from '@/shared/utils/validators';
import { getErrorMessage } from '@/shared/utils/errorHandler';
import { FieldError, FormError } from '@/features/auth/components/FormMessages';

export default function VerifyPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { currentUser, isAuthenticated } = useAuth();

  const prefilledEmail =
    (location.state as { email?: string } | null)?.email ?? currentUser?.email ?? '';

  const form = useForm<OtpInput>({
    resolver: zodResolver(otpSchema),
    defaultValues: { email: prefilledEmail, otp: '' },
  });

  const verify = useMutation({
    mutationFn: (values: OtpInput) => authApi.verifyOtp(values),
    // Two routes lead here. Someone who just signed up already holds tokens and can go straight to
    // reading. Someone sent here by a blocked sign-in has no session at all, so dropping them on
    // the home page would silently leave them signed out after an apparently successful step.
    onSuccess: () => navigate(isAuthenticated ? '/' : '/login', { replace: true }),
  });

  return (
    <div className="container max-w-sm py-20">
      <h1 className="font-serif text-3xl font-bold tracking-tight">Verify your email</h1>
      <p className="mt-2 text-sm text-muted-foreground">
        Enter the six-digit code we emailed you. It expires quickly, so if it stops working just
        sign in again and we will send a new one.
      </p>

      <form
        onSubmit={form.handleSubmit((values) => verify.mutate(values))}
        className="mt-8 space-y-4"
        noValidate
      >
        <div className="space-y-2">
          <Label htmlFor="email">Email</Label>
          <Input id="email" type="email" autoComplete="email" {...form.register('email')} />
          <FieldError message={form.formState.errors.email?.message} />
        </div>

        <div className="space-y-2">
          <Label htmlFor="otp">Verification code</Label>
          <Input
            id="otp"
            inputMode="numeric"
            autoComplete="one-time-code"
            maxLength={6}
            placeholder="000000"
            className="tracking-[0.5em]"
            {...form.register('otp')}
          />
          <FieldError message={form.formState.errors.otp?.message} />
        </div>

        {verify.isError && <FormError message={getErrorMessage(verify.error)} />}

        <Button type="submit" className="w-full" disabled={verify.isPending}>
          {verify.isPending ? 'Verifying…' : 'Verify email'}
        </Button>
      </form>

      <p className="mt-6 text-center text-xs text-muted-foreground">
        Need a new code? Sign in again — a failed sign-in on an unverified account sends a fresh
        one.
      </p>
    </div>
  );
}
