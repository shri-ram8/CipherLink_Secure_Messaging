package com.cipherlink.dto;

import com.cipherlink.model.GroupRole;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagedResult<T> {
    private List<T> items;
    private int totalCount;
    private int page;
    private int pageSize;

    public int getTotalPages() {
        return (int) Math.ceil((double) totalCount / pageSize);
    }

    public boolean isHasNextPage() {
        return page < getTotalPages();
    }

    public boolean isHasPrevPage() {
        return page > 1;
    }
}
