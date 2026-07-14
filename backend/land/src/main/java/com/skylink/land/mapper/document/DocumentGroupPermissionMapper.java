package com.skylink.land.mapper.document;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.skylink.land.entity.document.DocumentGroupPermission;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DocumentGroupPermissionMapper extends BaseMapper<DocumentGroupPermission> {

    @Select("SELECT * FROM document_group_permission WHERE document_id = #{documentId} ORDER BY create_time")
    List<DocumentGroupPermission> selectByDocumentId(@Param("documentId") Long documentId);

    @Insert("""
        INSERT INTO document_group_permission(document_id, group_id, permission_type)
        VALUES(#{documentId}, #{groupId}, #{permissionType})
        ON DUPLICATE KEY UPDATE permission_type = VALUES(permission_type)
        """)
    int upsert(DocumentGroupPermission permission);

    @Delete("DELETE FROM document_group_permission WHERE document_id = #{documentId} AND group_id = #{groupId}")
    int deleteGrant(@Param("documentId") Long documentId, @Param("groupId") Long groupId);
}
