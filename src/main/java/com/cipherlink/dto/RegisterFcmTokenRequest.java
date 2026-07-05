package com.cipherlink.dto;

import com.cipherlink.model.GroupRole;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterFcmTokenRequest {
    private UUID deviceId;
    private String fcmToken;
}
