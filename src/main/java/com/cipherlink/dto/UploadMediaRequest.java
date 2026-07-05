package com.cipherlink.dto;

import com.cipherlink.model.GroupRole;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadMediaRequest {
    private UUID conversationId;
    private UUID receiverDeviceId;
    private String fileName;
    private String base64FileData;
    private String mediaType;
    private String mimeType;
    private Integer voiceDurationSeconds;
    private String encryptedKey;
    private String nonce;
}
