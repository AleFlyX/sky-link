package com.skylink.land.mapper.chat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.skylink.land.entity.chat.GroupMember;
import com.skylink.land.vo.group.GroupMemberRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface GroupMemberMapper extends BaseMapper<GroupMember> {

    @Select("""
        SELECT group_id, user_id, member_role, join_time
        FROM group_member
        WHERE group_id = #{groupId} AND user_id = #{userId}
        LIMIT 1
        """)
    GroupMember selectMembership(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Select("""
        SELECT COUNT(1)
        FROM group_member
        WHERE group_id = #{groupId}
          AND member_role IN (1, 2, 3)
        """)
    long countActiveMembers(@Param("groupId") Long groupId);

    @Select("""
        SELECT
          gm.user_id,
          u.username,
          u.nickname,
          u.avatar,
          gm.member_role,
          gm.join_time
        FROM group_member gm
        JOIN user u
          ON u.user_id = gm.user_id
         AND u.is_deleted = 0
        WHERE gm.group_id = #{groupId}
          AND gm.member_role IN (1, 2, 3)
        ORDER BY gm.member_role ASC, gm.join_time ASC
        LIMIT #{offset}, #{size}
        """)
    List<GroupMemberRow> selectActiveMembers(
        @Param("groupId") Long groupId,
        @Param("offset") long offset,
        @Param("size") int size
    );

    @Select("""
        SELECT
          gm.user_id,
          u.username,
          u.nickname,
          u.avatar,
          gm.member_role,
          gm.join_time
        FROM group_member gm
        JOIN user u
          ON u.user_id = gm.user_id
         AND u.is_deleted = 0
        WHERE gm.group_id = #{groupId}
          AND gm.member_role IN (1, 2, 3)
        ORDER BY gm.member_role ASC, gm.join_time ASC
        """)
    List<GroupMemberRow> selectAllActiveMembers(@Param("groupId") Long groupId);

    @Update("""
        UPDATE group_member
        SET member_role = 4
        WHERE group_id = #{groupId}
          AND user_id = #{userId}
          AND member_role IN (1, 2, 3)
        """)
    int deactivateMember(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Update("""
        UPDATE group_member
        SET member_role = 4
        WHERE group_id = #{groupId}
          AND member_role IN (1, 2, 3)
        """)
    int deactivateAllMembers(@Param("groupId") Long groupId);
}
