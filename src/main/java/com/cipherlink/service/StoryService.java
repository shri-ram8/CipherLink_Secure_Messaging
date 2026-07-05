package com.cipherlink.service;

import com.cipherlink.dto.*;
import com.cipherlink.model.*;
import com.cipherlink.repository.*;
import com.cipherlink.security.JwtService;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoryService {

    private final StoryRepository storyRepo;
    private final StoryViewRepository storyViewRepo;
    private final ContactRepository contactRepo;
    private final UserRepository userRepo;
    private final UserProfileRepository profileRepo;

    @Value("${media.stories-path:./media/stories}")
    private String storiesPath;

    @Transactional
    public StoryResponse createStory(UUID userId, CreateStoryRequest request) throws IOException {
        String contentUrl;
        if ("Text".equalsIgnoreCase(request.getType())) {
            contentUrl = request.getBase64Content();
        } else {
            byte[] fileBytes = Base64.getDecoder().decode(request.getBase64Content());
            String ext = request.getFileExtension() != null ? request.getFileExtension() : ".jpg";
            String fileName = UUID.randomUUID() + ext;
            Path filePath = Paths.get(storiesPath, fileName);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, fileBytes);
            contentUrl = "/media/stories/" + fileName;
        }

        StoryType storyType = StoryType.valueOf(request.getType().toUpperCase());
        Story story = Story.builder()
                .userId(userId)
                .type(storyType)
                .contentUrl(contentUrl)
                .caption(request.getCaption())
                .backgroundColor(request.getBackgroundColor())
                .build();
        storyRepo.save(story);

        return buildStoryResponse(story, userId);
    }

    @Transactional(readOnly = true)
    public List<StoryFeedResponse> getStoryFeed(UUID userId) {
        List<UUID> contactIds = contactRepo.findContactIdsByOwnerId(userId);

        List<Story> stories = storyRepo.findActiveStoriesByUsers(contactIds, LocalDateTime.now());

        Map<UUID, List<Story>> grouped = stories.stream()
                .collect(Collectors.groupingBy(Story::getUserId));

        List<StoryFeedResponse> result = new ArrayList<>();
        for (Map.Entry<UUID, List<Story>> entry : grouped.entrySet()) {
            UUID storyUserId = entry.getKey();
            UserProfile profile = profileRepo.findByUserId(storyUserId).orElse(null);
            User user = userRepo.findById(storyUserId).orElse(null);

            List<StoryResponse> storyResponses = entry.getValue().stream()
                    .map(s -> buildStoryResponse(s, userId))
                    .collect(Collectors.toList());

            result.add(StoryFeedResponse.builder()
                    .userId(storyUserId)
                    .userName(profile != null ? profile.getDisplayName()
                            : (user != null ? user.getPhoneNumber() : ""))
                    .userProfilePic(profile != null ? profile.getProfilePictureUrl() : null)
                    .stories(storyResponses)
                    .hasUnviewed(storyResponses.stream().anyMatch(s -> !s.isHasViewed()))
                    .build());
        }

        result.sort((a, b) -> {
            if (a.isHasUnviewed() != b.isHasUnviewed())
                return a.isHasUnviewed() ? -1 : 1;
            LocalDateTime aMax = a.getStories().stream()
                    .map(StoryResponse::getCreatedAt).max(Comparator.naturalOrder()).orElse(LocalDateTime.MIN);
            LocalDateTime bMax = b.getStories().stream()
                    .map(StoryResponse::getCreatedAt).max(Comparator.naturalOrder()).orElse(LocalDateTime.MIN);
            return bMax.compareTo(aMax);
        });

        return result;
    }

    @Transactional(readOnly = true)
    public List<StoryResponse> getMyStories(UUID userId) {
        return storyRepo.findByUserIdAndIsActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(userId, LocalDateTime.now())
                .stream().map(s -> buildStoryResponse(s, userId)).collect(Collectors.toList());
    }

    @Transactional
    public void viewStory(UUID viewerUserId, UUID storyId) {
        Story story = storyRepo.findById(storyId).orElseThrow(() -> new RuntimeException("Story not found"));
        if (story.getUserId().equals(viewerUserId)) return;
        if (storyViewRepo.existsByStoryIdAndViewerUserId(storyId, viewerUserId)) return;

        storyViewRepo.save(StoryView.builder().storyId(storyId).viewerUserId(viewerUserId).build());
    }

    @Transactional
    public void deleteStory(UUID userId, UUID storyId) {
        Story story = storyRepo.findById(storyId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Story not found"));
        story.setActive(false);
        storyRepo.save(story);
    }

    public List<StoryView> getStoryViewers(UUID userId, UUID storyId) {
        storyRepo.findById(storyId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Story not found or not yours"));

        return storyViewRepo.findByStoryIdOrderByViewedAtDesc(storyId);
    }

    private StoryResponse buildStoryResponse(Story story, UUID viewerUserId) {
        UserProfile profile = profileRepo.findByUserId(story.getUserId()).orElse(null);
        User user = userRepo.findById(story.getUserId()).orElse(null);

        boolean hasViewed = story.getViews().stream()
                .anyMatch(v -> v.getViewerUserId().equals(viewerUserId));

        return StoryResponse.builder()
                .id(story.getId())
                .userId(story.getUserId())
                .userName(profile != null ? profile.getDisplayName()
                        : (user != null ? user.getPhoneNumber() : ""))
                .userProfilePic(profile != null ? profile.getProfilePictureUrl() : null)
                .type(story.getType().name())
                .contentUrl(story.getContentUrl())
                .caption(story.getCaption())
                .backgroundColor(story.getBackgroundColor())
                .createdAt(story.getCreatedAt())
                .expiresAt(story.getExpiresAt())
                .viewCount(story.getViews().size())
                .hasViewed(hasViewed)
                .build();
    }
}
