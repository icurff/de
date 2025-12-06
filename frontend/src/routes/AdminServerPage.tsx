import { AdminLayout } from "@/components/AdminLayout";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogTrigger,
} from "@/components/ui/dialog";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Plus,
  Server as ServerIcon,
  Activity,
  HardDrive,
  Cpu,
} from "lucide-react";
import { useListServers } from "@/hooks/Server/useListServers";
import { useCreateServerMutation } from "@/hooks/Server/useCreateServer";
import { useUpdateServerMutation } from "@/hooks/Server/useUpdateServer";
import { useDeleteServerMutation } from "@/hooks/Server/useDeleteServer";
import { useMemo, useState } from "react";

export default function AdminServerPage() {
  const { data, isLoading, isError } = useListServers();
  const { mutateAsync: createServer, isPending: isCreating } = useCreateServerMutation();
  const { mutateAsync: updateServer, isPending: isUpdating } = useUpdateServerMutation();
  const { mutateAsync: deleteServer, isPending: isDeleting } = useDeleteServerMutation();

  const [addOpen, setAddOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null);

  const [name, setName] = useState("");
  const [ip, setIp] = useState("");
  const [editingId, setEditingId] = useState<string | null>(null);

  const isMutating = useMemo(() => isCreating || isUpdating || isDeleting, [isCreating, isUpdating, isDeleting]);

  function openAddDialog() {
    setName("");
    setIp("");
    setAddOpen(true);
  }

  function openEditDialog(server: { id: string; name: string; location: string }) {
    setEditingId(server.id);
    setName(server.name ?? "");
    setIp(server.location ?? "");
    setEditOpen(true);
  }

  async function handleSubmitAdd() {
    await createServer({ name, ip });
    setAddOpen(false);
  }

  async function handleSubmitEdit() {
    if (!editingId) return;
    await updateServer({ serverId: editingId, name, ip });
    setEditOpen(false);
    setEditingId(null);
  }

  async function handleConfirmDelete() {
    if (!confirmDeleteId) return;
    await deleteServer(confirmDeleteId);
    setConfirmDeleteId(null);
  }
  const servers = data?.rows ?? [];
  return (
    <AdminLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-3xl font-semibold text-foreground">Servers</h2>
            <p className="text-muted-foreground">
              Monitor and manage your servers
            </p>
          </div>
          <Dialog open={addOpen} onOpenChange={setAddOpen}>
            <DialogTrigger asChild>
              <Button className="gap-2" onClick={openAddDialog}>
                <Plus className="h-4 w-4" />
                Add Server
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Add Server</DialogTitle>
              </DialogHeader>
              <div className="grid gap-4 py-2">
                <div className="grid gap-2">
                  <Label htmlFor="server-name">Name</Label>
                  <Input id="server-name" placeholder="My Server" value={name} onChange={(e) => setName(e.target.value)} />
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="server-ip">IP</Label>
                  <Input id="server-ip" placeholder="192.168.1.10" value={ip} onChange={(e) => setIp(e.target.value)} />
                </div>
              </div>
              <DialogFooter>
                <Button variant="secondary" onClick={() => setAddOpen(false)} disabled={isMutating}>
                  Cancel
                </Button>
                <Button onClick={handleSubmitAdd} disabled={!name || !ip || isMutating}>
                  Save
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </div>

        {isLoading && (
          <div className="text-sm text-muted-foreground">Loading servers...</div>
        )}
        {isError && (
          <div className="text-sm text-red-500">Failed to load servers.</div>
        )}
        {!isLoading && !isError && (
        <div className="grid gap-6 md:grid-cols-2">
          {servers.map((server) => (
            <Card key={server.id}>
              <CardHeader>
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-3">
                    <div className="rounded-lg bg-secondary p-2">
                      <ServerIcon className="h-5 w-5" />
                    </div>
                    <div>
                      <CardTitle className="text-lg">{server.name}</CardTitle>
                      <p className="text-sm text-muted-foreground">
                        {server.location}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button size="sm" variant="outline">Actions</Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem onClick={() => openEditDialog(server)}>Edit</DropdownMenuItem>
                        <DropdownMenuItem className="text-red-600" onClick={() => setConfirmDeleteId(server.id)}>Delete</DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                    <Badge
                      variant={
                        server.status?.toUpperCase() === "UP" || server.status?.toUpperCase() === "ONLINE"
                          ? "default"
                          : "secondary"
                      }
                    >
                      {server.status}
                    </Badge>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <div className="flex items-center justify-between text-sm">
                    <div className="flex items-center gap-2">
                      <Cpu className="h-4 w-4 text-muted-foreground" />
                      <span>CPU Usage</span>
                    </div>
                    <span className="font-medium">{server.cpu}%</span>
                  </div>
                  <Progress value={server.cpu} />
                </div>

                <div className="space-y-2">
                  <div className="flex items-center justify-between text-sm">
                    <div className="flex items-center gap-2">
                      <Activity className="h-4 w-4 text-muted-foreground" />
                      <span>Memory</span>
                    </div>
                    <span className="font-medium">{server.memory}%</span>
                  </div>
                  <Progress value={server.memory} />
                </div>

                <div className="space-y-2">
                  <div className="flex items-center justify-between text-sm">
                    <div className="flex items-center gap-2">
                      <HardDrive className="h-4 w-4 text-muted-foreground" />
                      <span>Disk Usage</span>
                    </div>
                    <span className="font-medium">{server.disk}%</span>
                  </div>
                  <Progress value={server.disk} />
                </div>

                <div className="flex items-center justify-between border-t border-border pt-4">
                  <span className="text-sm text-muted-foreground">Last Update</span>
                  <span className="text-sm font-medium">{server.uptime ?? "-"}</span>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
        )}
        <Dialog open={editOpen} onOpenChange={setEditOpen}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Edit Server</DialogTitle>
            </DialogHeader>
            <div className="grid gap-4 py-2">
              <div className="grid gap-2">
                <Label htmlFor="edit-server-name">Name</Label>
                <Input id="edit-server-name" value={name} onChange={(e) => setName(e.target.value)} />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="edit-server-ip">IP</Label>
                <Input id="edit-server-ip" value={ip} onChange={(e) => setIp(e.target.value)} />
              </div>
            </div>
            <DialogFooter>
              <Button variant="secondary" onClick={() => setEditOpen(false)} disabled={isMutating}>
                Cancel
              </Button>
              <Button onClick={handleSubmitEdit} disabled={!name || !ip || isMutating}>
                Save
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>

        <AlertDialog open={!!confirmDeleteId} onOpenChange={(open) => !open && setConfirmDeleteId(null)}>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>Delete server?</AlertDialogTitle>
              <AlertDialogDescription>
                This action cannot be undone. This will permanently remove the server.
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel disabled={isMutating} onClick={() => setConfirmDeleteId(null)}>Cancel</AlertDialogCancel>
              <AlertDialogAction className="bg-red-600 hover:bg-red-700" onClick={handleConfirmDelete} disabled={isMutating}>Delete</AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </div>
    </AdminLayout>
  );
}
