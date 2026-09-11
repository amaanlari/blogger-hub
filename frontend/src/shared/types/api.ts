/**
 * Wire types for the backend's response envelopes.
 *
 * Field names here are the *exact* keys on the wire. Do not "normalise" them — the API mixes
 * snake_case and camelCase within a single response body (see `PaginationMeta` below), so any
 * blanket transform silently breaks half of it.
 */

/**
 * The outer envelope, applied to every single response by `GlobalResponseHandler` (a
 * `ResponseBodyAdvice` whose `supports()` returns true unconditionally). Both `data` and `error`
 * are always present; neither is omitted when null.
 */
export interface OuterEnvelope<T = unknown> {
  /** Server-local `LocalDateTime`, no zone offset — not directly comparable to entity timestamps. */
  timestamp: string;
  data: T | null;
  error: OuterError | null;
}

/** Shape A's payload. Only ever produced by `GlobalExceptionHandler`, i.e. always an HTTP 500. */
export interface OuterError {
  /** The Spring `HttpStatus` enum *name* (`"INTERNAL_SERVER_ERROR"`), not a number. */
  status?: string;
  /** Raw `e.getMessage()` — may be a multi-line Spring dump. Never render unmapped. */
  message?: string;
  /** Always null; nothing in the backend ever populates it. */
  sub_errors?: unknown;
}

/** The inner envelope most controllers return, which then gets wrapped again by the outer one. */
export interface InnerEnvelope<T = unknown> {
  success: boolean;
  status_code: number;
  message: string;
  /** Present on `DataResponse`, absent on `SuccessResponse`. */
  data?: T;
  /** Present on `ErrorResponse`. Usually null even on failure — the real message is `message`. */
  error?: unknown;
}

/**
 * Shape C: Spring Boot's stock `BasicErrorController` body, which lands here because
 * `AccessTokenEntryPoint` calls `response.sendError(401)` and the resulting `/error` dispatch gets
 * wrapped by the outer envelope like anything else. Distinguished by a *numeric* `status` plus
 * `path`, and the absence of `success`/`status_code`.
 */
export interface SpringErrorBody {
  timestamp: string;
  status: number;
  error: string;
  /** Empty string in practice — `server.error.include-message` defaults to `never`. */
  message?: string;
  path: string;
}

/**
 * Pagination metadata. Built server-side with `Map.of(...)`, which bypasses Jackson's
 * `SNAKE_CASE` strategy entirely — hence `totalElements`/`totalPages` in camelCase sitting next to
 * snake_case DTO fields in the same response.
 *
 * There is no `hasNext`/`last`/cursor. Derive it: `page + 1 < totalPages`.
 */
export interface PaginationMeta {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** True when another page exists. The API gives no such field, so every caller derives it here. */
export function hasNextPage(pagination: PaginationMeta): boolean {
  return pagination.page + 1 < pagination.totalPages;
}
