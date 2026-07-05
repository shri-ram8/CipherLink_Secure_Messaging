package com.cipherlink.service;

import com.cipherlink.dto.PrivacySettingsDto;
import com.cipherlink.dto.UpdatePrivacySettingsRequest;
import com.cipherlink.model.UserPrivacySettings;
import com.cipherlink.repository.UserPrivacySettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrivacyService {

    private final UserPrivacySettingsRepository privacyRepo;

    private static final String EVERYONE = "EVERYONE";
    private static final String CONTACTS = "CONTACTS";
    private static final String NOBODY   = "NOBODY";

    public PrivacySettingsDto getPrivacySettings(UUID userId) {
        UserPrivacySettings settings = getOrCreate(userId);
        return mapToDto(settings);
    }

    @Transactional
    public PrivacySettingsDto updatePrivacySettings(UUID userId, UpdatePrivacySettingsRequest req) {
        UserPrivacySettings settings = getOrCreate(userId);

        if (isValidVisibility(req.getOnlineStatusVisibility()))
            settings.setOnlineStatusVisibility(req.getOnlineStatusVisibility());
        if (isValidVisibility(req.getLastSeenVisibility()))
            settings.setLastSeenVisibility(req.getLastSeenVisibility());
        if (isValidVisibility(req.getReadReceiptsVisibility()))
            settings.setReadReceiptsVisibility(req.getReadReceiptsVisibility());
        if (isValidVisibility(req.getStoryVisibility()))
            settings.setStoryVisibility(req.getStoryVisibility());
        if (isValidVisibility(req.getProfilePictureVisibility()))
            settings.setProfilePictureVisibility(req.getProfilePictureVisibility());
        if (isValidVisibility(req.getAboutVisibility()))
            settings.setAboutVisibility(req.getAboutVisibility());

        privacyRepo.save(settings);
        return mapToDto(settings);
    }

    private UserPrivacySettings getOrCreate(UUID userId) {
        return privacyRepo.findByUserId(userId)
                .orElseGet(() -> privacyRepo.save(
                        UserPrivacySettings.builder().userId(userId).build()));
    }

    private boolean isValidVisibility(String v) {
        return v != null && (v.equals(EVERYONE) || v.equals(CONTACTS) || v.equals(NOBODY));
    }

    private PrivacySettingsDto mapToDto(UserPrivacySettings s) {
        return PrivacySettingsDto.builder()
                .onlineStatusVisibility(s.getOnlineStatusVisibility())
                .lastSeenVisibility(s.getLastSeenVisibility())
                .readReceiptsVisibility(s.getReadReceiptsVisibility())
                .storyVisibility(s.getStoryVisibility())
                .profilePictureVisibility(s.getProfilePictureVisibility())
                .aboutVisibility(s.getAboutVisibility())
                .build();
    }
}
