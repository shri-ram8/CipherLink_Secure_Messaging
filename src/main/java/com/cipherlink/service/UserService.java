package com.cipherlink.service;

import com.cipherlink.dto.*;
import com.cipherlink.model.*;
import com.cipherlink.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepo;
    private final UserProfileRepository profileRepo;
    private final ContactRepository contactRepo;

    public UserProfileResponse getProfile(UUID userId) {
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        UserProfile profile = profileRepo.findByUserId(userId).orElse(null);

        return UserProfileResponse.builder()
                .userId(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .displayName(profile != null ? profile.getDisplayName() : "")
                .aboutText(profile != null ? profile.getAboutText() : null)
                .profilePictureUrl(profile != null ? profile.getProfilePictureUrl() : null)
                .build();
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        UserProfile profile = profileRepo.findByUserId(userId)
                .orElseGet(() -> UserProfile.builder().userId(userId).build());

        profile.setDisplayName(request.getDisplayName());
        profile.setAboutText(request.getAboutText());
        if (request.getProfilePictureUrl() != null) {
            profile.setProfilePictureUrl(request.getProfilePictureUrl());
        }
        profile.setUpdatedAt(LocalDateTime.now());
        profileRepo.save(profile);

        return UserProfileResponse.builder()
                .userId(userId)
                .phoneNumber(user.getPhoneNumber())
                .displayName(profile.getDisplayName())
                .aboutText(profile.getAboutText())
                .profilePictureUrl(profile.getProfilePictureUrl())
                .build();
    }

    @Transactional
    public void updateProfilePicture(UUID userId, String url) {
        UserProfile profile = profileRepo.findByUserId(userId)
                .orElseGet(() -> UserProfile.builder().userId(userId).build());
        profile.setProfilePictureUrl(url);
        profile.setUpdatedAt(LocalDateTime.now());
        profileRepo.save(profile);
    }

    @Transactional
    public ContactResponse addContact(UUID ownerId, AddContactRequest request) {
        User contactUser = userRepo.findByPhoneNumber(com.cipherlink.helper.PhoneNumberUtil.normalize(request.getPhoneNumber()))
                .orElseThrow(() -> new RuntimeException("User not found on CipherLink"));

        if (contactRepo.existsByOwnerIdAndContactUserId(ownerId, contactUser.getId())) {
            throw new RuntimeException("Contact already added");
        }

        Contact contact = Contact.builder()
                .ownerId(ownerId)
                .contactUserId(contactUser.getId())
                .nickname(request.getNickname())
                .build();
        contactRepo.save(contact);

        UserProfile profile = profileRepo.findByUserId(contactUser.getId()).orElse(null);

        return ContactResponse.builder()
                .contactUserId(contactUser.getId())
                .phoneNumber(contactUser.getPhoneNumber())
                .displayName(profile != null ? profile.getDisplayName() : "")
                .nickname(contact.getNickname())
                .isBlocked(false)
                .isOnCipherLink(true)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ContactResponse> getContacts(UUID ownerId) {
        List<Contact> contacts = contactRepo.findByOwnerIdAndIsBlockedFalse(ownerId);
        return contacts.stream().map(contact -> {
            UserProfile profile = profileRepo.findByUserId(contact.getContactUserId()).orElse(null);
            User contactUser = userRepo.findById(contact.getContactUserId()).orElse(null);
            return ContactResponse.builder()
                    .contactUserId(contact.getContactUserId())
                    .phoneNumber(contactUser != null ? contactUser.getPhoneNumber() : "")
                    .displayName(profile != null ? profile.getDisplayName() : "")
                    .nickname(contact.getNickname())
                    .isBlocked(contact.isBlocked())
                    .isOnCipherLink(true)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public void blockContact(UUID ownerId, UUID contactUserId) {
        Contact contact = contactRepo.findByOwnerIdAndContactUserId(ownerId, contactUserId)
                .orElseThrow(() -> new RuntimeException("Contact not found"));
        contact.setBlocked(true);
        contactRepo.save(contact);
    }
}