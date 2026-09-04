package io.github.alexistrejo11.pimienta.config.logger;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AuditLogInterceptor implements HandlerInterceptor {
  protected static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuditLogInterceptor.class);
  protected final AuditLogger auditLogger;
  protected final String serviceName;
  protected final ThreadLocal<Long> startTime = new ThreadLocal<>();

  protected final Set<String> excludedEndpoints = Set.of(
      "/actuator/health",
      "/actuator/info",
      "/actuator/metrics",
      "/actuator/prometheus",
      "/api/v2/health",
      "/favicon.ico",
      "/swagger-ui.html",
      "/v3/api-docs",
      "/webjars",
      "/swagger-resources");

  @Autowired
  public AuditLogInterceptor(AuditLogger auditLogger, String serviceName) {
    this.auditLogger = auditLogger;
    this.serviceName = serviceName;
  }

  @Override
  public boolean preHandle(HttpServletRequest request,
      HttpServletResponse response,
      Object handler) {
    if (shouldSkipAudit(request)) {
      return true;
    }
    startTime.set(System.currentTimeMillis());
    return true;
  }

  @Override
  public void afterCompletion(HttpServletRequest request,
      HttpServletResponse response,
      Object handler,
      Exception ex) {
    if (shouldSkipAudit(request))
      return;

    Long start = startTime.get();
    if (start == null)
      return;

    long duration = System.currentTimeMillis() - start;

    try {
      AuditEvent event = AuditEvent.builder()
          .serviceName(serviceName)
          .method(request.getMethod())
          .endpoint(sanitizeEndpoint(request.getRequestURI()))
          .operation(extractOperation(request))
          .userID(extractUserId(request))
          .clientIP(getClientIp(request))
          .userAgent(request.getHeader("User-Agent"))
          .statusCode(response.getStatus())
          .durationMs(duration)
          .success(ex == null && response.getStatus() < 400)
          .metadata(buildMetadata(request, ex))
          .build();

      auditLogger.logAuditEvent(event);

    } catch (Exception e) {
      log.warn("Error creating audit event", e);
    } finally {
      startTime.remove();
    }
  }

  protected boolean shouldSkipAudit(HttpServletRequest request) {
    String requestURI = request.getRequestURI();

    for (String excluded : excludedEndpoints) {
      if (requestURI.startsWith(excluded)) {
        return true;
      }
    }

    // Exclude OPTIONS requests commonly used for CORS preflight
    return "OPTIONS".equalsIgnoreCase(request.getMethod());
  }

  protected String sanitizeEndpoint(String endpoint) {
    if (endpoint == null)
      return "";

    return endpoint.replaceAll("/\\d+", "/{id}")
        .replaceAll("/[0-9a-fA-F-]{36}", "/{uuid}")
        .replaceAll("/[A-Z0-9]{10,}", "/{code}");
  }

  protected String extractOperation(HttpServletRequest request) {
    String method = request.getMethod().toUpperCase(Locale.ROOT);
    String sanitizedEndpoint = sanitizeEndpoint(request.getRequestURI())
        .replaceAll("^/api/v\\d+/", "")
        .replaceAll("^/", "")
        .replaceAll("/\\{[^}]+\\}", "")
        .trim();

    if (sanitizedEndpoint.isEmpty()) {
      return method + "_ROOT";
    }

    String[] segments = sanitizedEndpoint.split("/");
    String resource = segments[segments.length - 1]
        .replaceAll("[^a-zA-Z0-9_]", "_")
        .toUpperCase(Locale.ROOT);

    return method + "_" + resource;
  }

  protected String extractUserId(HttpServletRequest request) {
    // 1.= From headers
    String userIdHeader = request.getHeader("X-User-ID");
    if (userIdHeader != null && !userIdHeader.trim().isEmpty()) {
      return userIdHeader;
    }

    // 2.= From request attributes (set by some auth middleware)
    Object userIdAttr = request.getAttribute("userId");
    if (userIdAttr != null) {
      return userIdAttr.toString();
    }

    return "anonymous";
  }

  protected String getClientIp(HttpServletRequest request) {
    String[] ipHeaders = {
        "X-Forwarded-For",
        "X-Real-IP",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_X_FORWARDED_FOR",
        "HTTP_X_FORWARDED",
        "HTTP_X_CLUSTER_CLIENT_IP",
        "HTTP_CLIENT_IP",
        "HTTP_FORWARDED_FOR",
        "HTTP_FORWARDED",
        "HTTP_VIA",
        "REMOTE_ADDR"
    };

    for (String header : ipHeaders) {
      String ip = request.getHeader(header);
      if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
        // In this case, X-Forwarded-For can contain multiple IPs, the first is taken
        if (header.equals("X-Forwarded-For")) {
          return ip.split(",")[0].trim();
        }
        return ip;
      }
    }

    // Fallback to remote address
    return request.getRemoteAddr();
  }

  protected Map<String, Object> buildMetadata(HttpServletRequest request, Exception ex) {
    Map<String, Object> metadata = new HashMap<>();

    metadata.put("queryString", request.getQueryString());
    metadata.put("contentType", request.getContentType());
    metadata.put("serverName", request.getServerName());
    metadata.put("serverPort", request.getServerPort());
    metadata.put("locale", request.getLocale().toString());

    metadata.put("accept", request.getHeader("Accept"));
    metadata.put("acceptLanguage", request.getHeader("Accept-Language"));
    metadata.put("acceptEncoding", request.getHeader("Accept-Encoding"));

    if (request.getSession(false) != null) {
      metadata.put("sessionId", request.getSession().getId());
    }

    if (ex != null) {
      metadata.put("errorType", ex.getClass().getSimpleName());
      metadata.put("errorMessage", ex.getMessage());
      // Stacktrace limited to the first element for brevity
      StackTraceElement[] stackTrace = ex.getStackTrace();
      if (stackTrace.length > 0) {
        metadata.put("errorLocation", stackTrace[0].toString());
      }
    }

    return metadata;
  }
}
