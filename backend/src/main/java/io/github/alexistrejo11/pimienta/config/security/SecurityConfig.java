package io.github.alexistrejo11.pimienta.config.security;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private static final String[] SWAGGER_PUBLIC_PATHS = {
      "/swagger-ui/**",
      "/swagger-ui.html",
      "/v3/api-docs/**",
      "/v3/api-docs.yaml"
  };

  private static final String[] ACTUATOR_PUBLIC_PATHS = {
      "/actuator/health",
      "/actuator/health/**",
      "/actuator/info",
      // Scraped by Prometheus on the Docker network (same port as API in prod).
      "/actuator/prometheus"
  };

  private static final String[] AUTH_PUBLIC_PATHS = {
      BASE + "/auth/**"
  };

  private static final String[] POS_DEVICE_PUBLIC_PATHS = {
      BASE + "/pos/devices/enroll",
      BASE + "/pos/devices/refresh",
      BASE + "/pos/releases/android/latest"
  };

  private static final String[] HEALTH_PUBLIC_PATHS = {
      "/api/v2/health/**",
      "/health"
  };

  @Bean
  @Primary
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  @Qualifier("pinPasswordEncoder")
  public PasswordEncoder pinPasswordEncoder() {
    return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
  }

  /** Staff JWT principal only (blocks POS device tokens on web/admin routes). */
  static AuthorizationManager<RequestAuthorizationContext> staffJwtOnly() {
    return (authentication, context) -> {
      var auth = authentication.get();
      boolean granted = auth != null
          && auth.isAuthenticated()
          && auth.getPrincipal() instanceof JwtAuthenticationContext;
      return new AuthorizationDecision(granted);
    };
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      PimientaAuthenticationEntryPoint authenticationEntryPoint,
      PimientaAccessDeniedHandler accessDeniedHandler) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(
            session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            ex -> ex.authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
        .authorizeHttpRequests(
            auth -> auth.requestMatchers(SWAGGER_PUBLIC_PATHS).permitAll()
                .requestMatchers(ACTUATOR_PUBLIC_PATHS).permitAll()
                .requestMatchers(HEALTH_PUBLIC_PATHS).permitAll()
                .requestMatchers(AUTH_PUBLIC_PATHS).permitAll()
                .requestMatchers(POS_DEVICE_PUBLIC_PATHS).permitAll()
                .requestMatchers(BASE + "/users/me", BASE + "/users/me/**")
                .access(staffJwtOnly())
                .requestMatchers(HttpMethod.POST, BASE + "/telemetry/web/events")
                .access(staffJwtOnly())
                // POS operator read/report and sale access is deliberately narrower than POS
                // admin.
                .requestMatchers(
                    BASE + "/pos/admin/reports/sales",
                    BASE + "/pos/admin/reports/products",
                    BASE + "/pos/admin/reports/waste-cancellations",
                    BASE + "/pos/admin/reports/shift-closes")
                .hasAnyRole("ADMIN", "MANAGER", "POS_OPERATOR")
                .requestMatchers(BASE + "/pos/admin/**")
                .hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(
                    BASE + "/headquarters/*/pos-settings",
                    BASE + "/headquarters/*/pos-settings/**")
                .hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(
                    HttpMethod.GET,
                    BASE + "/headquarters/*/pos-catalog",
                    BASE + "/headquarters/*/pos-catalog/**",
                    BASE + "/headquarters/*/pos-categories",
                    BASE + "/headquarters/*/pos-categories/**")
                .hasAnyRole("ADMIN", "MANAGER", "POS_OPERATOR")
                .requestMatchers(
                    BASE + "/headquarters/*/pos-categories",
                    BASE + "/headquarters/*/pos-categories/**")
                .hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(
                    BASE + "/headquarters/*/pos-catalog",
                    BASE + "/headquarters/*/pos-catalog/**")
                .hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(HttpMethod.GET, BASE + "/headquarters/statistics")
                .hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, BASE + "/headquarters/export")
                .hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, BASE + "/headquarters")
                .hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, BASE + "/headquarters/name/**")
                .hasAnyRole("ADMIN", "MANAGER", "POS_OPERATOR")
                .requestMatchers(HttpMethod.GET, BASE + "/headquarters/*")
                .hasAnyRole("ADMIN", "MANAGER", "POS_OPERATOR")
                // POS operators may inspect stock only; inventory writes stay with staff roles.
                .requestMatchers(HttpMethod.GET, BASE + "/inventory/**")
                .hasAnyRole("ADMIN", "MANAGER", "POS_OPERATOR")
                .requestMatchers(HttpMethod.POST, BASE + "/inventory/transactions/sale")
                .hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(
                    HttpMethod.POST,
                    BASE + "/inventory/transactions/adjustment",
                    BASE + "/inventory/transactions/physical-adjustment")
                .hasRole("ADMIN")
                .requestMatchers(
                    HttpMethod.POST,
                    BASE + "/inventory/count-sessions/*/approve")
                .hasRole("ADMIN")
                // Managers own CRM, talent, tasks, and HQ-scoped inventory workflows.
                .requestMatchers(
                    BASE + "/clients/**",
                    BASE + "/opportunities/**",
                    BASE + "/projects/**",
                    BASE + "/tasks/**",
                    BASE + "/employees/**",
                    BASE + "/contracts/**",
                    BASE + "/payroll/**",
                    BASE + "/inventory/**")
                .hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(BASE + "/pos/**")
                .hasAuthority(DeviceAuthenticationContext.AUTHORITY_SCOPE_POS_SYNC)
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .anyRequest()
                .hasRole("ADMIN"))
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
