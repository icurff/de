import {
  Play,
  MoreVertical,
  Heart,
  Share,
  Clock,
  Trash,
  Lock,
  Globe,
  Settings,
} from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useListRecentVideos } from "@/hooks/Video/useListRecentVideos";
import { useNavigate } from "react-router-dom";
import { useCallback, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "@/config/CustomAxios";
import { useToast } from "@/hooks/use-toast";
import { Badge } from "@/components/ui/badge";
import { useUpdateVideoPrivacy } from "@/hooks/Video/useUpdateVideoPrivacy";
import { type VideoPrivacy } from "@/types/video";

function formatDate(iso?: string) {
  if (!iso) return "";
  const d = new Date(iso);
  return d.toLocaleDateString();
}

export function VideoGrid() {
  const { data: videos, isLoading } = useListRecentVideos(12);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { toast } = useToast();
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);
  const [pendingPrivacyId, setPendingPrivacyId] = useState<string | null>(null);

  const { mutateAsync: deleteVideo, isPending } = useMutation({
    mutationFn: async (videoId: string) => {
      await axios.delete(`/api/videos/${videoId}`);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["recentVideos"] });
    },
  });

  const { isPending: isPrivacyUpdating } = useUpdateVideoPrivacy();

  const handleDelete = useCallback(
    async (videoId: string, title: string) => {
      const confirmed = window.confirm(
        `Bạn có chắc muốn xoá video "${title}"?`
      );
      if (!confirmed) {
        return;
      }

      try {
        setPendingDeleteId(videoId);
        await deleteVideo(videoId);
        toast({
          title: "Đã xoá video",
          description: `Video "${title}" đã được xoá.`,
        });
      } catch (error: any) {
        const message =
          error?.response?.data ?? error?.message ?? "Xoá video thất bại";
        toast({
          title: "Xoá video thất bại",
          description: message,
          variant: "destructive",
        });
      } finally {
        setPendingDeleteId(null);
      }
    },
    [deleteVideo, toast]
  );

  const isAnyActionPending = isPending || isPrivacyUpdating;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold">Recent uploads</h2>
        <Button variant="ghost" className="text-primary hover:text-primary/80">
          View all
        </Button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
        {(videos ?? []).map((video) => {
          const thumbnailSrc =
            video.thumbnail && video.thumbnail.trim().length > 0
              ? video.thumbnail
              : "/placeholder.svg";

          return (
            <Card
              key={video.id}
              className="group cursor-pointer transition-all duration-300 hover:shadow-medium"
              onClick={(e) => {
                e.preventDefault();
                e.stopPropagation();
                navigate(`/manage/videos/${video.id}`);
              }}
            >
              <CardContent className="p-0">
                {/* Thumbnail */}
                <div className="relative aspect-video overflow-hidden rounded-t-lg">
                  <img
                    src={thumbnailSrc}
                    alt={video.title}
                    className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
                    loading="lazy"
                  />
                  <div className="absolute top-2 left-2">
                    <Badge
                      variant={
                        video.privacy === "PRIVATE"
                          ? "destructive"
                          : "secondary"
                      }
                      className="uppercase"
                    >
                      {video.privacy === "PRIVATE" ? "Riêng tư" : "Công khai"}
                    </Badge>
                  </div>
                  <div className="absolute inset-0 bg-black/0 group-hover:bg-black/20 transition-colors duration-300 flex items-center justify-center">
                    <Button
                      size="icon"
                      className="opacity-0 group-hover:opacity-100 transition-opacity duration-300 bg-primary/90 hover:bg-primary text-primary-foreground"
                      onClick={(e) => {
                        e.stopPropagation();
                        navigate(`/video/${video.id}`);
                      }}
                    >
                      <Play className="h-6 w-6" />
                    </Button>
                  </div>
                  <div className="absolute bottom-2 right-2 bg-black/80 text-white px-2 py-1 rounded text-xs font-medium flex items-center gap-1">
                    <Clock className="h-3 w-3" />
                    {/* duration not available yet */}
                  </div>
                </div>

                {/* Content */}
                <div className="p-4 space-y-3">
                  <div className="flex items-start justify-between gap-2">
                    <h3 className="font-semibold line-clamp-2 text-sm leading-5 group-hover:text-primary transition-colors">
                      {video.title}
                    </h3>
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8 shrink-0"
                        >
                          <MoreVertical className="h-4 w-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem
                          className="gap-2"
                          onClick={(e) => {
                            e.preventDefault();
                            e.stopPropagation();
                            navigate(`/manage/videos/${video.id}`);
                          }}
                        >
                          <Settings className="h-4 w-4" />
                          Quản lý video
                        </DropdownMenuItem>

                        <DropdownMenuItem
                          className="gap-2 text-destructive focus:text-destructive"
                          disabled={
                            pendingDeleteId === video.id || isAnyActionPending
                          }
                          onClick={(e) => {
                            e.preventDefault();
                            e.stopPropagation();
                            void handleDelete(video.id, video.title);
                          }}
                        >
                          <Trash className="h-4 w-4" />
                          Delete
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </div>

                  <div className="flex items-center gap-3">
                    <Avatar className="h-8 w-8">
                      <AvatarFallback className="text-xs bg-primary/10 text-primary">
                        {/* Placeholder initials */}
                        {"V"}
                      </AvatarFallback>
                    </Avatar>
                    <div className="text-xs text-muted-foreground space-y-1">
                      <p className="font-medium text-foreground">Your video</p>
                      <p>{formatDate(video.uploadedDate)}</p>
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          );
        })}
      </div>
    </div>
  );
}
