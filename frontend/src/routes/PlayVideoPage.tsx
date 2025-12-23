import { useParams, useNavigate } from "react-router-dom";
import { useGetVideo } from "@/hooks/Video/useGetVideo";
import { Header } from "@/components/Header";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Separator } from "@/components/ui/separator";
import { ArrowLeft, Loader2, ThumbsUp, ThumbsDown, Share2, MoreHorizontal } from "lucide-react";
import { useState } from "react";
import YouTubeVideoPlayer from "@/components/YouTubeVideoPlayer";
import { VideoCommentsSection } from "@/components/VideoComments/VideoCommentsSection";

const PlayVideoPage = () => {
  const { videoId } = useParams<{ videoId: string }>();
  const navigate = useNavigate();
  const { data: video, isLoading, error } = useGetVideo(videoId || "");
  const [showFullDescription, setShowFullDescription] = useState(false);
  const [likes] = useState(0);
  const [isLiked, setIsLiked] = useState(false);
  const [isDisliked, setIsDisliked] = useState(false);

  // Construct video source URL
  const getVideoUrl = () => {
    if (!video) return "";
    const serverLocation = video.server_locations?.[0];
    if (!serverLocation) return "";

    let baseUrl = serverLocation.trim();
    if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
      baseUrl = `http://${baseUrl}`;
    }
    baseUrl = baseUrl.replace(/\/$/, "");
    return `${baseUrl}/videos/${video.username}/${video.id}/master.m3u8`;
  };

  const getThumbnailUrl = () => {
    if (!video) return undefined;
    return video.thumbnail && video.thumbnail.trim().length > 0 ? video.thumbnail : undefined;
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background">
        <Header />
        <div className="flex items-center justify-center min-h-[60vh]">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
        </div>
      </div>
    );
  }

  if (error || !video) {
    return (
      <div className="min-h-screen bg-background">
        <Header />
        <div className="flex flex-col items-center justify-center min-h-[60vh] space-y-4">
          <p className="text-muted-foreground">Không tìm thấy video</p>
          <Button onClick={() => navigate("/")} variant="outline">
            <ArrowLeft className="h-4 w-4 mr-2" />
            Quay lại trang chủ
          </Button>
        </div>
      </div>
    );
  }

  const getUserInitials = (username: string) => {
    return username
      .split(" ")
      .map((part) => part[0])
      .join("")
      .slice(0, 2)
      .toUpperCase();
  };

  return (
    <div className="min-h-screen bg-background">
      <Header />
      <div className="container mx-auto px-4 py-6 max-w-[1280px]">
        {/* YouTube-Style Video Player */}
        <div className="mb-4">
          <YouTubeVideoPlayer
            src={getVideoUrl()}
            poster={getThumbnailUrl()}
          />
        </div>

        {/* Video Title */}
        <div className="mb-3">
          <h1 className="text-xl font-semibold mb-2">{video.title}</h1>
        </div>

        {/* Video Info Row */}
        <div className="flex items-start justify-between gap-4 mb-3">
          {/* Channel Info */}
          <div className="flex items-center gap-3">
            <Avatar 
              className="h-10 w-10 cursor-pointer hover:opacity-80 transition-opacity"
              onClick={() => navigate(`/@${video.username}`)}
            >
              <AvatarFallback className="bg-gradient-to-br from-blue-400 to-blue-600 text-white font-semibold">
                {getUserInitials(video.username)}
              </AvatarFallback>
            </Avatar>
            <div 
              className="flex flex-col cursor-pointer hover:opacity-80 transition-opacity"
              onClick={() => navigate(`/@${video.username}`)}
            >
              <span className="font-semibold text-sm">{video.username}</span>
              <span className="text-xs text-muted-foreground">
                {new Date(video.uploadedDate).toLocaleDateString("vi-VN")}
              </span>
            </div>
            <Button
              variant="default"
              className="ml-4 rounded-full bg-foreground text-background hover:bg-foreground/90"
              onClick={() => navigate(`/@${video.username}`)}
            >
              Visit channel
            </Button>
          </div>

          {/* Action Buttons */}
          <div className="flex items-center gap-2">
            <div className="flex items-center bg-muted rounded-full">
              <Button
                variant="ghost"
                size="sm"
                className={`rounded-l-full hover:bg-muted-foreground/10 ${
                  isLiked ? "text-primary" : ""
                }`}
                onClick={() => {
                  setIsLiked(!isLiked);
                  if (isDisliked) setIsDisliked(false);
                }}
              >
                <ThumbsUp className="h-5 w-5 mr-2" />
                <span className="font-medium">{likes}</span>
              </Button>
              <Separator orientation="vertical" className="h-6" />
              <Button
                variant="ghost"
                size="sm"
                className={`rounded-r-full hover:bg-muted-foreground/10 ${
                  isDisliked ? "text-primary" : ""
                }`}
                onClick={() => {
                  setIsDisliked(!isDisliked);
                  if (isLiked) setIsLiked(false);
                }}
              >
                <ThumbsDown className="h-5 w-5" />
              </Button>
            </div>

            <Button
              variant="ghost"
              size="sm"
              className="rounded-full bg-muted hover:bg-muted-foreground/10"
            >
              <Share2 className="h-5 w-5 mr-2" />
              Share
            </Button>

            <Button
              variant="ghost"
              size="icon"
              className="rounded-full bg-muted hover:bg-muted-foreground/10"
            >
              <MoreHorizontal className="h-5 w-5" />
            </Button>
          </div>
        </div>

        {/* Video Description */}
        <div className="bg-muted/40 rounded-xl p-4 mb-6">
          <div className="flex items-center gap-4 text-sm font-medium mb-2">
            <Badge
              variant={video.privacy === "PRIVATE" ? "destructive" : "secondary"}
            >
              {video.privacy === "PRIVATE" ? "Private" : "Public"}
            </Badge>
            {video.resolutions && video.resolutions.length > 0 && (
              <span className="text-muted-foreground">
                {video.resolutions.join(", ")}
              </span>
            )}
          </div>
          <p
            className={`text-sm whitespace-pre-line ${
              !showFullDescription ? "line-clamp-2" : ""
            }`}
          >
            {video.description || "No description available"}
          </p>
          {video.description && video.description.length > 100 && (
            <Button
              variant="ghost"
              size="sm"
              className="mt-2 p-0 h-auto font-semibold hover:bg-transparent"
              onClick={() => setShowFullDescription(!showFullDescription)}
            >
              {showFullDescription ? "Show less" : "...more"}
            </Button>
          )}
        </div>

        {/* Comments Section */}
        <div>
          <VideoCommentsSection videoId={video.id} />
        </div>
      </div>
    </div>
  );
};

export default PlayVideoPage;

