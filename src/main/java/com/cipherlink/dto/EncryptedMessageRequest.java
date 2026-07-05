package com.cipherlink.dto;

import com.cipherlink.model.GroupRole;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EncryptedMessageRequest {
    private UUID conversationId;
    private UUID receiverDeviceId;
    private String ciphertext;
    private String nonce;
    private String ephemeralPublicKey;
}
