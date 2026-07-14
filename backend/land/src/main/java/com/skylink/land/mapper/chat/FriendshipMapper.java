package com.skylink.land.mapper.chat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.skylink.land.entity.chat.Friendship;
import com.skylink.land.vo.friend.FriendListRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FriendshipMapper extends BaseMapper<Friendship> {

    @Select("""
        SELECT user_id, friend_user_id, create_time
        FROM friendship
        WHERE user_id = #{userId} AND friend_user_id = #{friendUserId}
        LIMIT 1
        """)
    Friendship selectByUsers(@Param("userId") Long userId, @Param("friendUserId") Long friendUserId);

    @Select("""
        <script>
        SELECT COUNT(1)
        FROM friendship f
        JOIN user u
          ON u.user_id = CASE
              WHEN f.user_id = #{currentUserId} THEN f.friend_user_id
              ELSE f.user_id
          END
        WHERE (f.user_id = #{currentUserId} OR f.friend_user_id = #{currentUserId})
          AND u.is_deleted = 0
          <if test="nickname != null and nickname != ''">
            AND u.nickname LIKE CONCAT('%', #{nickname}, '%')
          </if>
        </script>
        """)
    long countAcceptedFriends(@Param("currentUserId") Long currentUserId, @Param("nickname") String nickname);

    @Select("""
        <script>
        SELECT
          CASE
            WHEN f.user_id = #{currentUserId} THEN f.friend_user_id
            ELSE f.user_id
          END AS friend_user_id,
          u.username,
          u.nickname,
          u.email,
          u.phone,
          u.status,
          u.department_id,
          d.department_name,
          u.create_time,
          f.create_time AS add_time
        FROM friendship f
        JOIN user u
          ON u.user_id = CASE
              WHEN f.user_id = #{currentUserId} THEN f.friend_user_id
              ELSE f.user_id
          END
        LEFT JOIN department d
          ON d.department_id = u.department_id
         AND d.is_deleted = 0
        WHERE (f.user_id = #{currentUserId} OR f.friend_user_id = #{currentUserId})
          AND u.is_deleted = 0
          <if test="nickname != null and nickname != ''">
            AND u.nickname LIKE CONCAT('%', #{nickname}, '%')
          </if>
        ORDER BY f.create_time DESC
        LIMIT #{offset}, #{size}
        </script>
        """)
    List<FriendListRow> selectAcceptedFriends(
        @Param("currentUserId") Long currentUserId,
        @Param("nickname") String nickname,
        @Param("offset") long offset,
        @Param("size") int size
    );

    @Delete("""
        DELETE FROM friendship
        WHERE user_id = #{userId} AND friend_user_id = #{friendUserId}
        """)
    int deleteByUsers(@Param("userId") Long userId, @Param("friendUserId") Long friendUserId);
}
