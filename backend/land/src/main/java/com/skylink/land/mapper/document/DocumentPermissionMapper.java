package com.skylink.land.mapper.document;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.skylink.land.entity.document.DocumentPermission;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DocumentPermissionMapper extends BaseMapper<DocumentPermission> {

    @Select("""
        SELECT MAX(permission_type) FROM (
          SELECT permission_type FROM document_permission
          WHERE document_id = #{documentId} AND user_id = #{userId}
          UNION ALL
          SELECT dgp.permission_type
          FROM document_group_permission dgp
          JOIN group_member gm ON gm.group_id = dgp.group_id
          WHERE dgp.document_id = #{documentId} AND gm.user_id = #{userId}
            AND gm.member_role IN (1, 2, 3)
        ) effective_permissions
        """)
    Integer selectEffectivePermission(@Param("documentId") Long documentId, @Param("userId") Long userId);

    @Select("SELECT * FROM document_permission WHERE document_id = #{documentId} ORDER BY create_time")
    List<DocumentPermission> selectByDocumentId(@Param("documentId") Long documentId);

    @Insert("""
        INSERT INTO document_permission(document_id, user_id, permission_type)
        VALUES(#{documentId}, #{userId}, #{permissionType})
        ON DUPLICATE KEY UPDATE permission_type = VALUES(permission_type)
        """)
    int upsert(DocumentPermission permission);

    @Delete("DELETE FROM document_permission WHERE document_id = #{documentId} AND user_id = #{userId}")
    int deleteGrant(@Param("documentId") Long documentId, @Param("userId") Long userId);
}
