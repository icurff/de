import { Home, Upload } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Video } from "lucide-react";
import { useLocation, useNavigate } from "react-router-dom";

const mainNavItems = [
  { icon: Home, label: "Home", href: "/" },
  { icon: Video, label: "Livestream", href: "/livestream" },
];

export function Sidebar() {
  const location = useLocation();
  const navigate = useNavigate();

  const isActive = (href: string) => {
    if (href === "/") {
      return location.pathname === "/";
    }
    return location.pathname.startsWith(href);
  };

  return (
    <aside className="w-64 bg-sidebar-bg border-r min-h-screen p-4">
      <div className="space-y-6">
        {/* Main Navigation */}
        <div className="space-y-2">
          {mainNavItems.map((item) => (
            <Button
              key={item.label}
              variant={isActive(item.href) ? "sidebar-active" : "sidebar"}
              className="gap-3"
              onClick={() => {
                navigate(item.href);
              }}
            >
              <item.icon className="h-5 w-5" />
              {item.label}
            </Button>
          ))}
        </div>
      </div>
    </aside>
  );
}
