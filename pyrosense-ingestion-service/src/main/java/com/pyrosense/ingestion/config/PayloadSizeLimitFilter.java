package com.pyrosense.ingestion.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1)
public class PayloadSizeLimitFilter implements Filter {

    private static final int MAX_CONTENT_LENGTH = 8192;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        int contentLength = httpRequest.getContentLength();

        if (contentLength > MAX_CONTENT_LENGTH) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            httpResponse.getWriter().write("{\"status\":\"REJECTED\",\"message\":\"Payload too large\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
