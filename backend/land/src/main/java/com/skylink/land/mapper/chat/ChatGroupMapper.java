package com.skylink.land.mapper.chat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.skylink.land.entity.chat.ChatGroup;
import com.skylink.land.vo.group.GroupSummaryRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ChatGroupMapper extends BaseMapper<ChatGroup> {

    @Select("""
        SELECT COUNT(1)
        FROM chat_group g
        JOIN group_member gm
          ON gm.group_id = g.group_id
        WHERE gm.user_id = #{currentUserId}
          AND gm.member_role IN (1, 2, 3)
          AND g.is_deleted = 0
          AND g.status = 1
        """)
    long countJoinedGroups(@Param("currentUserId") Long currentUserId);

    @Select("""
        SELECT
          g.group_id,
          g.group_name,
          g.avatar,
          g.notice,
          g.owner_id,
          COALESCE(NULLIF(owner.nickname, ''), owner.username) AS owner_name,
          COUNT(active_member.user_id) AS member_count,
          g.create_time
        FROM chat_group g
        JOIN group_member gm
          ON gm.group_id = g.group_id
        LEFT JOIN user owner
          ON owner.user_id = g.owner_id
         AND owner.is_deleted = 0
        LEFT JOIN group_member active_member
          ON active_member.group_id = g.group_id
         AND active_member.member_role IN (1, 2, 3)
        WHERE gm.user_id = #{currentUserId}
          AND gm.member_role IN (1, 2, 3)
          AND g.is_deleted = 0
          AND g.status = 1
        GROUP BY g.group_id, g.group_name, g.avatar, g.notice, g.owner_id, owner_name, g.create_time
        ORDER BY g.update_time DESC
        LIMIT #{offset}, #{size}
        """)
    List<GroupSummaryRow> selectJoinedGroups(
        @Param("currentUserId") Long currentUserId,
        @Param("offset") long offset,
        @Param("size") int size
    );
}
