import { useQuery, type UseQueryResult } from "@tanstack/react-query";
import CustomAxios from "@/config/CustomAxios";

export type ServerSpecification = {
  ram: number;
  ram_usage: number;
  cpu: number;
  cpu_usage: number;
  disk: number;
  disk_usage: number;
};

export type AppServer = {
  id: string;
  name: string;
  ip: string;
  status: string; // EServerStatus
  current_load?: number;
  specification?: ServerSpecification;
  createdDate?: string;
  lastModifiedDate?: string;
};

export type AdminServerRow = {
  id: string;
  name: string;
  status: string;
  cpu: number; // CPU usage percentage
  cpuCores: number; // Total CPU cores
  memory: number; // RAM usage percentage
  ramTotal: number; // Total RAM in GB
  disk: number; // Disk usage percentage
  diskTotal: number; // Total Disk in GB
  location: string;
  uptime?: string;
};

export type AdminServers = {
  rows: AdminServerRow[];
};

async function fetchServers(): Promise<AdminServers> {
  const res = await CustomAxios.get<AppServer[]>(`/api/servers/`);
  const servers = res.data ?? [];
  const rows: AdminServerRow[] = servers.map((s) => {
    const spec = s.specification ?? ({} as ServerSpecification);
    return {
      id: s.id,
      name: s.name ?? s.ip ?? "Server",
      status: s.status ?? "UNKNOWN",
      cpu: Number(spec.cpu_usage ?? 0),
      cpuCores: Number(spec.cpu ?? 0),
      memory: Number(spec.ram_usage ?? 0),
      ramTotal: Number(spec.ram ?? 0),
      disk: Number(spec.disk_usage ?? 0),
      diskTotal: Number(spec.disk ?? 0),
      location: s.ip ?? "-",
      uptime: s.lastModifiedDate
        ? new Date(s.lastModifiedDate).toLocaleString()
        : s.createdDate
        ? new Date(s.createdDate).toLocaleString()
        : undefined,
    };
  });
  return { rows };
}

export function useListServers(): UseQueryResult<AdminServers, Error> {
  return useQuery<AdminServers, Error>({
    queryKey: ["admin", "servers"],
    queryFn: fetchServers,
    staleTime: 10_000,
  });
}



