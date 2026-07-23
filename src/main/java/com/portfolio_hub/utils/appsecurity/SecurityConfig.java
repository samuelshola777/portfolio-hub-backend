package com.portfolio_hub.utils.appsecurity;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final JwtFilter jwtFilter;
  private final String allowedOrigins;

  public SecurityConfig(
    JwtFilter jwtFilter,
    @Value(
      "${application.allowed-origins:http://localhost:3000}"
    ) String allowedOrigins
  ) {
    this.jwtFilter = jwtFilter;
    this.allowedOrigins = allowedOrigins;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http)
    throws Exception {
    http
      .csrf(csrf -> csrf.disable())
      .cors(cors -> cors.configurationSource(corsConfigurationSource()))
      .sessionManagement(session ->
        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      )
      .exceptionHandling(errors ->
        errors
          .authenticationEntryPoint(
            new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
          )
          .accessDeniedHandler((request, response, exception) ->
            response.sendError(
              HttpStatus.FORBIDDEN.value(),
              "Administrator access is required"
            )
          )
      )
      .authorizeHttpRequests(auth ->
        auth
          .requestMatchers(HttpMethod.OPTIONS, "/**")
          .permitAll()
          .requestMatchers(
            "/api/v1/auth/public/**",
            "/api/v1/portfolios/public/**",
            "/api/v1/utilities/public/**",
            "/api/v1/analytics/public/**",
            "/api/v1/enquiries/public/**",
            "/api/v1/businesses/public/**",
            "/api/v1/github/public/**",
            "/api/v1/system/public/**",
            "/api/v1/setup-requests/public/**",
            "/api/v1/subscriptions/public/**",
            "/api/v1/billing/public/**",
            "/api/v1/system/keep-alive",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/api-docs/**",
            "/v3/api-docs/**"
          )
          .permitAll()
          .requestMatchers(
            "/api/v1/admin/**",
            "/api/v1/announcements/admin/**",
            "/api/v1/feedback/admin/**",
            "/api/v1/setup-requests/admin/**"
          )
          .hasRole("SUPER_ADMIN")
          .anyRequest()
          .authenticated()
      )
      .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(
      Arrays.stream(allowedOrigins.split(","))
        .map(String::trim)
        .filter(origin -> !origin.isBlank())
        .toList()
    );
    configuration.setAllowedMethods(
      List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
    );
    configuration.setAllowedHeaders(
      List.of("Authorization", "Content-Type", "Accept", "X-Requested-With")
    );
    configuration.setExposedHeaders(List.of("Content-Disposition"));
    configuration.setAllowCredentials(false);

    UrlBasedCorsConfigurationSource source =
      new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
