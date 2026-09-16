package io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Counts rejected telemetry HTTP outcomes without turning paths or identities into labels. */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class TelemetryRejectionMetricsFilter extends OncePerRequestFilter {

  private static final String WEB_PATH = BASE + "/telemetry/web/events";
  private static final String POS_PATH = BASE + "/pos/telemetry/events";
  private final MeterRegistry meterRegistry;

  public TelemetryRejectionMetricsFilter(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return !WEB_PATH.equals(path) && !POS_PATH.equals(path);
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      filterChain.doFilter(request, response);
    } finally {
      int status = response.getStatus();
      if (status >= 400) {
        String source = WEB_PATH.equals(request.getRequestURI()) ? "web" : "pos";
        meterRegistry.counter("pimienta.telemetry.events.rejected", "source", source, "status",
            boundedStatus(status)).increment();
      }
    }
  }

  private static String boundedStatus(int status) {
    return switch (status) {
      case 400, 401, 403, 413, 429 -> String.valueOf(status);
      default -> status >= 500 ? "5xx" : "other";
    };
  }
}
