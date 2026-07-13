package com.skylink.land.dto.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long total;

    private Integer page;

    private Integer size;

    private List<T> records;

    public static <T> PageResponse<T> of(IPage<T> page) {
        return PageResponse.<T>builder()
            .total(page.getTotal())
            .page(Math.toIntExact(page.getCurrent()))
            .size(Math.toIntExact(page.getSize()))
            .records(page.getRecords())
            .build();
    }

    public static <T> PageResponse<T> empty(PageRequest request) {
        return PageResponse.<T>builder()
            .total(0L)
            .page(request == null ? PageRequest.DEFAULT_PAGE : request.pageOrDefault())
            .size(request == null ? PageRequest.DEFAULT_SIZE : request.sizeOrDefault())
            .records(Collections.emptyList())
            .build();
    }
}
