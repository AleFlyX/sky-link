package com.skylink.land.mapper.chat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.skylink.land.entity.chat.FriendRequest;
import com.skylink.land.vo.friend.FriendRequestRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FriendRequestMapper extends BaseMapper<FriendRequest> {

    @Select("""
        SELECT request_id, requester_id, receiver_id, message, status, create_time, update_time
        FROM friend_request
        WHERE status = 0
          AND ((requester_id = #{firstUserId} AND receiver_id = #{secondUserId})
            OR (requester_id = #{secondUserId} AND receiver_id = #{firstUserId}))
        ORDER BY request_id DESC
        LIMIT 1
        """)
    FriendRequest selectPendingBetween(
        @Param("firstUserId") Long firstUserId,
        @Param("secondUserId") Long secondUserId
    );

    @Select("""
        SELECT COUNT(1)
        FROM friend_request fr
        JOIN user u
          ON u.user_id = fr.requester_id
         AND u.is_deleted = 0
        WHERE fr.receiver_id = #{currentUserId}
          AND fr.status = 0
        """)
    long countIncomingRequests(@Param("currentUserId") Long currentUserId);

    @Select("""
        SELECT
          fr.request_id,
          fr.requester_id AS request_user_id,
          fr.message,
          fr.status AS request_status,
          u.username,
          u.nickname,
          u.avatar,
          u.email,
          u.phone,
          u.status,
          u.department_id,
          d.department_name,
          u.create_time,
          fr.create_time AS request_time
        FROM friend_request fr
        JOIN user u
          ON u.user_id = fr.requester_id
         AND u.is_deleted = 0
        LEFT JOIN department d
          ON d.department_id = u.department_id
         AND d.is_deleted = 0
        WHERE fr.receiver_id = #{currentUserId}
          AND fr.status = 0
        ORDER BY fr.create_time DESC, fr.request_id DESC
        LIMIT #{offset}, #{size}
        """)
    List<FriendRequestRow> selectIncomingRequests(
        @Param("currentUserId") Long currentUserId,
        @Param("offset") long offset,
        @Param("size") int size
    );
}
