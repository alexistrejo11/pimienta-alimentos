package io.github.alexistrejo11.pimienta.config.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

  public static final String TRACE_ID_HEADER = "X-Trace-Id";
  public static final String TRACE_ID_REQUEST_ATTR = TraceIdFilter.class.getName() + ".traceId";

  private static final Pattern CANONICAL_UUID =
      Pattern.compile(
          "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String traceId = acceptedTraceId(request.getHeader(TRACE_ID_HEADER));
    request.setAttribute(TRACE_ID_REQUEST_ATTR, traceId);
    response.setHeader(TRACE_ID_HEADER, traceId);
    MDC.put("traceId", traceId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove("traceId");
    }
  }

  private static String acceptedTraceId(String header) {
    if (header == null) {
      return UUID.randomUUID().toString();
    }
    String trimmed = header.trim();
    if (CANONICAL_UUID.matcher(trimmed).matches()) {
      return trimmed;
    }
    return UUID.randomUUID().toString();
  }
}
