import { ApiError } from '@/shared/lib/api-response';

/**
 * Backend messages that are really ordinary user-facing outcomes, mapped to copy worth showing.
 *
 * These all arrive as HTTP 500 Shape A, because `BadCredentialsException` and
 * `UsernameNotFoundException` are not registered in `GlobalExceptionHandler` and fall through to
 * the catch-all handler. Treating a 500 as "something broke, try again" would therefore tell a user
 * with a typo'd password exactly the wrong thing.
 *
 * Matched as substrings, most specific first — the raw message is sometimes a longer Spring dump
 * with the meaningful phrase embedded in it.
 */
const MESSAGE_MAP: ReadonlyArray<readonly [string, string]> = [
  ['User not verified', 'Please verify your email first — we just sent you a new code.'],
  ['Invalid OTP', 'That code is incorrect or has expired. Request a new one.'],
  ['User not found by email', 'No account is registered with that email address.'],
  ['User not found', 'No account found with those details.'],
  ['Invalid token', 'Your session has expired. Please log in again.'],
  ['Invalid credentials', 'Incorrect username or password.'],
  ['Access Denied', 'You do not have permission to do that.'],
  ['This is a premium post', 'This post is for premium members.'],
  ['Blog post not found', 'That post no longer exists.'],
  ['Notification not found', 'That notification no longer exists.'],
  ['Block relationship not found', 'That user is not blocked.'],
  [
    'not authorized to access this user',
    'You can only view or change your own profile.',
  ],
  [
    'does not match requested user',
    'Only the author can change this.',
  ],
];

/**
 * Turns any thrown error into a sentence worth putting in front of a user.
 *
 * Signup is the notable special case: `AuthService` catches its own specific failures and rethrows
 * a generic `BadCredentialsException("Invalid credentials")`, so a duplicate username, a duplicate
 * email and a database outage are literally indistinguishable in the response. The honest phrasing
 * is therefore a hedge, not a guess — hence `context`.
 */
export function getErrorMessage(
  error: unknown,
  context?: 'signup' | 'login',
): string {
  if (!(error instanceof ApiError)) {
    return 'Something went wrong. Please try again.';
  }

  if (error.kind === 'network') {
    return 'Could not reach the server. Check your connection and try again.';
  }

  if (context === 'signup' && error.message.includes('Invalid credentials')) {
    return 'Could not create that account — the username or email may already be taken.';
  }

  for (const [needle, friendly] of MESSAGE_MAP) {
    if (error.message.includes(needle)) return friendly;
  }

  // A business error carries a message a controller author wrote deliberately, so it is usually
  // safe to show. An unmapped exception message is raw `e.getMessage()` and may be a stack-shaped
  // Spring dump, so it never reaches the user.
  if (error.kind === 'business') return error.message;

  return 'Something went wrong. Please try again.';
}
