/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Optional API base URL override. Defaults to the relative `/api` — see .env.example. */
  readonly VITE_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
