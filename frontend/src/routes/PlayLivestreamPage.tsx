import { useParams, useNavigate, useLocation } from "react-router-dom";
import { Header } from "@/components/Header";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { ArrowLeft, Loader2, Radio } from "lucide-react";
import { useState, useEffect } from "react";
import YouTubeVideoPlayer from "@/components/YouTubeVideoPlayer";
import axios from "@/config/CustomAxios";

const PlayLivestreamPage = () => {
  const { livestreamId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const [livestream, setLivestream] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Get livestream from location state or fetch it
    if (location.state?.livestream) {
      setLivestream(location.state.livestream);
      setLoading(false);
    } else if (livestreamId) {
      // Fetch livestream by ID
      axios
        .get(`/api/livestream/${livestreamId}`)
        .then((response) => {
          setLivestream(response.data);
          setLoading(false);
        })
        .catch((error) => {
          console.error("Failed to fetch livestream:", error);
          navigate("/", { replace: true });
        });
    } else {
      navigate("/", { replace: true });
    }
  }, [location.state, livestreamId, navigate]);

  const getVideoUrl = () => {
    if (!livestream?.dvrPath) return "";
    
    const dvrPath = livestream.dvrPath;
    
    // If it's already a URL, return it
    if (dvrPath.startsWith("http://") || dvrPath.startsWith("https://")) {
      return dvrPath;
    }
    
    // Construct URL from server location
    // DVR files are stored in: /storage/outputs/username/livestreams/livestreamId/file.mp4
    // They should be served via: http://serverLocation/livestreams/username/livestreamId/file.mp4
    const serverLocation = livestream.serverLocation || "localhost:80";
    let baseUrl = serverLocation.trim();
    if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
      baseUrl = `http://${baseUrl}`;
    }
    baseUrl = baseUrl.replace(/\/$/, "");
    
    // Extract filename from dvrPath
    // dvrPath format: /storage/outputs/username/livestreams/livestreamId/file.mp4
    // We need: /livestreams/username/livestreamId/file.mp4
    const pathParts = dvrPath.split("/");
    const livestreamsIndex = pathParts.findIndex((part) => part === "livestreams");
    if (livestreamsIndex >= 0) {
      // Get username (the part before "livestreams")
      const username = pathParts[livestreamsIndex - 1];
      // Get everything after "livestreams" (livestreamId/file.mp4)
      const afterLivestreams = pathParts.slice(livestreamsIndex + 1).join("/");
      return `${baseUrl}/livestreams/${username}/${afterLivestreams}`;
    }
    
    // Fallback: try to construct path manually
    const fileName = pathParts[pathParts.length - 1];
    return `${baseUrl}/livestreams/${livestream.username}/${livestreamId}/${fileName}`;
  };

  const getThumbnailUrl = () => {
    if (!livestream) return undefined;
    return livestream.thumbnail && livestream.thumbnail.trim().length > 0
      ? livestream.thumbnail
      : undefined;
  };

  const getUserInitials = (username: string) => {
    return username
      .split(" ")
      .map((part) => part[0])
      .join("")
      .slice(0, 2)
      .toUpperCase();
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-background">
        <Header />
        <div className="flex items-center justify-center min-h-[60vh]">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
        </div>
      </div>
    );
  }

  if (!livestream) {
    return (
      <div className="min-h-screen bg-background">
        <Header />
        <div className="flex flex-col items-center justify-center min-h-[60vh] space-y-4">
          <p className="text-muted-foreground">Không tìm thấy livestream</p>
          <Button onClick={() => navigate("/")} variant="outline">
            <ArrowLeft className="h-4 w-4 mr-2" />
            Quay lại trang chủ
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <Header />
      <div className="container mx-auto px-4 py-6 max-w-[1280px]">
        <Button
          onClick={() => navigate(`/@${livestream.username}`)}
          variant="ghost"
          className="mb-4"
        >
          <ArrowLeft className="h-4 w-4 mr-2" />
          Quay lại kênh
        </Button>

        {/* Video Player */}
        <div className="mb-4">
          <YouTubeVideoPlayer src={getVideoUrl()} poster={getThumbnailUrl()} />
        </div>

        {/* Livestream Title */}
        <div className="mb-3">
          <h1 className="text-xl font-semibold mb-2">{livestream.title}</h1>
        </div>

        {/* Livestream Info Row */}
        <div className="flex items-start justify-between gap-4 mb-3">
          {/* Channel Info */}
          <div className="flex items-center gap-3">
            <Avatar 
              className="h-10 w-10 cursor-pointer hover:opacity-80 transition-opacity"
              onClick={() => navigate(`/@${livestream.username}`)}
            >
              <AvatarFallback className="bg-gradient-to-br from-blue-400 to-blue-600 text-white font-semibold">
                {getUserInitials(livestream.username)}
              </AvatarFallback>
            </Avatar>
            <div 
              className="flex flex-col cursor-pointer hover:opacity-80 transition-opacity"
              onClick={() => navigate(`/@${livestream.username}`)}
            >
              <span className="font-semibold text-sm">{livestream.username}</span>
              <span className="text-xs text-muted-foreground">
                {new Date(livestream.uploadedDate).toLocaleDateString("vi-VN")}
              </span>
            </div>
            <Button
              variant="default"
              className="ml-4 rounded-full bg-foreground text-background hover:bg-foreground/90"
              onClick={() => navigate(`/@${livestream.username}`)}
            >
              Visit channel
            </Button>
          </div>

          {/* Badge */}
          <Badge variant="destructive" className="uppercase flex items-center gap-1">
            <Radio className="h-3 w-3" />
            Livestream
          </Badge>
        </div>

        {/* Description */}
        {livestream.description && (
          <div className="bg-muted/40 rounded-xl p-4 mb-6">
            <p className="text-sm whitespace-pre-line">
              {livestream.description}
            </p>
          </div>
        )}
      </div>
    </div>
  );
};

export default PlayLivestreamPage;

