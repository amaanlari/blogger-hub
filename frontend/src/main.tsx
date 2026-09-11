import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from '@/app/App';
import { Providers } from '@/app/providers';
import '@/index.css';

// Imported for its side effect: the auth store registers itself with the HTTP client on load, so
// this must happen before any request can fire.
import '@/features/auth/store/authStore';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Providers>
      <App />
    </Providers>
  </StrictMode>,
);
