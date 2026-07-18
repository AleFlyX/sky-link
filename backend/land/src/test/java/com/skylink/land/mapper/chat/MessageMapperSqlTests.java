package com.skylink.land.mapper.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

class MessageMapperSqlTests {

    @Test
    void singleSessionsAreLimitedToCurrentFriendships() throws Exception {
        Method method = MessageMapper.class.getMethod("selectSingleSessions", Long.class);
        String sql = String.join("\n", method.getAnnotation(Select.class).value());

        assertThat(sql)
            .contains("JOIN friendship f")
            .contains("f.user_id = LEAST(#{currentUserId}, conversation.target_id)")
            .contains("f.friend_user_id = GREATEST(#{currentUserId}, conversation.target_id)");
    }
}
