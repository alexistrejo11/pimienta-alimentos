package io.github.alexistrejo11.pimienta.config.logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuditLogInterceptorTest {

  @Test
  void doesNotAuditReadRequests() {
    RecordingAuditLogger logger = new RecordingAuditLogger();
    AuditLogInterceptor interceptor = new AuditLogInterceptor(logger, "test-service");
    MockHttpServletRequest request = request("GET", "/api/v1/employees/42?token=secret");

    interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
    interceptor.afterCompletion(request, new MockHttpServletResponse(), new Object(), null);

    assertNull(logger.event.get());
  }

  @Test
  void auditsMutationWithoutSensitiveRequestMetadata() {
    RecordingAuditLogger logger = new RecordingAuditLogger();
    AuditLogInterceptor interceptor = new AuditLogInterceptor(logger, "test-service");
    MockHttpServletRequest request = request("POST", "/api/v1/employees?password=secret");
    MockHttpServletResponse response = new MockHttpServletResponse();
    response.setStatus(201);

    interceptor.preHandle(request, response, new Object());
    interceptor.afterCompletion(request, response, new Object(), new IllegalArgumentException("sensitive detail"));

    AuditEvent event = logger.event.get();
    assertNotNull(event);
    assertEquals("POST", event.getMethod());
    assertEquals("/api/v1/employees", event.getEndpoint());
    assertFalse(event.getMetadata().containsKey("queryString"));
    assertFalse(event.getMetadata().containsKey("errorMessage"));
    assertEquals("IllegalArgumentException", event.getMetadata().get("errorType"));
  }

  private static MockHttpServletRequest request(String method, String uri) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
    request.setContentType("application/json");
    return request;
  }

  private static final class RecordingAuditLogger extends AuditLogger {
    private final AtomicReference<AuditEvent> event = new AtomicReference<>();

    @Override
    public void logAuditEvent(AuditEvent auditEvent) {
      event.set(auditEvent);
    }
  }
}
