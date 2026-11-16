package io.github.alexistrejo11.pimienta.module.notification.core.domain;

import io.github.alexistrejo11.pimienta.module.notification.core.domain.enums.NotificationChannel;
import io.github.alexistrejo11.pimienta.module.notification.core.domain.enums.NotificationStatus;
import io.github.alexistrejo11.pimienta.module.notification.core.domain.enums.NotificationType;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import io.github.alexistrejo11.pimienta.shared.util.Strings;

/**
 * Outbound notification payload. Persistence is optional; use cases build instances and hand them
 * to channel senders.
 */
public final class Notification {

  private UUID id;
  private NotificationChannel channel;
  private NotificationType type;
  private NotificationStatus status;
  private String recipientEmail;
  private String recipientPhone;
  private String recipientDisplayName;
  private Long recipientUserId;
  private String subject;
  private String body;
  private String templateId;
  private Map<String, String> templateVariables;
  private String correlationId;
  private Long relatedUserId;
  private String locale;
  private int attemptCount;
  private String lastError;
  private LocalDateTime createdAt;
  private LocalDateTime sentAt;

  private Notification() {}

  public static SafeBuilder builder() {
    return new SafeBuilder();
  }

  public UUID getId() {
    return id;
  }

  public NotificationChannel getChannel() {
    return channel;
  }

  public NotificationType getType() {
    return type;
  }

  public NotificationStatus getStatus() {
    return status;
  }

  public String getRecipientEmail() {
    return recipientEmail;
  }

  public String getRecipientPhone() {
    return recipientPhone;
  }

  public String getRecipientDisplayName() {
    return recipientDisplayName;
  }

  public Long getRecipientUserId() {
    return recipientUserId;
  }

  public String getSubject() {
    return subject;
  }

  public String getBody() {
    return body;
  }

  public String getTemplateId() {
    return templateId;
  }

  public Map<String, String> getTemplateVariables() {
    return templateVariables == null ? Map.of() : Collections.unmodifiableMap(templateVariables);
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public Long getRelatedUserId() {
    return relatedUserId;
  }

  public String getLocale() {
    return locale;
  }

  public int getAttemptCount() {
    return attemptCount;
  }

  public String getLastError() {
    return lastError;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getSentAt() {
    return sentAt;
  }

  public static final class SafeBuilder {
    private final Notification n = new Notification();

    private SafeBuilder() {}

    public SafeBuilder id(UUID id) {
      n.id = id;
      return this;
    }

    public SafeBuilder channel(NotificationChannel channel) {
      n.channel = channel;
      return this;
    }

    public SafeBuilder type(NotificationType type) {
      n.type = type;
      return this;
    }

    public SafeBuilder status(NotificationStatus status) {
      n.status = status;
      return this;
    }

    public SafeBuilder recipientEmail(String recipientEmail) {
      n.recipientEmail = Strings.blankToNull(recipientEmail);
      return this;
    }

    public SafeBuilder recipientPhone(String recipientPhone) {
      n.recipientPhone = Strings.blankToNull(recipientPhone);
      return this;
    }

    public SafeBuilder recipientDisplayName(String recipientDisplayName) {
      n.recipientDisplayName = Strings.blankToNull(recipientDisplayName);
      return this;
    }

    public SafeBuilder recipientUserId(Long recipientUserId) {
      n.recipientUserId = recipientUserId;
      return this;
    }

    public SafeBuilder subject(String subject) {
      n.subject = Strings.blankToNull(subject);
      return this;
    }

    public SafeBuilder body(String body) {
      n.body = Strings.blankToNull(body);
      return this;
    }

    public SafeBuilder templateId(String templateId) {
      n.templateId = Strings.blankToNull(templateId);
      return this;
    }

    public SafeBuilder templateVariables(Map<String, String> templateVariables) {
      if (templateVariables == null || templateVariables.isEmpty()) {
        n.templateVariables = Map.of();
      } else {
        n.templateVariables = new HashMap<>(templateVariables);
      }
      return this;
    }

    public SafeBuilder correlationId(String correlationId) {
      n.correlationId = Strings.blankToNull(correlationId);
      return this;
    }

    public SafeBuilder relatedUserId(Long relatedUserId) {
      n.relatedUserId = relatedUserId;
      return this;
    }

    public SafeBuilder locale(String locale) {
      n.locale = Strings.blankToNull(locale);
      return this;
    }

    public SafeBuilder attemptCount(int attemptCount) {
      n.attemptCount = attemptCount;
      return this;
    }

    public SafeBuilder lastError(String lastError) {
      n.lastError = Strings.blankToNull(lastError);
      return this;
    }

    public SafeBuilder createdAt(LocalDateTime createdAt) {
      n.createdAt = createdAt;
      return this;
    }

    public SafeBuilder sentAt(LocalDateTime sentAt) {
      n.sentAt = sentAt;
      return this;
    }

    public Notification compose() {
      if (n.id == null) {
        n.id = UUID.randomUUID();
      }
      if (n.channel == null) {
        throw new IllegalArgumentException("channel is required");
      }
      if (n.type == null) {
        n.type = NotificationType.UNDEFINED;
      }
      if (n.status == null) {
        n.status = NotificationStatus.PENDING;
      }
      if (n.createdAt == null) {
        n.createdAt = LocalDateTime.now();
      }
      if (n.locale == null) {
        n.locale = "en";
      }
      if (n.templateVariables == null) {
        n.templateVariables = Map.of();
      }
      return n;
    }
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Notification that)) {
      return false;
    }
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
