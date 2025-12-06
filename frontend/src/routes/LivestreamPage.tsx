import { useState, useEffect } from "react";
import { Header } from "@/components/Header";
import { Sidebar } from "@/components/Sidebar";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import axios from "@/config/CustomAxios";
import { toast } from "sonner";

interface LiveStream {
  id: string;
  userId: string;
  title: string;
  description: string;
  streamKey: string;
  isLive: boolean;
  hlsUrl: string;
}

const LivestreamPage = () => {
  const [stream, setStream] = useState<LiveStream | null>(null);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [loading, setLoading] = useState(false);
  const [showKey, setShowKey] = useState(false);

  useEffect(() => {
    fetchStreamInfo();
  }, []);

  const fetchStreamInfo = async () => {
    try {
      const response = await axios.get("/api/livestream");
      setStream(response.data);
      setTitle(response.data.title || "");
      setDescription(response.data.description || "");
    } catch (error) {
      console.error("Failed to fetch stream info", error);
    }
  };

  const handleSetup = async () => {
    setLoading(true);
    try {
      const response = await axios.post(
        "/api/livestream/setup",
        { title, description }
      );
      setStream(response.data);
      toast.success("Stream info updated");
    } catch (error) {
      toast.error("Failed to update stream info");
    } finally {
      setLoading(false);
    }
  };

  const handleResetKey = async () => {
    if (!confirm("Are you sure? This will invalidate the old key.")) return;
    try {
      const response = await axios.post(
        "/api/livestream/reset-key",
        {}
      );
      setStream(response.data);
      toast.success("Stream key reset");
    } catch (error) {
      toast.error("Failed to reset stream key");
    }
  };

  const rtmpUrl = "rtmp://localhost:1935/live";
  // Assuming the HLS URL is constructed like this or returned by backend
  // The backend model has hlsUrl field, but we didn't populate it in the service yet.
  // Let's construct it client side or assume backend sends it.
  // For now, let's construct it: http://localhost:8088/live/{streamKey}.m3u8
  // Wait, SRS default HLS path is usually http://server:port/app/stream.m3u8
  // In srs.conf: output rtmp://.../[app]?vhost=[vhost]/[stream]_[engine];
  // And hls_path ./objs/nginx/html;
  // If we push to rtmp://localhost:1935/live/{streamKey}
  // The app is 'live'. Stream is '{streamKey}'.
  // HLS URL should be http://localhost:8088/live/{streamKey}.m3u8 (if no transcoding suffix)
  // But we have transcoding.
  // engine ff -> [stream]_ff
  // engine sd -> [stream]_sd
  // engine ld -> [stream]_ld
  // So we have 3 streams.
  // We probably want a master playlist or just pick one.
  // Let's just use the high quality one for now: http://localhost:8088/live/{streamKey}_ff.m3u8

  // Transcoding is working!
  // Use the high quality stream by default.
  const playbackUrl = stream ? `http://localhost:8088/live/${stream.streamKey}_ff.m3u8` : "";

  return (
    <div className="min-h-screen bg-background">
      <Header />
      <div className="flex">
        <Sidebar />
        <main className="flex-1 p-8 space-y-8">
          <h1 className="text-3xl font-bold">Livestream Setup</h1>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            <Card>
              <CardHeader>
                <CardTitle>Stream Details</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div>
                  <label className="block text-sm font-medium mb-1">Title</label>
                  <Input value={title} onChange={(e) => setTitle(e.target.value)} />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">Description</label>
                  <Textarea value={description} onChange={(e) => setDescription(e.target.value)} />
                </div>
                <div className="flex gap-4">
                  <Button onClick={handleSetup} disabled={loading}>
                    {loading ? "Saving..." : "Save Details"}
                  </Button>
                  {stream && (
                    <Button 
                      variant="secondary" 
                      onClick={() => window.open(`/live/${stream.userId}`, '_blank')}
                    >
                      Start Livestream
                    </Button>
                  )}
                </div>
              </CardContent>
            </Card>

            {stream && (
              <Card>
                <CardHeader>
                  <CardTitle>Connection Info</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium mb-1">Public Link</label>
                    <div className="flex gap-2">
                        <Input value={`${window.location.origin}/live/${stream.userId}`} readOnly />
                        <Button variant="outline" onClick={() => navigator.clipboard.writeText(`${window.location.origin}/live/${stream.userId}`)}>Copy</Button>
                        <Button variant="ghost" onClick={() => window.open(`/live/${stream.userId}`, '_blank')}>Open</Button>
                    </div>
                    <p className="text-xs text-muted-foreground mt-1">
                        Share this link with your viewers.
                    </p>
                  </div>

                  <Separator />

                  <div>
                    <label className="block text-sm font-medium mb-1">RTMP URL</label>
                    <div className="flex gap-2">
                        <Input value={rtmpUrl} readOnly />
                        <Button variant="outline" onClick={() => navigator.clipboard.writeText(rtmpUrl)}>Copy</Button>
                    </div>
                  </div>
                  <div>
                    <label className="block text-sm font-medium mb-1">Stream Key</label>
                    <div className="flex gap-2">
                        <Input type={showKey ? "text" : "password"} value={stream.streamKey} readOnly />
                        <Button variant="ghost" onClick={() => setShowKey(!showKey)}>{showKey ? "Hide" : "Show"}</Button>
                        <Button variant="outline" onClick={() => navigator.clipboard.writeText(stream.streamKey)}>Copy</Button>
                    </div>
                  </div>
                  <Button variant="destructive" onClick={handleResetKey}>Reset Key</Button>
                  
                  <div className="pt-4 border-t">
                    <h4 className="text-sm font-medium mb-2">Server Management</h4>
                    <a 
                        href="http://localhost:8088/console/ng_index.html" 
                        target="_blank" 
                        rel="noopener noreferrer"
                        className="text-sm text-blue-500 hover:underline flex items-center gap-1"
                    >
                        Open SRS Console
                    </a>
                    <p className="text-xs text-muted-foreground mt-1">
                        Use the console to monitor streams and clients.
                    </p>
                  </div>
                </CardContent>
              </Card>
            )}
          </div>

        </main>
      </div>
    </div>
  );
};

export default LivestreamPage;
