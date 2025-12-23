package com.example.demo.service;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.ERole;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SupabaseImageService supabaseImageService;

    public Page<User> getUsersPage(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public User getUserById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    public User updateUserRoles(String id, Set<ERole> roles) {
        User user = getUserById(id);
        user.setRoles(roles);
        return userRepository.save(user);
    }

    public void deleteUser(String id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }

    public User updateUserAvatar(String id, MultipartFile avatarFile) throws IOException {
        User user = getUserById(id);
        
        // Delete old avatar from Supabase if exists
        if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
            try {
                // Extract file path from URL
                String oldAvatarPath = extractFilePathFromUrl(user.getAvatar());
                if (oldAvatarPath != null) {
                    supabaseImageService.deleteImage(oldAvatarPath);
                }
            } catch (Exception e) {
                // Log but don't fail if deletion fails
                System.err.println("Failed to delete old avatar: " + e.getMessage());
            }
        }

        // Upload new avatar
        String fileName = id + "_avatar";
        String folder = "avatars";
        String avatarUrl = supabaseImageService.uploadImage(avatarFile, folder, fileName);
        
        user.setAvatar(avatarUrl);
        return userRepository.save(user);
    }

    private String extractFilePathFromUrl(String url) {
        // Extract path from Supabase URL: https://xxx.supabase.co/storage/v1/object/public/bucket/path
        if (url == null || !url.contains("/storage/v1/object/public/")) {
            return null;
        }
        String[] parts = url.split("/storage/v1/object/public/");
        if (parts.length < 2) {
            return null;
        }
        String[] pathParts = parts[1].split("/", 2);
        if (pathParts.length < 2) {
            return null;
        }
        return pathParts[1]; // Return path after bucket name
    }
}


