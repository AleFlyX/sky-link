package com.skylink.land.mapper.chat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.skylink.land.entity.chat.Message;
import com.skylink.land.vo.message.MessageHistoryRow;
import com.skylink.land.vo.message.MessageSessionRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    @Select("""
        <script>
        SELECT
          'single' AS session_type,
          conversation.target_id,
          COALESCE(NULLIF(target_user.nickname, ''), target_user.username, CONCAT('用户#', conversation.target_id)) AS target_name,
          latest.message_id AS last_message_id,
          latest.sender_id AS last_message_sender_id,
          COALESCE(NULLIF(sender.nickname, ''), sender.username, CONCAT('用户#', latest.sender_id)) AS last_message_sender_name,
          latest.receiver_id,
          latest.group_id,
          latest.message_type AS last_message_type,
          latest.content AS last_message_content,
          latest.is_recalled AS last_message_recalled,
          latest.send_time AS last_time
        FROM (
          SELECT
            CASE WHEN m.sender_id = #{currentUserId} THEN m.receiver_id ELSE m.sender_id END AS target_id,
            MAX(m.message_id) AS last_message_id
          FROM message m
          WHERE m.group_id IS NULL
            AND m.receiver_id IS NOT NULL
            AND (m.sender_id = #{currentUserId} OR m.receiver_id = #{currentUserId})
          GROUP BY CASE WHEN m.sender_id = #{currentUserId} THEN m.receiver_id ELSE m.sender_id END
        ) conversation
        JOIN message latest
          ON latest.message_id = conversation.last_message_id
        LEFT JOIN user target_user
          ON target_user.user_id = conversation.target_id
        LEFT JOIN user sender
          ON sender.user_id = latest.sender_id
        ORDER BY latest.message_id DESC
        </script>
        """)
    List<MessageSessionRow> selectSingleSessions(@Param("currentUserId") Long currentUserId);

    @Select("""
        <script>
        SELECT
          'group' AS session_type,
          conversation.group_id AS target_id,
          COALESCE(NULLIF(g.group_name, ''), CONCAT('群聊#', conversation.group_id)) AS target_name,
          latest.message_id AS last_message_id,
          latest.sender_id AS last_message_sender_id,
          COALESCE(NULLIF(sender.nickname, ''), sender.username, CONCAT('用户#', latest.sender_id)) AS last_message_sender_name,
          latest.receiver_id,
          latest.group_id,
          latest.message_type AS last_message_type,
          latest.content AS last_message_content,
          latest.is_recalled AS last_message_recalled,
          latest.send_time AS last_time
        FROM (
          SELECT
            m.group_id,
            MAX(m.message_id) AS last_message_id
          FROM message m
          JOIN group_member gm
            ON gm.group_id = m.group_id
           AND gm.user_id = #{currentUserId}
           AND gm.member_role IN (1, 2, 3)
          WHERE m.group_id IS NOT NULL
          GROUP BY m.group_id
        ) conversation
        JOIN message latest
          ON latest.message_id = conversation.last_message_id
        JOIN chat_group g
          ON g.group_id = conversation.group_id
         AND g.is_deleted = 0
         AND g.status = 1
        LEFT JOIN user sender
          ON sender.user_id = latest.sender_id
        ORDER BY latest.message_id DESC
        </script>
        """)
    List<MessageSessionRow> selectGroupSessions(@Param("currentUserId") Long currentUserId);

    @Select("""
        <script>
        SELECT COUNT(1)
        FROM message m
        WHERE m.group_id IS NULL
          AND m.receiver_id IS NOT NULL
          AND (
            (m.sender_id = #{currentUserId} AND m.receiver_id = #{receiverId})
            OR (m.sender_id = #{receiverId} AND m.receiver_id = #{currentUserId})
          )
          <if test="before != null">
            AND m.message_id &lt; #{before}
          </if>
        </script>
        """)
    long countSingleMessages(
        @Param("currentUserId") Long currentUserId,
        @Param("receiverId") Long receiverId,
        @Param("before") Long before
    );

    @Select("""
        <script>
        SELECT COUNT(1)
        FROM message m
        JOIN group_member gm
          ON gm.group_id = m.group_id
         AND gm.user_id = #{currentUserId}
         AND gm.member_role IN (1, 2, 3)
        WHERE m.group_id = #{groupId}
          <if test="before != null">
            AND m.message_id &lt; #{before}
          </if>
        </script>
        """)
    long countGroupMessages(
        @Param("currentUserId") Long currentUserId,
        @Param("groupId") Long groupId,
        @Param("before") Long before
    );

    @Select("""
        <script>
        SELECT
          m.message_id,
          m.sender_id,
          COALESCE(NULLIF(sender.nickname, ''), sender.username, CONCAT('用户#', m.sender_id)) AS sender_name,
          m.receiver_id,
          m.group_id,
          m.message_type,
          m.content,
          m.is_recalled,
          m.send_time
        FROM message m
        LEFT JOIN user sender
          ON sender.user_id = m.sender_id
        WHERE m.group_id IS NULL
          AND m.receiver_id IS NOT NULL
          AND (
            (m.sender_id = #{currentUserId} AND m.receiver_id = #{receiverId})
            OR (m.sender_id = #{receiverId} AND m.receiver_id = #{currentUserId})
          )
          <if test="before != null">
            AND m.message_id &lt; #{before}
          </if>
        ORDER BY m.message_id DESC
        LIMIT #{offset}, #{size}
        </script>
        """)
    List<MessageHistoryRow> selectSingleMessages(
        @Param("currentUserId") Long currentUserId,
        @Param("receiverId") Long receiverId,
        @Param("before") Long before,
        @Param("offset") long offset,
        @Param("size") int size
    );

    @Select("""
        <script>
        SELECT
          m.message_id,
          m.sender_id,
          COALESCE(NULLIF(sender.nickname, ''), sender.username, CONCAT('用户#', m.sender_id)) AS sender_name,
          m.receiver_id,
          m.group_id,
          m.message_type,
          m.content,
          m.is_recalled,
          m.send_time
        FROM message m
        JOIN group_member gm
          ON gm.group_id = m.group_id
         AND gm.user_id = #{currentUserId}
         AND gm.member_role IN (1, 2, 3)
        LEFT JOIN user sender
          ON sender.user_id = m.sender_id
        WHERE m.group_id = #{groupId}
          <if test="before != null">
            AND m.message_id &lt; #{before}
          </if>
        ORDER BY m.message_id DESC
        LIMIT #{offset}, #{size}
        </script>
        """)
    List<MessageHistoryRow> selectGroupMessages(
        @Param("currentUserId") Long currentUserId,
        @Param("groupId") Long groupId,
        @Param("before") Long before,
        @Param("offset") long offset,
        @Param("size") int size
    );
}
