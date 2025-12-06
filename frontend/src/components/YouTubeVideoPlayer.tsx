import React, { useEffect, useRef, useState, useCallback } from 'react';
import Hls from 'hls.js';
import {
  Play,
  Pause,
  Volume2,
  VolumeX,
  Maximize,
  Minimize,
  Settings,
  Loader2,
  SkipForward,
  SkipBack,
  ChevronRight,
  ChevronLeft,
  Check,
} from 'lucide-react';
import { Button } from './ui/button';
import { Slider } from './ui/slider';

interface Level {
  height: number;
  width: number;
  bitrate: number;
  name?: string;
  index: number;
}

interface YouTubeVideoPlayerProps {
  src: string;
  poster?: string;
  onLevelsChange?: (levels: Level[]) => void;
  onLevelChange?: (level: number) => void;
}

const YouTubeVideoPlayer: React.FC<YouTubeVideoPlayerProps> = ({
  src,
  poster,
  onLevelsChange,
  onLevelChange,
}) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const hlsRef = useRef<Hls | null>(null);
  const controlsTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [volume, setVolume] = useState(1);
  const [isMuted, setIsMuted] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [showControls, setShowControls] = useState(true);
  const [isLoading, setIsLoading] = useState(true);
  const [availableLevels, setAvailableLevels] = useState<Level[]>([]);
  const [currentLevel, setCurrentLevel] = useState<number>(-1);
  const [playbackSpeed, setPlaybackSpeed] = useState(1);
  const [isHovering, setIsHovering] = useState(false);
  const [showSettingsMenu, setShowSettingsMenu] = useState(false);
  const [settingsView, setSettingsView] = useState<'main' | 'quality' | 'speed'>('main');

  // Format time helper
  const formatTime = (seconds: number) => {
    if (isNaN(seconds)) return '0:00';
    const hrs = Math.floor(seconds / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    const secs = Math.floor(seconds % 60);
    if (hrs > 0) {
      return `${hrs}:${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  // Initialize HLS
  useEffect(() => {
    const video = videoRef.current;
    if (!video || !src) return;

    setIsLoading(true);

    if (Hls.isSupported()) {
      const hls = new Hls({
        enableWorker: true,
        lowLatencyMode: false,
        backBufferLength: 90,
        startLevel: -1,
        capLevelToPlayerSize: false,
        maxBufferLength: 20,
        maxMaxBufferLength: 30,
      });

      hlsRef.current = hls;
      hls.loadSource(src);
      hls.attachMedia(video);

      hls.on(Hls.Events.MANIFEST_PARSED, () => {
        setIsLoading(false);
        const levels = hls.levels.map((level, index) => ({
          height: level.height || 0,
          width: level.width || 0,
          bitrate: level.bitrate || 0,
          name: level.name || `${level.height}p`,
          index: index,
        }));
        const sortedLevels = [...levels].sort((a, b) => b.height - a.height);
        setAvailableLevels(sortedLevels);
        setCurrentLevel(hls.currentLevel);
        onLevelsChange?.(sortedLevels);
      });

      hls.on(Hls.Events.LEVEL_SWITCHED, (event, data) => {
        setCurrentLevel(data.level);
        onLevelChange?.(data.level);
      });

      hls.on(Hls.Events.ERROR, (event, data) => {
        if (data.fatal) {
          switch (data.type) {
            case Hls.ErrorTypes.NETWORK_ERROR:
              hls.startLoad();
              break;
            case Hls.ErrorTypes.MEDIA_ERROR:
              hls.recoverMediaError();
              break;
            default:
              hls.destroy();
              break;
          }
        }
      });

      return () => {
        hls.destroy();
      };
    } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
      video.src = src;
      video.addEventListener('loadedmetadata', () => {
        setIsLoading(false);
      });
    }
  }, [src, onLevelsChange, onLevelChange]);

  // Video event listeners
  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    const handlePlay = () => setIsPlaying(true);
    const handlePause = () => setIsPlaying(false);
    const handleTimeUpdate = () => setCurrentTime(video.currentTime);
    const handleDurationChange = () => setDuration(video.duration);
    const handleVolumeChange = () => {
      setVolume(video.volume);
      setIsMuted(video.muted);
    };
    const handleWaiting = () => setIsLoading(true);
    const handleCanPlay = () => setIsLoading(false);

    video.addEventListener('play', handlePlay);
    video.addEventListener('pause', handlePause);
    video.addEventListener('timeupdate', handleTimeUpdate);
    video.addEventListener('durationchange', handleDurationChange);
    video.addEventListener('volumechange', handleVolumeChange);
    video.addEventListener('waiting', handleWaiting);
    video.addEventListener('canplay', handleCanPlay);

    return () => {
      video.removeEventListener('play', handlePlay);
      video.removeEventListener('pause', handlePause);
      video.removeEventListener('timeupdate', handleTimeUpdate);
      video.removeEventListener('durationchange', handleDurationChange);
      video.removeEventListener('volumechange', handleVolumeChange);
      video.removeEventListener('waiting', handleWaiting);
      video.removeEventListener('canplay', handleCanPlay);
    };
  }, []);

  // Hide controls after inactivity
  useEffect(() => {
    if (!isPlaying || isHovering || showSettingsMenu) {
      setShowControls(true);
      return;
    }

    const timeout = setTimeout(() => {
      setShowControls(false);
      // Close settings menu when controls hide
      setShowSettingsMenu(false);
      setSettingsView('main');
    }, 3000);

    return () => clearTimeout(timeout);
  }, [isPlaying, isHovering, showSettingsMenu]);

  // Keyboard shortcuts
  useEffect(() => {
    const handleKeyPress = (e: KeyboardEvent) => {
      if (!videoRef.current) return;

      switch (e.key) {
        case ' ':
        case 'k':
          e.preventDefault();
          togglePlayPause();
          break;
        case 'ArrowLeft':
          e.preventDefault();
          skip(-10);
          break;
        case 'ArrowRight':
          e.preventDefault();
          skip(10);
          break;
        case 'ArrowUp':
          e.preventDefault();
          changeVolume(0.1);
          break;
        case 'ArrowDown':
          e.preventDefault();
          changeVolume(-0.1);
          break;
        case 'm':
          e.preventDefault();
          toggleMute();
          break;
        case 'f':
          e.preventDefault();
          toggleFullscreen();
          break;
      }
    };

    window.addEventListener('keydown', handleKeyPress);
    return () => window.removeEventListener('keydown', handleKeyPress);
  }, []);

  // Fullscreen change listener
  useEffect(() => {
    const handleFullscreenChange = () => {
      setIsFullscreen(!!document.fullscreenElement);
    };

    document.addEventListener('fullscreenchange', handleFullscreenChange);
    return () => document.removeEventListener('fullscreenchange', handleFullscreenChange);
  }, []);

  // Close settings menu when clicking outside
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (showSettingsMenu && containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setShowSettingsMenu(false);
        setSettingsView('main');
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [showSettingsMenu]);

  const togglePlayPause = () => {
    if (!videoRef.current) return;
    if (isPlaying) {
      videoRef.current.pause();
    } else {
      videoRef.current.play();
    }
    // Close settings menu when playing/pausing
    if (showSettingsMenu) {
      setShowSettingsMenu(false);
      setSettingsView('main');
    }
  };

  const skip = (seconds: number) => {
    if (!videoRef.current) return;
    videoRef.current.currentTime = Math.max(
      0,
      Math.min(duration, videoRef.current.currentTime + seconds)
    );
  };

  const handleProgressChange = (value: number[]) => {
    if (!videoRef.current) return;
    const newTime = (value[0] / 100) * duration;
    videoRef.current.currentTime = newTime;
    setCurrentTime(newTime);
  };

  const changeVolume = (delta: number) => {
    if (!videoRef.current) return;
    const newVolume = Math.max(0, Math.min(1, volume + delta));
    videoRef.current.volume = newVolume;
    setVolume(newVolume);
    if (newVolume > 0 && isMuted) {
      videoRef.current.muted = false;
      setIsMuted(false);
    }
  };

  const handleVolumeChange = (value: number[]) => {
    if (!videoRef.current) return;
    const newVolume = value[0] / 100;
    videoRef.current.volume = newVolume;
    setVolume(newVolume);
    if (newVolume > 0 && isMuted) {
      videoRef.current.muted = false;
      setIsMuted(false);
    }
  };

  const toggleMute = () => {
    if (!videoRef.current) return;
    videoRef.current.muted = !isMuted;
    setIsMuted(!isMuted);
  };

  const toggleFullscreen = async () => {
    if (!containerRef.current) return;

    try {
      if (!isFullscreen) {
        await containerRef.current.requestFullscreen();
      } else {
        await document.exitFullscreen();
      }
    } catch (error) {
      console.error('Fullscreen error:', error);
    }
  };

  const handleQualityChange = (levelIndex: number) => {
    if (!hlsRef.current || !videoRef.current) return;

    const hls = hlsRef.current;
    const wasPlaying = !videoRef.current.paused;
    const currentTime = videoRef.current.currentTime;

    if (levelIndex === -1) {
      hls.loadLevel = -1;
      hls.nextLevel = -1;
      setCurrentLevel(-1);
    } else {
      hls.nextLevel = levelIndex;
      hls.loadLevel = levelIndex;

      hls.stopLoad();
      setTimeout(() => {
        hls.startLoad(currentTime);
        if (hls.currentLevel !== levelIndex) {
          hls.nextLevel = levelIndex;
          hls.loadLevel = levelIndex;
        }
      }, 20);

      setCurrentLevel(levelIndex);
    }

    if (wasPlaying && videoRef.current.paused) {
      setTimeout(() => {
        videoRef.current?.play().catch(console.error);
      }, 100);
    }
  };

  const handleSpeedChange = (speed: number) => {
    if (!videoRef.current) return;
    videoRef.current.playbackRate = speed;
    setPlaybackSpeed(speed);
    setShowSettingsMenu(false);
    setSettingsView('main');
  };

  const toggleSettingsMenu = () => {
    setShowSettingsMenu(!showSettingsMenu);
    setSettingsView('main');
  };

  const handleQualitySelect = (levelIndex: number) => {
    handleQualityChange(levelIndex);
    setShowSettingsMenu(false);
    setSettingsView('main');
  };

  const progressPercentage = duration > 0 ? (currentTime / duration) * 100 : 0;

  return (
    <div
      ref={containerRef}
      className="relative bg-black rounded-xl overflow-hidden aspect-video group"
      onMouseEnter={() => setIsHovering(true)}
      onMouseLeave={() => setIsHovering(false)}
    >
      {/* Video Element */}
      <video
        ref={videoRef}
        className="w-full h-full"
        poster={poster}
        playsInline
        onClick={togglePlayPause}
      />

      {/* Loading Spinner */}
      {isLoading && (
        <div className="absolute inset-0 flex items-center justify-center bg-black/50">
          <Loader2 className="h-12 w-12 animate-spin text-white" />
        </div>
      )}

      {/* Center Play/Pause Button */}
      {!isPlaying && !isLoading && (
        <div className="absolute inset-0 flex items-center justify-center">
          <Button
            size="icon"
            variant="ghost"
            className="h-20 w-20 rounded-full bg-black/60 hover:bg-black/80 text-white"
            onClick={togglePlayPause}
          >
            <Play className="h-10 w-10 ml-1" fill="white" />
          </Button>
        </div>
      )}

      {/* Controls Overlay */}
      <div
        className={`absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/90 via-black/60 to-transparent transition-opacity duration-300 ${
          showControls || !isPlaying ? 'opacity-100' : 'opacity-0'
        }`}
      >
        {/* Progress Bar */}
        <div className="px-4 pt-4">
          <Slider
            value={[progressPercentage]}
            onValueChange={handleProgressChange}
            max={100}
            step={0.1}
            className="cursor-pointer"
          />
        </div>

        {/* Control Buttons */}
        <div className="flex items-center justify-between px-4 py-3">
          <div className="flex items-center gap-2">
            {/* Play/Pause */}
            <Button
              size="icon"
              variant="ghost"
              className="text-white hover:bg-white/20 h-9 w-9"
              onClick={togglePlayPause}
            >
              {isPlaying ? <Pause className="h-5 w-5" /> : <Play className="h-5 w-5" />}
            </Button>

            {/* Skip Backward */}
            <Button
              size="icon"
              variant="ghost"
              className="text-white hover:bg-white/20 h-9 w-9"
              onClick={() => skip(-10)}
            >
              <SkipBack className="h-5 w-5" />
            </Button>

            {/* Skip Forward */}
            <Button
              size="icon"
              variant="ghost"
              className="text-white hover:bg-white/20 h-9 w-9"
              onClick={() => skip(10)}
            >
              <SkipForward className="h-5 w-5" />
            </Button>

            {/* Volume */}
            <div className="flex items-center gap-2 group/volume">
              <Button
                size="icon"
                variant="ghost"
                className="text-white hover:bg-white/20 h-9 w-9"
                onClick={toggleMute}
              >
                {isMuted || volume === 0 ? (
                  <VolumeX className="h-5 w-5" />
                ) : (
                  <Volume2 className="h-5 w-5" />
                )}
              </Button>
              <div className="w-0 group-hover/volume:w-20 overflow-hidden transition-all duration-200">
                <Slider
                  value={[isMuted ? 0 : volume * 100]}
                  onValueChange={handleVolumeChange}
                  max={100}
                  step={1}
                  className="cursor-pointer"
                />
              </div>
            </div>

            {/* Time */}
            <span className="text-white text-sm font-medium ml-2">
              {formatTime(currentTime)} / {formatTime(duration)}
            </span>
          </div>

          <div className="flex items-center gap-2">
            {/* Settings */}
            <Button
              size="icon"
              variant="ghost"
              className="text-white hover:bg-white/20 h-9 w-9"
              onClick={toggleSettingsMenu}
            >
              <Settings className="h-5 w-5" />
            </Button>

            {/* Fullscreen */}
            <Button
              size="icon"
              variant="ghost"
              className="text-white hover:bg-white/20 h-9 w-9"
              onClick={toggleFullscreen}
            >
              {isFullscreen ? (
                <Minimize className="h-5 w-5" />
              ) : (
                <Maximize className="h-5 w-5" />
              )}
            </Button>
          </div>
        </div>
      </div>

      {/* Settings Menu - In Video */}
      {showSettingsMenu && (showControls || !isPlaying) && (
        <div className="absolute bottom-20 right-4 w-80 bg-black/95 backdrop-blur-md text-white rounded-lg shadow-2xl overflow-hidden z-50 transition-opacity duration-300">
          {/* Main Settings View */}
          {settingsView === 'main' && (
            <div className="py-2">
              {/* Quality Option */}
              {availableLevels.length > 0 && (
                <button
                  onClick={() => setSettingsView('quality')}
                  className="w-full px-4 py-3 flex items-center justify-between hover:bg-white/10 transition-colors"
                >
                  <div className="flex items-center gap-3">
                    <Settings className="h-5 w-5" />
                    <span className="text-sm font-medium">Quality</span>
                  </div>
                  <div className="flex items-center gap-2 text-gray-400">
                    <span className="text-sm">
                      {currentLevel === -1 
                        ? `Auto (${availableLevels.find(l => l.index === hlsRef.current?.currentLevel)?.height || ''}p)` 
                        : `${availableLevels.find(l => l.index === currentLevel)?.height || ''}p`}
                    </span>
                    <ChevronRight className="h-4 w-4" />
                  </div>
                </button>
              )}

              {/* Playback Speed Option */}
              <button
                onClick={() => setSettingsView('speed')}
                className="w-full px-4 py-3 flex items-center justify-between hover:bg-white/10 transition-colors"
              >
                <div className="flex items-center gap-3">
                  <Settings className="h-5 w-5" />
                  <span className="text-sm font-medium">Playback speed</span>
                </div>
                <div className="flex items-center gap-2 text-gray-400">
                  <span className="text-sm">
                    {playbackSpeed === 1 ? 'Normal' : `${playbackSpeed}x`}
                  </span>
                  <ChevronRight className="h-4 w-4" />
                </div>
              </button>
            </div>
          )}

          {/* Quality Selection View */}
          {settingsView === 'quality' && (
            <div>
              {/* Header */}
              <div className="px-4 py-3 border-b border-white/10 flex items-center gap-3">
                <button
                  onClick={() => setSettingsView('main')}
                  className="hover:bg-white/10 p-1 rounded transition-colors"
                >
                  <ChevronLeft className="h-5 w-5" />
                </button>
                <span className="text-sm font-medium">Quality</span>
              </div>
              {/* Quality Options */}
              <div className="py-2 max-h-80 overflow-y-auto">
                <button
                  onClick={() => handleQualitySelect(-1)}
                  className="w-full px-4 py-3 flex items-center justify-between hover:bg-white/10 transition-colors"
                >
                  <span className="text-sm">Auto</span>
                  {currentLevel === -1 && <Check className="h-5 w-5 text-blue-500" />}
                </button>
                {availableLevels.map((level) => (
                  <button
                    key={level.index}
                    onClick={() => handleQualitySelect(level.index)}
                    className="w-full px-4 py-3 flex items-center justify-between hover:bg-white/10 transition-colors"
                  >
                    <span className="text-sm">{level.height}p</span>
                    {currentLevel === level.index && <Check className="h-5 w-5 text-blue-500" />}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Playback Speed Selection View */}
          {settingsView === 'speed' && (
            <div>
              {/* Header */}
              <div className="px-4 py-3 border-b border-white/10 flex items-center gap-3">
                <button
                  onClick={() => setSettingsView('main')}
                  className="hover:bg-white/10 p-1 rounded transition-colors"
                >
                  <ChevronLeft className="h-5 w-5" />
                </button>
                <span className="text-sm font-medium">Playback speed</span>
              </div>
              {/* Speed Options */}
              <div className="py-2 max-h-80 overflow-y-auto">
                {[0.25, 0.5, 0.75, 1, 1.25, 1.5, 1.75, 2].map((speed) => (
                  <button
                    key={speed}
                    onClick={() => handleSpeedChange(speed)}
                    className="w-full px-4 py-3 flex items-center justify-between hover:bg-white/10 transition-colors"
                  >
                    <span className="text-sm">
                      {speed === 1 ? 'Normal' : `${speed}x`}
                    </span>
                    {playbackSpeed === speed && <Check className="h-5 w-5 text-blue-500" />}
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Keyboard shortcuts hint (optional) */}
      <div className="absolute top-4 left-4 opacity-0 group-hover:opacity-100 transition-opacity duration-300">
        <div className="bg-black/70 text-white text-xs px-3 py-2 rounded-lg backdrop-blur-sm">
          <div className="font-semibold mb-1">Keyboard shortcuts:</div>
          <div>Space/K: Play/Pause • ←/→: Skip • ↑/↓: Volume</div>
          <div>M: Mute • F: Fullscreen</div>
        </div>
      </div>
    </div>
  );
};

export default YouTubeVideoPlayer;

