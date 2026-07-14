package com.skylink.land.mapper.identity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.skylink.land.entity.identity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("""
        SELECT user_id, username, password, nickname, avatar, email, phone, status, department_id,
               create_time, update_time, is_deleted
        FROM user
        WHERE username = #{username}
        LIMIT 1
        """)
    User selectByUsernameIncludingDeleted(@Param("username") String username);
}
