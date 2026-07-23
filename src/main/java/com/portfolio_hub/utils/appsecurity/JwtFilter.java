package com.portfolio_hub.utils.appsecurity;

import com.portfolio_hub.userauthmgt.user.User;
import com.portfolio_hub.userauthmgt.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final UserRepository userRepository;

  @Override
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain filterChain
  ) throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header == null || !header.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }
    try {
      String token = header.substring(7);
      String email = jwtService.extractUsername(token);
      if (SecurityContextHolder.getContext().getAuthentication() == null) {
        User user = userRepository
          .findByEmailAddressIgnoreCaseAndDeletedFalse(email)
          .orElse(null);
        int expectedTokenVersion = user == null ||
          user.getTokenVersion() == null
          ? 0
          : user.getTokenVersion();
        if (
          user != null &&
          jwtService.isAccessToken(token) &&
          jwtService.isTokenValid(token, user.getEmailAddress()) &&
          jwtService.extractTokenVersion(token) == expectedTokenVersion &&
          user.getStatus() != User.AccountStatus.BLOCKED
        ) {
          String authorityRole = user.getRole() == User.UserRole.USER
            ? "PROFESSIONAL"
            : user.getRole().name();
          var auth = new UsernamePasswordAuthenticationToken(
            user.getEmailAddress(),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + authorityRole))
          );
          SecurityContextHolder.getContext().setAuthentication(auth);
        }
      }
    } catch (Exception ignored) {
      SecurityContextHolder.clearContext();
    }
    filterChain.doFilter(request, response);
  }
}
