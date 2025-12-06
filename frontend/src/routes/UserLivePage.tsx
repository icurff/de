import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Header } from "@/components/Header";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { ArrowLeft, Loader2 } from "lucide-react";
import YouTubeVideoPlayer from "@/components/YouTubeVideoPlayer";
import axios from "@/config/CustomAxios";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";

interface LiveStream {
  id: string;
  userId: string;
  title: string;
  description: string;
  isLive: boolean;
  hlsUrl: string;
  streamKey?: string;
}

const UserLivePage = () => {
  const { userId } = useParams();
  const navigate = useNavigate();
  const [stream, setStream] = useState<LiveStream | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (userId) {
      fetchStreamInfo(userId);
    }
  }, [userId]);

  const fetchStreamInfo = async (id: string) => {
    try {
      const response = await axios.get(`/api/livestream/user/${id}`);
      setStream(response.data);
    } catch (err) {
      console.error("Failed to fetch stream info", err);
      setError("Failed to load stream information.");
    } finally {
      setLoading(false);
    }
  };

  const playbackUrl = stream?.hlsUrl || "";

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

  if (error || !stream) {
    return (
      <div className="min-h-screen bg-background">
        <Header />
        <div className="flex flex-col items-center justify-center min-h-[60vh] space-y-4">
          <p className="text-muted-foreground">{error || "Stream not found"}</p>
          <Button onClick={() => navigate("/")} variant="outline">
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back to Home
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <Header />
      <div className="container mx-auto px-4 py-6 max-w-7xl">
        <Button
          onClick={() => navigate("/")}
          variant="ghost"
          className="mb-4"
        >
          <ArrowLeft className="h-4 w-4 mr-2" />
          Back
        </Button>

        <div className="grid gap-8 lg:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
          <div className="space-y-6">
            {/* YouTube-Style Video Player */}
            <YouTubeVideoPlayer src={playbackUrl} />

            {/* Stream Info */}
            <div className="space-y-4">
              <div>
                <h1 className="text-3xl font-bold">{stream.title || "Untitled Stream"}</h1>
                <div className="mt-2 flex flex-wrap gap-2">
                  <Badge variant="destructive" className="uppercase animate-pulse">
                    LIVE
                  </Badge>
                  <Badge variant="secondary">
                    Public
                  </Badge>
                </div>
                
                <div className="flex items-center gap-4 mt-4 p-4 bg-card rounded-lg border">
                    <Avatar className="h-12 w-12">
                        <AvatarImage src={`https://api.dicebear.com/7.x/avataaars/svg?seed=${stream.userId}`} />
                        <AvatarFallback>{stream.userId.substring(0, 2).toUpperCase()}</AvatarFallback>
                    </Avatar>
                    <div>
                        <p className="font-semibold text-lg">{stream.userId}</p>
                        <p className="text-sm text-muted-foreground">Streamer</p>
                    </div>
                </div>

                <p className="text-muted-foreground mt-4 whitespace-pre-wrap">
                  {stream.description || "No description provided."}
                </p>
              </div>
            </div>
          </div>

          <aside className="h-fit space-y-4">
             {/* Chat Section Placeholder - mimicking the comments section style */}
            <Card className="h-[600px] flex flex-col">
                <CardHeader className="border-b py-3">
                    <CardTitle className="text-sm font-medium">Live Chat</CardTitle>
                </CardHeader>
                <CardContent className="flex-1 flex items-center justify-center bg-muted/10">
                    <div className="text-center text-muted-foreground">
                        <p>Chat is connecting...</p>
                        <p className="text-xs mt-1">(Chat feature coming soon)</p>
                    </div>
                </CardContent>
            </Card>
          </aside>
        </div>
      </div>
    </div>
  );
};

export default UserLivePage;
