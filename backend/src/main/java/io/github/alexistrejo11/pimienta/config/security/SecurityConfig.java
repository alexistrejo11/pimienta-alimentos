package io.github.alexistrejo11.pimienta.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
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
                        "/actuator/prometheus"
        };

        private static final String[] AUTH_PUBLIC_PATHS = {
                        "/api/v1/auth/**"
        };

        private static final String[] HEALTH_PUBLIC_PATHS = {
                        "/api/v2/health/**",
                        "/health"
        };

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
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
                                                                .requestMatchers("/actuator/**").hasRole("ADMIN")
                                                                .requestMatchers("/api/v1/notifications/management/**")
                                                                .hasRole("ADMIN")
                                                                .requestMatchers("/api/v1/notifications/logs/**")
                                                                .hasAnyRole("ADMIN", "MANAGER")
                                                                .requestMatchers("/api/v1/files/management/**")
                                                                .hasRole("ADMIN")
                                                                .requestMatchers("/api/v1/files/resources/**")
                                                                .hasAnyRole("ADMIN", "MANAGER")
                                                                .requestMatchers("/api/v1/employees/**").authenticated()
                                                                .anyRequest().authenticated())
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
                return http.build();
        }
}
