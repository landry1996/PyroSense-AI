package com.pyrosense.maintenance.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyrosense.shared.security.AllowedFields;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.util.Iterator;
import java.util.Set;

@Component
public class AllowedFieldsInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    public AllowedFieldsInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        if (!"POST".equals(request.getMethod()) && !"PUT".equals(request.getMethod())
                && !"PATCH".equals(request.getMethod())) {
            return true;
        }

        for (MethodParameter param : handlerMethod.getMethodParameters()) {
            AllowedFields annotation = param.getParameterAnnotation(AllowedFields.class);
            if (annotation == null) continue;

            Set<String> allowed = Set.of(annotation.value());
            byte[] body;
            if (request instanceof ContentCachingRequestWrapper wrapper) {
                body = wrapper.getContentAsByteArray();
                if (body.length == 0) return true;
            } else {
                return true;
            }

            JsonNode root = objectMapper.readTree(body);
            Iterator<String> fieldNames = root.fieldNames();
            while (fieldNames.hasNext()) {
                String field = fieldNames.next();
                if (!allowed.contains(field)) {
                    response.setStatus(HttpStatus.BAD_REQUEST.value());
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"error\":\"Field '%s' is not allowed in this request\"}".formatted(field));
                    return false;
                }
            }
        }
        return true;
    }
}
