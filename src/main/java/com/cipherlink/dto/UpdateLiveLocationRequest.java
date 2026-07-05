package com.cipherlink.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor
public class UpdateLiveLocationRequest {
    private Double latitude;
    private Double longitude;
}

