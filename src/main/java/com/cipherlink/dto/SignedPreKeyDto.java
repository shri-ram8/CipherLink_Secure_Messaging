package com.cipherlink.dto;

import com.cipherlink.model.GroupRole;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignedPreKeyDto {
    private int signedPreKeyId;
    private String publicKey;
    private String signature;
}
