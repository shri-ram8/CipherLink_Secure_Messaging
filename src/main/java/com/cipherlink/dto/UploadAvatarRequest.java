package com.cipherlink.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class UploadAvatarRequest {
    private String base64ImageData;
    private String fileName;
    private String mimeType;
}
