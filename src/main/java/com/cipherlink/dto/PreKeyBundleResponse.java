package com.cipherlink.dto;

import com.cipherlink.model.GroupRole;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreKeyBundleResponse {
    private UUID deviceId;
    private String identityPublicKey;
    private PreKeyDto preKey;
    private SignedPreKeyDto signedPreKey;
}
