import { AdminLayout } from "@/components/AdminLayout";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Users, Server, Activity, TrendingUp } from "lucide-react";

const stats = [
  {
    name: "Total Users",
    value: "2,345",
    icon: Users,
    change: "+12.5%",
    trend: "up",
  },
  {
    name: "Active Servers",
    value: "48",
    icon: Server,
    change: "+4.3%",
    trend: "up",
  },
  {
    name: "System Load",
    value: "67%",
    icon: Activity,
    change: "-2.1%",
    trend: "down",
  },
  {
    name: "Revenue",
    value: "$45.2K",
    icon: TrendingUp,
    change: "+23.1%",
    trend: "up",
  },
];

export default function AdminDashboardPage() {
  return (
    <AdminLayout>
      <div className="space-y-6">
        <div>
          <h2 className="text-3xl font-semibold text-foreground">Dashboard</h2>
          <p className="text-muted-foreground">Overview of your system</p>
        </div>

        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {stats.map((stat) => (
            <Card key={stat.name}>
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">
                  {stat.name}
                </CardTitle>
                <stat.icon className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{stat.value}</div>
                <p
                  className={`text-xs ${
                    stat.trend === "up" ? "text-green-600" : "text-red-600"
                  }`}
                >
                  {stat.change} from last month
                </p>
              </CardContent>
            </Card>
          ))}
        </div>

        <div className="grid gap-6 lg:grid-cols-2">
          <Card>
            <CardHeader>
              <CardTitle>Recent Activity</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {[1, 2, 3, 4].map((i) => (
                  <div key={i} className="flex items-center gap-4">
                    <div className="h-10 w-10 rounded-full bg-secondary" />
                    <div className="flex-1">
                      <p className="text-sm font-medium">User action #{i}</p>
                      <p className="text-xs text-muted-foreground">
                        2 hours ago
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>System Status</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {["Database", "API", "Storage", "Cache"].map((service) => (
                  <div
                    key={service}
                    className="flex items-center justify-between"
                  >
                    <span className="text-sm font-medium">{service}</span>
                    <span className="flex items-center gap-2 text-sm text-green-600">
                      <span className="h-2 w-2 rounded-full bg-green-600" />
                      Operational
                    </span>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </AdminLayout>
  );
}
