package com.cipherlink.dto;

import com.cipherlink.model.GroupRole;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateStoryRequest {
    private String type;
    private String base64Content;
    private String caption;
    private String backgroundColor;
    private String fileExtension;
}
