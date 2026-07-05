package com.cipherlink.helper;

import com.cipherlink.dto.PagedResult;
import java.util.List;

public class PaginationHelper {

    public static <T> PagedResult<T> create(List<T> items, int totalCount, int page, int pageSize) {
        return PagedResult.<T>builder()
                .items(items)
                .totalCount(totalCount)
                .page(page)
                .pageSize(pageSize)
                .build();
    }
}
