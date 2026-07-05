package com.cipherlink.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LinkPreviewDto {
    private String url;
    private String title;
    private String description;
    private String imageUrl;
    private String siteName;
    private String faviconUrl;
    // "youtube" | "instagram" | "amazon" | "twitter" | "generic"
    private String type;
    // YouTube: videoId, duration etc.
    private String videoId;
}
