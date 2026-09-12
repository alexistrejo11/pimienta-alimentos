package io.github.alexistrejo11.pimienta;

import io.github.alexistrejo11.pimienta.config.security.DeviceJwtProperties;
import io.github.alexistrejo11.pimienta.config.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, DeviceJwtProperties.class})
public class PimientaApplication {

  public static void main(String[] args) {
    SpringApplication.run(PimientaApplication.class, args);
  }
}
