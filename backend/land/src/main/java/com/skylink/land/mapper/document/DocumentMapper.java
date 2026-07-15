package com.skylink.land.mapper.document;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.skylink.land.entity.document.Document;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DocumentMapper extends BaseMapper<Document> {

    @Select("""
        <script>
        SELECT DISTINCT d.*
        FROM document d
        LEFT JOIN `user` creator ON creator.user_id = d.creator_id
        LEFT JOIN `user` viewer ON viewer.user_id = #{userId}
        LEFT JOIN document_permission dp ON dp.document_id = d.document_id AND dp.user_id = #{userId}
        LEFT JOIN document_group_permission dgp ON dgp.document_id = d.document_id
        LEFT JOIN group_member gm ON gm.group_id = dgp.group_id
          AND gm.user_id = #{userId} AND gm.member_role IN (1, 2, 3)
        WHERE d.is_deleted = 0
          AND (
            #{administrator} = TRUE
            OR d.creator_id = #{userId}
            OR dp.user_id IS NOT NULL
            OR gm.user_id IS NOT NULL
            OR (
              d.status = 2
              AND creator.department_id IS NOT NULL
              AND creator.department_id = viewer.department_id
            )
          )
        <if test="title != null and title != ''">
          AND d.title LIKE CONCAT('%', #{title}, '%')
        </if>
        ORDER BY d.update_time DESC
        </script>
        """)
    Page<Document> selectAccessiblePage(
        Page<Document> page,
        @Param("userId") Long userId,
        @Param("title") String title,
        @Param("administrator") boolean administrator
    );
}
