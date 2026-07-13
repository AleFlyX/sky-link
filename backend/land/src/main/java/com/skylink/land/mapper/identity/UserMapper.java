package com.skylink.land.mapper.identity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.skylink.land.entity.identity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
