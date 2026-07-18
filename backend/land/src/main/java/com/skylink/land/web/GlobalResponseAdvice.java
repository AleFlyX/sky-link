package com.skylink.land.web;

import com.skylink.land.dto.common.ApiResponse;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import tools.jackson.databind.ObjectMapper;

@RestControllerAdvice
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    public GlobalResponseAdvice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 所有 Controller 响应都进入统一处理；是否跳过包装在 beforeBodyWrite 中再判断。
        return true;
    }

    @Override
    public Object beforeBodyWrite(
        Object body,
        MethodParameter returnType,
        MediaType selectedContentType,
        Class<? extends HttpMessageConverter<?>> selectedConverterType,
        ServerHttpRequest request,
        ServerHttpResponse response
    ) {
        if (body instanceof ApiResponse<?> || body instanceof Resource || body instanceof byte[]) {
            // 已是统一响应不能再包一层；文件/字节流也必须保持原样，否则下载会变成 JSON。
            return body;
        }

        // 普通 Java 对象在这里被统一变成 { code, message, data }，Controller 无须反复手写成功包装。
        ApiResponse<Object> apiResponse = ApiResponse.success(body);
        if (StringHttpMessageConverter.class.isAssignableFrom(selectedConverterType)) {
            // String 使用专门的消息转换器，需先手动转为 JSON 字符串，避免转换器类型冲突。
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            try {
                return objectMapper.writeValueAsString(apiResponse);
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to serialize api response", exception);
            }
        }
        return apiResponse;
    }
}
