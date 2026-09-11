import type {
  InnerEnvelope,
  OuterEnvelope,
  SpringErrorBody,
} from '@/shared/types/api';

/**
 * How a request failed, in terms this API actually supports.
 *
 * Branch on this, never on the HTTP status. Status is not a reliable signal here: a wrong password
 * is a 500, an ownership violation is a 401, a role denial is a 500, and a genuine "not found" is a
 * 404 only when a controller happened to hand-build one.
 *
 * - `exception`    Shape A. An unhandled server exception. Always HTTP 500, but frequently means
 *                  something mundane like "wrong password" — see `errorHandler.ts`.
 * - `business`     Shape B. A deliberate, hand-built failure: 400/401/403/404 with a real message.
 * - `unauthenticated` Shape C. Missing, malformed, or expired access token. The *only* kind that
 *                  should trigger a token refresh.
 * - `network`      The request never got an answer.
 */
export type ApiErrorKind =
  | 'exception'
  | 'business'
  | 'unauthenticated'
  | 'network';

export class ApiError extends Error {
  readonly kind: ApiErrorKind;
  /** HTTP status where meaningful. Unreliable as a semantic signal — see `ApiErrorKind`. */
  readonly status?: number;
  /** Spring's `HttpStatus` enum name, present on Shape A only. */
  readonly statusName?: string;

  constructor(args: {
    kind: ApiErrorKind;
    message: string;
    status?: number;
    statusName?: string;
  }) {
    super(args.message);
    this.name = 'ApiError';
    this.kind = args.kind;
    this.status = args.status;
    this.statusName = args.statusName;
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

/**
 * Reads an error out of a response body, or returns null if the body is not an error.
 *
 * The ordering below is the whole trick, and it is not arbitrary:
 *
 *  1. Shape A is checked first because it is the only shape that puts anything in the *outer*
 *     `error` field. Every other shape leaves it null.
 *  2. Shape B before Shape C, because Shape B bodies also carry a `status_code` and would
 *     otherwise be mistaken for Spring's `status`. Testing `success === false` is unambiguous.
 *  3. Shape C last: a numeric `status` plus a `path`, with no `success` key.
 */
export function readError(body: unknown): ApiError | null {
  if (!isRecord(body)) return null;

  const envelope = body as unknown as OuterEnvelope<unknown>;

  // Shape A — unhandled exception, reported at the outer level.
  if (isRecord(envelope.error)) {
    const outer = envelope.error;
    return new ApiError({
      kind: 'exception',
      message: typeof outer.message === 'string' ? outer.message : 'Request failed',
      status: 500,
      statusName: typeof outer.status === 'string' ? outer.status : undefined,
    });
  }

  const inner = envelope.data;
  if (!isRecord(inner)) return null;

  // Shape B — a deliberate ErrorResponse from a controller or service.
  if (inner.success === false) {
    const errorResponse = inner as unknown as InnerEnvelope;
    return new ApiError({
      kind: 'business',
      message: errorResponse.message || 'Request failed',
      status: errorResponse.status_code,
    });
  }

  // Shape C — Spring Boot's default error body. 401 in practice.
  if (typeof inner.status === 'number' && typeof inner.path === 'string') {
    const springError = inner as unknown as SpringErrorBody;
    return new ApiError({
      kind: springError.status === 401 ? 'unauthenticated' : 'business',
      message: springError.error || 'Request failed',
      status: springError.status,
    });
  }

  return null;
}

/**
 * Unwraps a successful response down to the payload the caller actually asked for, throwing an
 * {@link ApiError} if the body turns out to describe a failure.
 *
 * Two nesting depths exist and both are normal:
 *
 * - **Double-wrapped** (nearly everything): the controller returned a `DataResponse`, which the
 *   global advice then wrapped again. Payload is at `data.data`.
 * - **Single-wrapped** (`/auth/access-token`, `/auth/refresh-token`): the controller returned a
 *   bare DTO, so the advice's wrap is the only one. Payload is at `data`.
 *
 * `SuccessResponse` endpoints (create/update/delete) carry no payload at all and resolve to
 * `undefined` — use {@link unwrapMessage} if you need their confirmation text.
 */
export function unwrapResponse<T>(body: unknown): T {
  const error = readError(body);
  if (error) throw error;

  if (!isRecord(body)) return body as T;

  const inner = (body as unknown as OuterEnvelope<unknown>).data;

  // Double-wrapped: a DataResponse or SuccessResponse sitting inside the outer envelope.
  if (isRecord(inner) && inner.success === true) {
    return inner.data as T;
  }

  // Single-wrapped: the payload is the outer envelope's data.
  return inner as T;
}

/**
 * The inner envelope's human-readable message, when there is one.
 *
 * Needed because several endpoints communicate their only real result through this string — most
 * notably `PATCH /api/notifications/read-all`, whose response interpolates the count into the
 * message and provides it nowhere else.
 */
export function unwrapMessage(body: unknown): string | undefined {
  if (!isRecord(body)) return undefined;
  const inner = (body as unknown as OuterEnvelope<unknown>).data;
  if (isRecord(inner) && typeof inner.message === 'string') return inner.message;
  return undefined;
}
