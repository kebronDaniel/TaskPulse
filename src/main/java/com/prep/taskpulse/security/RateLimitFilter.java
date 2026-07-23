package com.prep.taskpulse.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private static final int LIMIT = 100;
    private static final int WINDOW_SECONDS = 60;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
            String identifier = resolveIdentifier(request);
            String key = "rate_limit:" + identifier;
        Long count = null;
        try {
            count = executeAtomicIncrement(key);
        } catch (DataAccessException exception) {
            log.error("Rate limiter unavailable: allowing requests", exception);
            filterChain.doFilter(request,response);
            return;
        }
        if (count == null){
            filterChain.doFilter(request,response);
            return; // Fail open when Redis returns no script result.
        }
        response.setHeader("X-RateLimit-Limit", String.valueOf(LIMIT));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, (LIMIT - count))));

        if (count > LIMIT){
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(WINDOW_SECONDS));
            response.setContentType("application/problem+json");
            response.getWriter().write("""
                {
                  "status": 429,
                  "title": "Too Many Requests",
                  "detail": "Rate limit exceeded"
                }
                """);
            return;
        }
        filterChain.doFilter(request,response);
    }

    private String resolveIdentifier(HttpServletRequest request){

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticatedUser = authentication != null && authentication.isAuthenticated()
                                        && !(authentication instanceof AnonymousAuthenticationToken);

        if (isAuthenticatedUser) return "user:" + authentication.getName();
        return "ip:" + request.getRequestURI();
    }

    private Long executeAtomicIncrement(String key) {
        String luaScript = """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """;
        DefaultRedisScript<Long> script =
                new DefaultRedisScript<>(luaScript, Long.class);

        return redisTemplate.execute(
                script,
                List.of(key),
                String.valueOf(WINDOW_SECONDS)
        );
    }

}
