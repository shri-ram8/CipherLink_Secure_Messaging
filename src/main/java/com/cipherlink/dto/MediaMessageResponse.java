package com.cipherlink.dto;

import com.cipherlink.model.GroupRole;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaMessageResponse {
    private UUID messageId;
    private String fileUrl;
    private String storagePath;
    private String fileName;
    private String mediaType;
    private long fileSizeBytes;
    private String thumbnailUrl;
    private LocalDateTime uploadedAt;
}
