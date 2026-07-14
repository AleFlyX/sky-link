package com.skylink.land.mapper.identity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.skylink.land.entity.identity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    @Select("""
        SELECT role_id, role_name, role_code, description, status, create_time, update_time, is_deleted
        FROM role
        WHERE role_code = #{roleCode}
        LIMIT 1
        """)
    Role selectByRoleCodeIncludingDeleted(@Param("roleCode") String roleCode);

    @Update("""
        UPDATE role
        SET is_deleted = 0, status = 1, update_time = NOW()
        WHERE role_id = #{roleId} AND is_deleted = 1
        """)
    int restoreSystemRole(@Param("roleId") Long roleId);
}
