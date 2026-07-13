package com.skylink.land.auth;

import java.util.Optional;

public final class AuthContext {

    private static final ThreadLocal<AuthenticatedUser> CURRENT_USER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void setCurrentUser(AuthenticatedUser user) {
        CURRENT_USER.set(user);
    }

    public static Optional<AuthenticatedUser> getCurrentUser() {
        return Optional.ofNullable(CURRENT_USER.get());
    }

    public static Long requireUserId() {
        return getCurrentUser()
            .map(AuthenticatedUser::getUserId)
            .orElseThrow(() -> new IllegalStateException("Current user is missing"));
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
