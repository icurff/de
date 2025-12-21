import HomePage from "@/routes/HomePage";
import LivestreamPage from "@/routes/LivestreamPage";
import UserLivePage from "@/routes/UserLivePage";
import NotFoundPage from "@/routes/NotFoundPage";
import AdminDashboardPage from "@/routes/AdminDashboardPage";
import AdminUsersPage from "@/routes/AdminUserPage";
import AdminServerPage from "@/routes/AdminServerPage";
import PlayVideoPage from "@/routes/PlayVideoPage";
import ManageVideoPage from "@/routes/ManageVideoPage";
import { createBrowserRouter, RouterProvider } from "react-router-dom";

const router = createBrowserRouter([
  {
    path: "/",
    element: <HomePage />,
  },
  {
    path: "/livestream",
    element: <LivestreamPage />,
  },
  {
    // Route format: /@username/live
    // The @ is encoded as %40 in the URL
    path: "/:atUsername/live",
    element: <UserLivePage />,
  },
  {
    path: "/video/:videoId",
    element: <PlayVideoPage />,
  },
  {
    path: "/manage/videos/:videoId",
    element: <ManageVideoPage />,
  },
  {
    path: "/admin",
    element: <AdminDashboardPage />,
  },
  {
    path: "/admin/users",
    element: <AdminUsersPage />,
  },
  {
    path: "/admin/servers",
    element: <AdminServerPage />,
  },
  {
    path: "*",
    element: <NotFoundPage />,
  },
]);

const AppRouter = () => {
  return <RouterProvider router={router} />;
};

export default AppRouter;
