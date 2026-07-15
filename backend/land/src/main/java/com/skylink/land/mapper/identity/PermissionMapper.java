package com.skylink.land.mapper.identity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.skylink.land.entity.identity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    @Select("""
        SELECT permission_id, permission_name, permission_code, permission_type, sort_no,
               create_time, update_time, is_deleted
        FROM permission
        WHERE permission_code = #{permissionCode}
        LIMIT 1
        """)
    Permission selectByPermissionCodeIncludingDeleted(@Param("permissionCode") String permissionCode);

    @Update("""
        UPDATE permission
        SET is_deleted = 0, update_time = NOW()
        WHERE permission_id = #{permissionId} AND is_deleted = 1
        """)
    int restoreSystemPermission(@Param("permissionId") Long permissionId);
}
