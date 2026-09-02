package com.sast.crs.response;

import com.sast.crs.enums.ErrorEnum;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import jakarta.annotation.Resource;

@RestControllerAdvice
public class GlobalResponseHandler implements ResponseBodyAdvice<Object> {
    @Resource
    private JsonMapper jsonMapper;

    @Override
    public boolean supports(@NotNull MethodParameter returnType, @NotNull Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, @NotNull MethodParameter returnType, @NotNull MediaType selectedContentType, @NotNull Class<? extends HttpMessageConverter<?>> selectedConverterType, @NotNull ServerHttpRequest request, @NotNull ServerHttpResponse response) {
        if (body == null) {
            return GlobalResponse.success();
        } else if (body instanceof GlobalResponse) {
            return body;
        } else if (body instanceof String) {
            try {
                response.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                return jsonMapper.writeValueAsString(GlobalResponse.success(body));
            } catch (JacksonException e) {
                e.printStackTrace();
            }
        } else if (body instanceof ErrorEnum) {
            return GlobalResponse.failure((ErrorEnum) body);
        }
        return GlobalResponse.success(body);
    }
}
