package com.example.demo.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

public class FFmpegUtil {
    public static int[] getVideoResolution(String videoPath) throws IOException {

        int[] resolution = new int[2];

        String commandPath = "src/main/resources/command/get_resolution.sh";
        videoPath = videoPath.replace("\\", "/");
        ProcessBuilder pb = new ProcessBuilder("bash", commandPath, videoPath);
//        ProcessBuilder pb = new ProcessBuilder(
//                "ffprobe", "-v", "error",
//                "-select_streams", "v:0",
//                "-show_entries", "stream=width,height",
//                "-of", "csv=s=x:p=0",
//                videoPath
//        );
        pb.redirectErrorStream(true);
        Process p = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = reader.readLine();
        System.out.println("[FFmpegUtil] Resolution output: " + line);

        if (line == null || !line.contains("x")) {
            throw new IOException("Failed to get video resolution. Output: " + line);
        }

        try {
            String[] parts = line.split("x");
            resolution[0] = Integer.parseInt(parts[0].trim()); // width
            resolution[1] = Integer.parseInt(parts[1].trim()); // height
        } catch (NumberFormatException e) {
            throw new IOException("Invalid resolution format: " + line, e);
        }

        return resolution;
    }

    public static void transcodeVideo(String videoPath, String outputPath, String resolution, String fileName) throws IOException, InterruptedException {
        String commandPath = "src/main/resources/command/transcode_video.sh";

        File outDir = new File(outputPath);
        if (!outDir.exists()) outDir.mkdirs();

        videoPath = videoPath.replace("\\", "/");
        outputPath = outputPath.replace("\\", "/");
        ProcessBuilder pb = new ProcessBuilder("bash", commandPath, videoPath, outputPath, resolution, fileName);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println("[ffmpeg] " + line);
        }

    }

    public static void generateThumbnail(String videoPath, String outputPath, double timestampSeconds) throws IOException, InterruptedException {
        String commandPath = "src/main/resources/command/generate_thumbnail.sh";

        videoPath = videoPath.replace("\\", "/");
        outputPath = outputPath.replace("\\", "/");

        String timestamp = String.format(Locale.US, "%.2f", Math.max(0, timestampSeconds));

        ProcessBuilder pb = new ProcessBuilder("bash", commandPath, videoPath, outputPath, timestamp);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[ffmpeg-thumbnail] " + line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("FFmpeg thumbnail generation failed with exit code " + exitCode);
        }
    }

    public static Integer getVideoDuration(String videoPath) throws IOException, InterruptedException {
        videoPath = videoPath.replace("\\", "/");
        
        ProcessBuilder pb = new ProcessBuilder(
                "ffprobe", "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                videoPath
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line = reader.readLine();
            if (line != null && !line.trim().isEmpty()) {
                try {
                    double durationSeconds = Double.parseDouble(line.trim());
                    return (int) Math.round(durationSeconds);
                } catch (NumberFormatException e) {
                    throw new IOException("Failed to parse duration: " + line, e);
                }
            }
        }
        
        int exitCode = p.waitFor();
        if (exitCode != 0) {
            throw new IOException("FFprobe failed to get video duration with exit code " + exitCode);
        }
        
        throw new IOException("No duration found in ffprobe output");
    }
}
//    public static void transcodeVideo(String input, String outputDir, String height, String name)
//            throws IOException {
//
//        File outDir = new File(outputDir);
//        if (!outDir.exists()) outDir.mkdirs();
//
//        String playlist = outputDir + "/" + name + "_" + height + "p.m3u8";
//        String segmentPattern = outputDir + "/" + name + "_" + height + "p_%06d.ts";
//        ProcessBuilder pb = new ProcessBuilder(
//                "ffmpeg",
//                "-y",
//                "-i", input,
//                "-vf", "scale=-2:" + height,
//                "-c:v", "libx264",
//                "-c:a", "aac",
//                "-threads", "1",
//                "-f", "hls",
//                "-hls_time", "10",
//                "-hls_playlist_type", "vod",
//                "-hls_segment_filename", segmentPattern,
//                playlist
//        );
//
//        pb.redirectErrorStream(true);
//        Process p = pb.start();
//
//        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
//            String line;
//            while ((line = reader.readLine()) != null) {
//                System.out.println("[ffmpeg] " + line);
//            }
//        }
//
/// /        int exitCode = p.waitFor();
/// /        if (exitCode == 0) {
/// /            System.out.println("Transcoding done: " + playlist);
/// /        } else {
/// /            System.err.println("FFmpeg failed with exit code " + exitCode);
/// /            throw new RuntimeException("FFmpeg failed: exit code " + exitCode);
/// /        }
//    }
//}
