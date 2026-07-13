package com.skylink.land.dto.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_PAGE = 1;

    public static final int DEFAULT_SIZE = 20;

    public static final int MAX_SIZE = 100;

    private Integer page;

    private Integer size;

    public int pageOrDefault() {
        if (page == null || page < 1) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    public int sizeOrDefault() {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    public <T> Page<T> toMybatisPage() {
        return Page.of(pageOrDefault(), sizeOrDefault());
    }
}
