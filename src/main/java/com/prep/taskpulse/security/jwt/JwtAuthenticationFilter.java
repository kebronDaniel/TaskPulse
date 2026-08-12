package com.prep.taskpulse.security.jwt;

import com.prep.taskpulse.security.service.TaskFlowUserDetails;
import com.prep.taskpulse.security.service.TaskFlowUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final TaskFlowUserDetailsService userDetailsService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    final String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return; // to allow the request continue because the client might be consuming open endpoints.
    }
    final String jwt = authHeader.substring(7);

    final String userId = jwtService.extractSubject(jwt);
    if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      UUID parsedUserId = UUID.fromString(userId);
      TaskFlowUserDetails userDetails = userDetailsService.loadUserById(parsedUserId);
      if (jwtService.isTokenValid(jwt, userDetails)) {
        UsernamePasswordAuthenticationToken authenticationToken =
            new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        // save to the global vault.
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
      }
    }
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.getPrincipal() instanceof TaskFlowUserDetails userDetails) {
      try (MDC.MDCCloseable ignored = MDC.putCloseable("userId", userDetails.getId().toString())) {
        filterChain.doFilter(request, response);
      }
      return;
    }
    filterChain.doFilter(request, response);
  }
}
