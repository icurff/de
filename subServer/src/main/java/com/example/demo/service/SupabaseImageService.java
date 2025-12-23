package com.example.demo.service;

import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class SupabaseImageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Value("${supabase.bucket:images}")
    private String bucketName;

    private static final MediaType IMAGE_JPEG = MediaType.parse("image/jpeg");
    private static final MediaType IMAGE_PNG = MediaType.parse("image/png");
    private static final MediaType IMAGE_WEBP = MediaType.parse("image/webp");

    private final OkHttpClient httpClient = new OkHttpClient();

    /**
     * Upload an image file from a local path to Supabase Storage
     * @param localFilePath The local file path
     * @param folder The folder path in the bucket (e.g., "avatars", "thumbnails/videos", "thumbnails/livestreams")
     * @param fileName The file name in Supabase
     * @return The public URL of the uploaded image
     * @throws IOException if upload fails
     */
    public String uploadImageFromPath(Path localFilePath, String folder, String fileName) throws IOException {
        if (!Files.exists(localFilePath)) {
            throw new IllegalArgumentException("File does not exist: " + localFilePath);
        }

        String originalFilename = localFilePath.getFileName().toString();
        String extension = getFileExtension(originalFilename);
        
        if (fileName == null || fileName.isEmpty()) {
            fileName = UUID.randomUUID().toString() + extension;
        } else if (!fileName.contains(".")) {
            fileName = fileName + extension;
        }

        String filePath = folder != null && !folder.isEmpty() 
            ? folder + "/" + fileName 
            : fileName;

        byte[] fileBytes = Files.readAllBytes(localFilePath);
        RequestBody requestBody = RequestBody.create(fileBytes, getMediaType(extension));

        Request request = new Request.Builder()
                .url(supabaseUrl + "/storage/v1/object/" + bucketName + "/" + filePath)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .addHeader("apikey", supabaseKey)
                .addHeader("Content-Type", getContentType(extension))
                .put(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                throw new IOException("Failed to upload image to Supabase: " + response.code() + " - " + errorBody);
            }

            // Construct public URL
            return supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + filePath;
        }
    }

    /**
     * Delete an image from Supabase Storage
     * @param filePath The file path in the bucket (e.g., "avatars/user123.jpg")
     * @throws IOException if deletion fails
     */
    public void deleteImage(String filePath) throws IOException {
        Request request = new Request.Builder()
                .url(supabaseUrl + "/storage/v1/object/" + bucketName + "/" + filePath)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .addHeader("apikey", supabaseKey)
                .delete()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() && response.code() != 404) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                throw new IOException("Failed to delete image from Supabase: " + response.code() + " - " + errorBody);
            }
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private MediaType getMediaType(String extension) {
        return switch (extension.toLowerCase()) {
            case ".png" -> IMAGE_PNG;
            case ".webp" -> IMAGE_WEBP;
            default -> IMAGE_JPEG;
        };
    }

    private String getContentType(String extension) {
        return switch (extension.toLowerCase()) {
            case ".png" -> "image/png";
            case ".webp" -> "image/webp";
            default -> "image/jpeg";
        };
    }
}

