package com.cipherlink.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "user_privacy_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPrivacySettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID userId;

    // "EVERYONE" | "CONTACTS" | "NOBODY"
    @Builder.Default
    private String onlineStatusVisibility = "CONTACTS";

    @Builder.Default
    private String lastSeenVisibility = "CONTACTS";

    @Builder.Default
    private String readReceiptsVisibility = "CONTACTS";

    @Builder.Default
    private String storyVisibility = "CONTACTS";

    @Builder.Default
    private String profilePictureVisibility = "CONTACTS";

    @Builder.Default
    private String aboutVisibility = "CONTACTS";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", insertable = false, updatable = false)
    private User user;
}
