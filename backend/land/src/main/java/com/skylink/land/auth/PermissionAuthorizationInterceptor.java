package com.skylink.land.auth;

import com.skylink.land.exception.ForbiddenException;
import com.skylink.land.service.identity.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PermissionAuthorizationInterceptor implements HandlerInterceptor {

    private final UserService userService;

    public PermissionAuthorizationInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequirePermission requirePermission = resolveRequiredPermission(handlerMethod);
        if (requirePermission == null || requirePermission.value().length == 0) {
            // 接口未声明功能权限时，本拦截器不拦；是否仍需 JWT 认证取决于认证拦截器配置。
            return true;
        }

        Long userId = AuthContext.requireUserId();
        // 每次请求从数据库汇总最新权限，而不是相信前端传来的角色或权限数组。
        Set<String> userPermissions = userService.listPermissionCodes(userId).stream()
            .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(userPermissions)) {
            throw new ForbiddenException("permission denied");
        }

        boolean allowed = Arrays.stream(requirePermission.value())
            // 一个注解写多个权限时，当前实现要求“全部拥有”才放行。
            .allMatch(userPermissions::contains);
        if (!allowed) {
            throw new ForbiddenException("permission denied");
        }
        return true;
    }

    private RequirePermission resolveRequiredPermission(HandlerMethod handlerMethod) {
        RequirePermission methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(
            handlerMethod.getMethod(),
            RequirePermission.class
        );
        if (methodAnnotation != null) {
            // 方法上的声明优先级更高，允许同一 Controller 的不同接口有不同权限要求。
            return methodAnnotation;
        }
        return AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequirePermission.class);
    }
}
