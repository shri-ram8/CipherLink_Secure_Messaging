package com.cipherlink.dto;

import com.cipherlink.model.GroupRole;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreKeyCountResponse {
    private UUID deviceId;
    private int remainingPreKeys;
    private boolean needsRefill;
}
