package io.github.alexistrejo11.pimienta.config.cors;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig {

  private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

  @Bean
  public CorsConfigurationSource corsConfigurationSource(CorsProperties props) {
    List<String> origins = OriginLists.parse(props.getAllowedOrigins());
    if (origins.isEmpty()) {
      log.error(
          "pimienta.cors.allowed-origins is empty; browsers will block API calls with Invalid CORS request");
    } else {
      log.info("CORS allowed origins: {}", origins);
    }

    CorsConfiguration cors = new CorsConfiguration();
    cors.setAllowedOrigins(origins);
    cors.setAllowedMethods(props.getAllowedMethods());
    cors.setAllowedHeaders(props.getAllowedHeaders());
    cors.setExposedHeaders(props.getExposedHeaders());
    cors.setAllowCredentials(props.isAllowCredentials());
    cors.setMaxAge(props.getMaxAgeSeconds());

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration(props.getPathPattern(), cors);
    return source;
  }
}
