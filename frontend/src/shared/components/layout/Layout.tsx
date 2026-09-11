import { Outlet } from 'react-router-dom';
import { Header } from '@/shared/components/layout/Header';

export function Layout() {
  return (
    <div className="flex min-h-screen flex-col">
      <Header />
      <main className="flex-1">
        <Outlet />
      </main>
      <footer className="border-t py-8">
        <div className="container text-sm text-muted-foreground">
          Blogger Hub — write something worth reading.
        </div>
      </footer>
    </div>
  );
}
