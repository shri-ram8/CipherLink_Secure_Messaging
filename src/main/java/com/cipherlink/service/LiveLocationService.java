package com.cipherlink.service;

import com.cipherlink.dto.LiveLocationResponse;
import com.cipherlink.dto.StartLiveLocationRequest;
import com.cipherlink.dto.UpdateLiveLocationRequest;
import com.cipherlink.model.LiveLocation;
import com.cipherlink.model.UserProfile;
import com.cipherlink.repository.LiveLocationRepository;
import com.cipherlink.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LiveLocationService {

    private final LiveLocationRepository locationRepo;
    private final UserProfileRepository profileRepo;

    @Transactional
    public LiveLocationResponse startSharing(UUID userId, StartLiveLocationRequest req) {
        // Stop any existing active share in same conversation
        if (req.getConversationId() != null) {
            locationRepo.findFirstByUserIdAndConversationIdAndIsActiveTrueOrderBySharedAtDesc(
                    userId, req.getConversationId())
                    .ifPresent(l -> { l.setActive(false); locationRepo.save(l); });
        }

        int duration = req.getDurationMinutes() != null ? req.getDurationMinutes() : 60;

        LiveLocation loc = LiveLocation.builder()
                .userId(userId)
                .conversationId(req.getConversationId())
                .groupId(req.getGroupId())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .label(req.getLabel())
                .expiresAt(LocalDateTime.now().plusMinutes(duration))
                .build();
        locationRepo.save(loc);
        return buildResponse(loc);
    }

    @Transactional
    public LiveLocationResponse updateLocation(UUID userId, UUID locationId, UpdateLiveLocationRequest req) {
        LiveLocation loc = locationRepo.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Location share not found"));
        if (!loc.getUserId().equals(userId)) throw new RuntimeException("Unauthorized");
        if (!loc.isActive()) throw new RuntimeException("Location share expired");

        loc.setLatitude(req.getLatitude());
        loc.setLongitude(req.getLongitude());
        locationRepo.save(loc);
        return buildResponse(loc);
    }

    @Transactional
    public void stopSharing(UUID userId, UUID locationId) {
        LiveLocation loc = locationRepo.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Location share not found"));
        if (!loc.getUserId().equals(userId)) throw new RuntimeException("Unauthorized");
        loc.setActive(false);
        locationRepo.save(loc);
    }

    public List<LiveLocationResponse> getActiveLocations(UUID conversationId) {
        return locationRepo.findByConversationIdAndIsActiveTrueAndExpiresAtAfter(
                conversationId, LocalDateTime.now())
                .stream().map(this::buildResponse).collect(Collectors.toList());
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireLocations() {
        locationRepo.findExpired(LocalDateTime.now()).forEach(l -> {
            l.setActive(false);
            locationRepo.save(l);
        });
    }

    private LiveLocationResponse buildResponse(LiveLocation loc) {
        UserProfile profile = profileRepo.findByUserId(loc.getUserId()).orElse(null);
        return LiveLocationResponse.builder()
                .id(loc.getId())
                .userId(loc.getUserId())
                .userName(profile != null ? profile.getDisplayName() : "")
                .userAvatarUrl(profile != null ? profile.getProfilePictureUrl() : null)
                .latitude(loc.getLatitude())
                .longitude(loc.getLongitude())
                .label(loc.getLabel())
                .sharedAt(loc.getSharedAt())
                .expiresAt(loc.getExpiresAt())
                .isActive(loc.isActive())
                .build();
    }
}
