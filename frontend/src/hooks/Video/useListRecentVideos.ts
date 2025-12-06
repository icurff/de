import { useQuery, type UseQueryResult } from "@tanstack/react-query";
import axios from "@/config/CustomAxios";
import { type VideoPrivacy } from "@/types/video";

export type RecentVideo = {
  id: string;
  title: string;
  description: string;
  thumbnail: string;
  uploadedDate: string;
  privacy: VideoPrivacy;
};

async function fetchRecentVideos(limit: number): Promise<RecentVideo[]> {
  const res = await axios.get(`/api/videos/recent`, { params: { limit } });
  return res.data as RecentVideo[];
}

export function useListRecentVideos(limit = 12): UseQueryResult<RecentVideo[], Error> {
  return useQuery({ queryKey: ["recentVideos", limit], queryFn: () => fetchRecentVideos(limit) });
}






