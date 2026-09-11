import { describe, expect, it } from 'vitest';
import {
  ApiError,
  readError,
  unwrapMessage,
  unwrapResponse,
} from '@/shared/lib/api-response';

/**
 * Every body below is copied from a real wire response documented in docs/API_CONTRACT.md, not
 * invented — the whole point of this suite is that the shapes are weird in ways that are easy to
 * get subtly wrong.
 */

describe('unwrapResponse', () => {
  it('unwraps a double-wrapped DataResponse to the inner payload', () => {
    const body = {
      timestamp: '2026-09-09T12:34:56.789',
      data: {
        success: true,
        status_code: 200,
        message: 'Logged in',
        data: { user_id: '65f', access_token: 'eyJ', refresh_token: 'eyK' },
      },
      error: null,
    };

    expect(unwrapResponse(body)).toEqual({
      user_id: '65f',
      access_token: 'eyJ',
      refresh_token: 'eyK',
    });
  });

  it('unwraps a single-wrapped token response one level shallower', () => {
    // POST /api/auth/access-token and /refresh-token return a bare TokenResponseDto, so the
    // global advice's wrap is the only one. Getting this wrong breaks silent refresh entirely.
    const body = {
      timestamp: '2026-09-09T12:34:56.789',
      data: { user_id: '65f', access_token: 'new', refresh_token: 'same' },
      error: null,
    };

    expect(unwrapResponse<{ access_token: string }>(body).access_token).toBe('new');
  });

  it('resolves a payload-less SuccessResponse to undefined', () => {
    const body = {
      timestamp: '2026-09-09T12:34:56.789',
      data: { success: true, status_code: 200, message: 'Blog post created successfully' },
      error: null,
    };

    expect(unwrapResponse(body)).toBeUndefined();
    expect(unwrapMessage(body)).toBe('Blog post created successfully');
  });

  it('does not mistake a payload of its own for an envelope', () => {
    // A post document has a `status`-free shape but is still a plain object; make sure nothing in
    // the discriminator chain grabs it.
    const body = {
      timestamp: '2026-09-09T12:34:56.789',
      data: {
        success: true,
        status_code: 200,
        message: 'Blog post found successfully.',
        data: { blog_post_id: '66a1', title: 'Hello', premium: false },
      },
      error: null,
    };

    expect(unwrapResponse<{ title: string }>(body).title).toBe('Hello');
  });
});

describe('readError — the three shapes', () => {
  it('Shape A: reports an outer-level exception as kind "exception"', () => {
    const body = {
      timestamp: '2026-09-09T12:00:00.123',
      data: null,
      error: {
        status: 'INTERNAL_SERVER_ERROR',
        message: 'Invalid credentials',
        sub_errors: null,
      },
    };

    const error = readError(body);
    expect(error).toBeInstanceOf(ApiError);
    expect(error?.kind).toBe('exception');
    expect(error?.message).toBe('Invalid credentials');
    expect(error?.statusName).toBe('INTERNAL_SERVER_ERROR');
    expect(error?.status).toBe(500);
  });

  it('Shape B: reports a hand-built ErrorResponse as kind "business"', () => {
    const body = {
      timestamp: '2026-09-09T12:00:00.123',
      data: {
        success: false,
        status_code: 404,
        message: 'Blog post not found',
        error: null,
      },
      error: null,
    };

    const error = readError(body);
    expect(error?.kind).toBe('business');
    expect(error?.status).toBe(404);
    expect(error?.message).toBe('Blog post not found');
  });

  it('Shape C: reports a Spring default 401 body as kind "unauthenticated"', () => {
    const body = {
      timestamp: '2026-09-09T21:08:21.338230623',
      data: {
        timestamp: '2026-09-09T15:38:21.337+00:00',
        status: 401,
        error: 'Unauthorized',
        path: '/api/users/id/65f',
      },
      error: null,
    };

    expect(readError(body)?.kind).toBe('unauthenticated');
  });

  it('Shape B wins over Shape C when a body could match both', () => {
    // This is the ordering trap. A Shape B body carries `status_code`, and an unlucky
    // discriminator reading a numeric `status` first would misread an ownership failure as an
    // expired token — which would then send the client into a refresh loop that never resolves.
    const ownershipFailure = {
      timestamp: '2026-09-09T12:00:00.123',
      data: {
        success: false,
        status_code: 401,
        message: "You are not authorized to access this user's profile",
        error: "Logged in user's id does not match the requested user's id",
      },
      error: null,
    };

    const error = readError(ownershipFailure);
    expect(error?.kind).toBe('business');
    expect(error?.kind).not.toBe('unauthenticated');
  });

  it('treats a Spring default 403 as a business failure, not a token problem', () => {
    const body = {
      timestamp: '2026-09-09T21:08:21.338230623',
      data: {
        timestamp: '2026-09-09T15:38:21.337+00:00',
        status: 403,
        error: 'Forbidden',
        path: '/api/users/all',
      },
      error: null,
    };

    expect(readError(body)?.kind).toBe('business');
  });

  it('returns null for a successful body', () => {
    const body = {
      timestamp: '2026-09-09T12:34:56.789',
      data: { success: true, status_code: 200, message: 'ok', data: [] },
      error: null,
    };

    expect(readError(body)).toBeNull();
  });

  it('unwrapResponse throws rather than returning an error body', () => {
    const body = {
      timestamp: '2026-09-09T12:00:00.123',
      data: null,
      error: { status: 'INTERNAL_SERVER_ERROR', message: 'User not found' },
    };

    expect(() => unwrapResponse(body)).toThrow(ApiError);
  });
});

describe('list endpoints that answer with null', () => {
  it('unwraps an empty comment list to null, which callers must coerce', () => {
    // Verified against a running server: a post with no comments answers `data: null`, not `[]`.
    // The unwrapper faithfully returns what the server sent; coercion belongs in the API layer
    // (see `toArray` in features/blogs/api/blogsApi.ts), so this test pins the actual behaviour
    // rather than a hoped-for one.
    const body = {
      timestamp: '2026-09-11T07:32:35.740391415',
      data: {
        success: true,
        status_code: 200,
        message: 'Comments retrieved successfully',
        data: null,
      },
      error: null,
    };

    expect(unwrapResponse(body)).toBeNull();
  });
});
