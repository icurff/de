import { ReactNode } from "react";
import { AdminSidebar } from "./AdminSidebar";
import { Search, Bell, ChevronDown } from "lucide-react";
import { Button } from "./ui/button";
import { Input } from "./ui/input";

interface AdminLayoutProps {
  children: ReactNode;
}

export function AdminLayout({ children }: AdminLayoutProps) {
  return (
    <div className="flex h-screen bg-background">
      <AdminSidebar />

      <div className="flex flex-1 flex-col overflow-hidden">
        <header className="flex h-16 items-center justify-between border-b border-border bg-card px-6">
          <div className="flex flex-1 items-center gap-4">
            <div className="relative w-96">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input placeholder="Search..." className="pl-10" />
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2 rounded-lg bg-secondary px-3 py-1.5">
              <div className="h-8 w-8 rounded-full bg-primary" />
              <span className="text-sm font-medium">Admin User</span>
              <ChevronDown className="h-4 w-4" />
            </div>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto p-6">{children}</main>
      </div>
    </div>
  );
}
