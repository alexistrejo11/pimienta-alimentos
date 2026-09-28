package io.github.alexistrejo11.pimienta.module.notification.infrastructure.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.alexistrejo11.pimienta.module.notification.core.domain.Notification;
import java.time.LocalDateTime;
import java.util.Map;

final class NotificationPersistenceMapper {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final TypeReference<Map<String, String>> TEMPLATE_VARS_TYPE = new TypeReference<>() {};

  private NotificationPersistenceMapper() {}

  static NotificationJpaEntity toJpa(Notification domain) {
    NotificationJpaEntity e = new NotificationJpaEntity();
    e.setId(domain.getId());
    e.setChannel(domain.getChannel());
    e.setType(domain.getType());
    e.setStatus(domain.getStatus());
    e.setRecipientEmail(domain.getRecipientEmail());
    e.setRecipientPhone(domain.getRecipientPhone());
    e.setRecipientDisplayName(domain.getRecipientDisplayName());
    e.setRecipientUserId(domain.getRecipientUserId());
    e.setSubject(domain.getSubject());
    e.setBody(domain.getBody());
    e.setTemplateId(domain.getTemplateId());
    e.setTemplateVariablesJson(toJson(domain.getTemplateVariables()));
    e.setCorrelationId(domain.getCorrelationId());
    e.setRelatedUserId(domain.getRelatedUserId());
    e.setLocale(domain.getLocale() != null ? domain.getLocale() : "en");
    e.setAttemptCount(domain.getAttemptCount());
    e.setLastError(domain.getLastError());
    e.setSentAt(domain.getSentAt());
    LocalDateTime now = LocalDateTime.now();
    e.setCreatedAt(domain.getCreatedAt() != null ? domain.getCreatedAt() : now);
    e.setUpdatedAt(now);
    e.setDeletedAt(null);
    return e;
  }

  static Notification toDomain(NotificationJpaEntity e) {
    return Notification.builder()
        .id(e.getId())
        .channel(e.getChannel())
        .type(e.getType())
        .status(e.getStatus())
        .recipientEmail(e.getRecipientEmail())
        .recipientPhone(e.getRecipientPhone())
        .recipientDisplayName(e.getRecipientDisplayName())
        .recipientUserId(e.getRecipientUserId())
        .subject(e.getSubject())
        .body(e.getBody())
        .templateId(e.getTemplateId())
        .templateVariables(fromJson(e.getTemplateVariablesJson()))
        .correlationId(e.getCorrelationId())
        .relatedUserId(e.getRelatedUserId())
        .locale(e.getLocale())
        .attemptCount(e.getAttemptCount())
        .lastError(e.getLastError())
        .createdAt(e.getCreatedAt())
        .sentAt(e.getSentAt())
        .compose();
  }

  private static String toJson(Map<String, String> map) {
    if (map == null || map.isEmpty()) {
      return null;
    }
    try {
      return MAPPER.writeValueAsString(map);
    } catch (JsonProcessingException ex) {
      throw new IllegalArgumentException("Failed to serialize template variables", ex);
    }
  }

  private static Map<String, String> fromJson(String json) {
    if (json == null || json.isBlank()) {
      return Map.of();
    }
    try {
      return MAPPER.readValue(json, TEMPLATE_VARS_TYPE);
    } catch (JsonProcessingException ex) {
      return Map.of();
    }
  }
}
