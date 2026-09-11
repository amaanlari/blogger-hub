import { z } from 'zod';

/**
 * All input validation lives here, client-side, because the backend does essentially none.
 *
 * `@Valid` is wired up on exactly two endpoints in the whole API (login and block-user); every
 * other DTO carries `@NotBlank`/`@Size`/`@Email` annotations that are never evaluated. Signup will
 * cheerfully accept an empty username and an invalid email. And on the two endpoints where
 * validation does fire, the resulting `MethodArgumentNotValidException` is unhandled and comes back
 * as a 500 with a multi-line Spring dump — useless to show a user. So the only validation feedback
 * worth having is the kind that happens before the request leaves.
 *
 * The bounds mirror the (unenforced) server-side annotations so the two never disagree.
 */

export const loginSchema = z.object({
  username: z.string().min(1, 'Enter your username'),
  password: z.string().min(1, 'Enter your password'),
});

export const signupSchema = z.object({
  username: z
    .string()
    .min(3, 'Username must be at least 3 characters')
    .max(20, 'Username must be 20 characters or fewer')
    .regex(/^[a-zA-Z0-9_.-]+$/, 'Letters, numbers, dots, dashes and underscores only'),
  email: z.string().email('Enter a valid email address').max(60, 'Email is too long'),
  password: z
    .string()
    .min(6, 'Password must be at least 6 characters')
    .max(40, 'Password must be 40 characters or fewer'),
  bio: z.string().max(300, 'Bio must be 300 characters or fewer').optional(),
});

export const otpSchema = z.object({
  email: z.string().email('Enter a valid email address'),
  otp: z.string().regex(/^\d{6}$/, 'The code is six digits'),
});

export const postSchema = z.object({
  title: z.string().min(1, 'Give your post a title').max(200, 'Title is too long'),
  description: z
    .string()
    .min(1, 'Write a short description — it is what readers see in the feed')
    .max(300, 'Description must be 300 characters or fewer'),
  banner_image_url: z
    .string()
    .url('Enter a valid image URL')
    .or(z.literal(''))
    .optional(),
  content: z.string().min(1, 'Your post needs some content'),
});

export const profileSchema = z.object({
  bio: z.string().max(300, 'Bio must be 300 characters or fewer'),
});

export type LoginInput = z.infer<typeof loginSchema>;
export type SignupInput = z.infer<typeof signupSchema>;
export type OtpInput = z.infer<typeof otpSchema>;
export type PostInput = z.infer<typeof postSchema>;
export type ProfileInput = z.infer<typeof profileSchema>;
