package com.skylink.land.mapper.task;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.skylink.land.entity.task.Task;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}
