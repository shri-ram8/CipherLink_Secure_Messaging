package com.cipherlink.dto;

import com.cipherlink.model.GroupRole;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpRequest {
    private String firebaseIdToken;
    private String otpCode;
    private String deviceName;
    private String identityPublicKey;
    private String devicePublicKey;
}
