package com.prep.taskpulse.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    static final String HEADER_NAME = "X-Correlation-Id";
    static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String correlationId = resolveCorrelationId(request.getHeader(HEADER_NAME));
        MDC.put(MDC_KEY, correlationId);

        try {
            filterChain.doFilter(request,response);
        } finally {
            // servlet threads are reused so clean for every request.
            // or else, the next one would have the same value.
            MDC.remove(MDC_KEY);
        }
    }

    private String resolveCorrelationId(String value){
        return UUID.randomUUID().toString();
    }
}
