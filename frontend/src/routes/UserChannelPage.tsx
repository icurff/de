import { useParams, useNavigate } from "react-router-dom";
import { Header } from "@/components/Header";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import {
  Play,
  Radio,
  Clock,
  Loader2,
  Users,
  Video,
  MoreVertical,
  Settings,
  Trash,
} from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useGetPublicVideosByUsername } from "@/hooks/Video/useGetPublicVideosByUsername";
import { useGetLivestreamRecordingsByUsername } from "@/hooks/Livestream/useGetLivestreamRecordingsByUsername";
import { useState, useCallback } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "@/config/CustomAxios";
import { useToast } from "@/hooks/use-toast";

const UserChannelPage = () => {
  const { atUsername } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { toast } = useToast();
  const queryClient = useQueryClient();
  // Remove @ prefix if present
  const username = atUsername?.startsWith("@")
    ? atUsername.slice(1)
    : atUsername;

  const { data: videos, isLoading: videosLoading } =
    useGetPublicVideosByUsername(username || "", 50);
  const { data: livestreams, isLoading: livestreamsLoading } =
    useGetLivestreamRecordingsByUsername(username || "", 50);

  const isOwner = user?.username === username;

  const formatDuration = (seconds?: number) => {
    if (!seconds) return "";
    const hrs = Math.floor(seconds / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    const secs = Math.floor(seconds % 60);
    if (hrs > 0) {
      return `${hrs}:${mins.toString().padStart(2, "0")}:${secs.toString().padStart(2, "0")}`;
    }
    return `${mins}:${secs.toString().padStart(2, "0")}`;
  };

  const formatDate = (iso?: string) => {
    if (!iso) return "";
    const d = new Date(iso);
    return d.toLocaleDateString("vi-VN");
  };

  const getUserInitials = (username: string) => {
    return username
      .split(" ")
      .map((part) => part[0])
      .join("")
      .slice(0, 2)
      .toUpperCase();
  };

  const handleLivestreamClick = (livestream: any) => {
    // Navigate to play livestream DVR page
    navigate(`/livestream/${livestream.id}`, {
      state: { livestream },
    });
  };

  const { mutateAsync: deleteVideo, isPending: isDeletingVideo } = useMutation({
    mutationFn: async (videoId: string) => {
      await axios.delete(`/api/videos/${videoId}`);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["publicVideos", username] });
      toast({
        title: "Đã xoá video",
        description: "Video đã được xoá thành công.",
      });
    },
    onError: (error: any) => {
      const message = error?.response?.data ?? error?.message ?? "Xoá video thất bại";
      toast({
        title: "Xoá video thất bại",
        description: message,
        variant: "destructive",
      });
    },
  });

  const { mutateAsync: deleteLivestream, isPending: isDeletingLivestream } = useMutation({
    mutationFn: async (livestreamId: string) => {
      await axios.delete(`/api/livestream/${livestreamId}`);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["livestreamRecordings", username] });
      toast({
        title: "Đã xoá livestream",
        description: "Livestream đã được xoá thành công.",
      });
    },
    onError: (error: any) => {
      const message = error?.response?.data ?? error?.message ?? "Xoá livestream thất bại";
      toast({
        title: "Xoá livestream thất bại",
        description: message,
        variant: "destructive",
      });
    },
  });

  const handleDelete = useCallback(
    async (item: any) => {
      const confirmed = window.confirm(
        `Bạn có chắc muốn xoá "${item.title}"?`
      );
      if (!confirmed) {
        return;
      }

      try {
        if (item.type === "video") {
          await deleteVideo(item.id);
        } else {
          await deleteLivestream(item.id);
        }
      } catch (error) {
        // Error is handled by mutation
      }
    },
    [deleteVideo, deleteLivestream]
  );

  if (!username) {
    return (
      <div className="min-h-screen bg-background">
        <Header />
        <div className="flex items-center justify-center min-h-[60vh]">
          <p className="text-muted-foreground">Không tìm thấy kênh</p>
        </div>
      </div>
    );
  }

  const isLoading = videosLoading || livestreamsLoading;
  const allItems = [
    ...(Array.isArray(videos) ? videos.map((v) => ({ ...v, type: "video" as const })) : []),
    ...(Array.isArray(livestreams) ? livestreams.map((l) => ({ ...l, type: "livestream" as const })) : []),
  ].sort((a, b) => {
    const dateA = new Date(a.uploadedDate || 0).getTime();
    const dateB = new Date(b.uploadedDate || 0).getTime();
    return dateB - dateA;
  });

  return (
    <div className="min-h-screen bg-background">
      <Header />
      <div className="container mx-auto px-4 py-6 max-w-7xl">
        {/* Channel Header */}
        <div className="mb-8">
          <div className="flex items-center gap-4 mb-4">
            <Avatar className="h-20 w-20">
              <AvatarFallback className="bg-gradient-to-br from-blue-400 to-blue-600 text-white font-semibold text-2xl">
                {getUserInitials(username)}
              </AvatarFallback>
            </Avatar>
            <div>
              <h1 className="text-2xl font-bold">{username}</h1>
              <p className="text-muted-foreground">@{username}</p>
              <div className="flex items-center gap-4 mt-2">
                <div className="flex items-center gap-1 text-sm text-muted-foreground">
                  <Users className="h-4 w-4" />
                  <span>14.3K subscribers</span>
                </div>
                <div className="flex items-center gap-1 text-sm text-muted-foreground">
                  <Video className="h-4 w-4" />
                  <span>{allItems.length} videos</span>
                </div>
              </div>
            </div>
            <Button className="ml-auto rounded-full">Visit channel</Button>
          </div>
        </div>

        {/* Videos Grid */}
        {isLoading ? (
          <div className="flex items-center justify-center min-h-[40vh]">
            <Loader2 className="h-8 w-8 animate-spin text-primary" />
          </div>
        ) : allItems.length === 0 ? (
          <div className="flex items-center justify-center min-h-[40vh]">
            <p className="text-muted-foreground">Chưa có video nào</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {allItems.map((item) => {
              const thumbnailSrc =
                item.thumbnail && item.thumbnail.trim().length > 0
                  ? item.thumbnail
                  : "/placeholder.svg";

              return (
                <Card
                  key={item.id}
                  className="group cursor-pointer transition-all duration-300 hover:shadow-medium"
                  onClick={() => {
                    if (item.type === "video") {
                      navigate(`/video/${item.id}`);
                    } else {
                      handleLivestreamClick(item);
                    }
                  }}
                >
                  <CardContent className="p-0">
                    {/* Thumbnail */}
                    <div className="relative aspect-video overflow-hidden rounded-t-lg">
                      <img
                        src={thumbnailSrc}
                        alt={item.title}
                        className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
                        loading="lazy"
                      />
                      {item.type === "livestream" && (
                        <div className="absolute top-2 left-2">
                          <Badge
                            variant="destructive"
                            className="uppercase flex items-center gap-1"
                          >
                            <Radio className="h-3 w-3" />
                            Live
                          </Badge>
                        </div>
                      )}
                      <div className="absolute inset-0 bg-black/0 group-hover:bg-black/20 transition-colors duration-300 flex items-center justify-center">
                        <Button
                          size="icon"
                          className="opacity-0 group-hover:opacity-100 transition-opacity duration-300 bg-primary/90 hover:bg-primary text-primary-foreground"
                          onClick={(e) => {
                            e.stopPropagation();
                            if (item.type === "video") {
                              navigate(`/video/${item.id}`);
                            } else {
                              handleLivestreamClick(item);
                            }
                          }}
                        >
                          <Play className="h-6 w-6" />
                        </Button>
                      </div>
                      <div className="absolute bottom-2 right-2 bg-black/80 text-white px-2 py-1 rounded text-xs font-medium flex items-center gap-1">
                        <Clock className="h-3 w-3" />
                        {item.type === "livestream"
                          ? formatDuration(item.duration)
                          : ""}
                      </div>
                    </div>

                    {/* Content */}
                    <div className="p-4 space-y-3">
                      <div className="flex items-start justify-between gap-2">
                        <h3 className="font-semibold line-clamp-2 text-sm leading-5 group-hover:text-primary transition-colors flex-1">
                          {item.title}
                        </h3>
                        {isOwner && (
                          <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                              <Button
                                variant="ghost"
                                size="icon"
                                className="h-8 w-8 shrink-0"
                                onClick={(e) => {
                                  e.stopPropagation();
                                }}
                              >
                                <MoreVertical className="h-4 w-4" />
                              </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                              {item.type === "video" && (
                                <DropdownMenuItem
                                  className="gap-2"
                                  onClick={(e) => {
                                    e.preventDefault();
                                    e.stopPropagation();
                                    navigate(`/manage/videos/${item.id}`);
                                  }}
                                >
                                  <Settings className="h-4 w-4" />
                                  Chỉnh sửa
                                </DropdownMenuItem>
                              )}
                              <DropdownMenuItem
                                className="gap-2 text-destructive focus:text-destructive"
                                disabled={isDeletingVideo || isDeletingLivestream}
                                onClick={(e) => {
                                  e.preventDefault();
                                  e.stopPropagation();
                                  void handleDelete(item);
                                }}
                              >
                                <Trash className="h-4 w-4" />
                                Xóa
                              </DropdownMenuItem>
                            </DropdownMenuContent>
                          </DropdownMenu>
                        )}
                      </div>

                      <div className="flex items-center gap-3">
                        <Avatar className="h-8 w-8">
                          <AvatarFallback className="text-xs bg-primary/10 text-primary">
                            {getUserInitials(username)}
                          </AvatarFallback>
                        </Avatar>
                        <div className="text-xs text-muted-foreground space-y-1">
                          <p className="font-medium text-foreground">
                            {username}
                          </p>
                          <p>{formatDate(item.uploadedDate)}</p>
                        </div>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};

export default UserChannelPage;

