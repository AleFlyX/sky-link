package com.skylink.land.websocket;

public final class WebSocketSessionKeys {

    // 握手拦截器写入、Handler/注册表读取的用户 ID 键；集中声明可避免字符串拼写不一致。
    public static final String USER_ID = "websocket.userId";

    // 当前消息推送暂未直接使用用户名，但保留其可信握手身份，供将来审计或欢迎消息使用。
    public static final String USERNAME = "websocket.username";

    private WebSocketSessionKeys() {
        // 工具常量类不应被实例化。
    }
}
