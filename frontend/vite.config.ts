import type { Plugin } from 'vite';
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import path from 'node:path';
import fs from 'node:fs';

const STATIC_DIR = path.resolve(__dirname, '../src/main/resources/static');

/**
 * `emptyOutDir` wipes the whole static directory, `.gitkeep` included — and .gitignore relies on
 * that file existing to keep the otherwise-ignored directory tracked. Without this, every build
 * shows up as a spurious deletion in `git status`.
 */
function keepGitkeep(): Plugin {
  return {
    name: 'blogger-hub:keep-gitkeep',
    closeBundle() {
      fs.writeFileSync(path.join(STATIC_DIR, '.gitkeep'), '');
    },
  };
}

export default defineConfig({
  plugins: [react(), keepGitkeep()],
  resolve: {
    alias: { '@': path.resolve(__dirname, './src') },
  },
  build: {
    // Compiled output lands straight in the Spring Boot classpath, so `mvn package` bundles the
    // SPA into the same JAR as the API. Wired to the frontend-maven-plugin `npm run build`
    // execution in ../pom.xml.
    outDir: STATIC_DIR,
    emptyOutDir: true,
  },
  server: {
    port: 5173,
    // This proxy is load-bearing, not a convenience. SecurityConfig combines allowedOrigins("*")
    // with allowCredentials(true) — a combination Spring rejects at request time — and omits PATCH
    // from allowedMethods, which breaks the four PATCH endpoints for any browser client. Routing
    // dev traffic through Vite makes every call same-origin, so neither defect is reachable. In
    // production the SPA ships inside the backend JAR, which is same-origin for free.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
  },
});
