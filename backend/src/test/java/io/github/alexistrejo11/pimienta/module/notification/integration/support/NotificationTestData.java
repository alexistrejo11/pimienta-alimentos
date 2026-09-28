package io.github.alexistrejo11.pimienta.module.notification.integration.support;

import io.github.alexistrejo11.pimienta.module.notification.core.domain.enums.NotificationChannel;
import io.github.alexistrejo11.pimienta.module.notification.core.domain.enums.NotificationStatus;
import io.github.alexistrejo11.pimienta.module.notification.core.domain.enums.NotificationType;
import io.github.alexistrejo11.pimienta.module.notification.infrastructure.adapter.out.persistence.NotificationJpaEntity;
import io.github.alexistrejo11.pimienta.module.notification.infrastructure.adapter.out.persistence.NotificationJpaRepository;
import java.time.LocalDateTime;
import java.util.UUID;

/** Seeds {@link NotificationJpaEntity} rows for read-only notification API integration tests. */
public final class NotificationTestData {

  private NotificationTestData() {}

  public static UUID seed(
      NotificationJpaRepository repository,
      NotificationChannel channel,
      NotificationStatus status,
      String correlationId,
      String subject,
      LocalDateTime createdAt) {
    LocalDateTime ts = createdAt != null ? createdAt : LocalDateTime.now();
    NotificationJpaEntity entity = new NotificationJpaEntity();
    UUID id = UUID.randomUUID();
    entity.setId(id);
    entity.setChannel(channel);
    entity.setType(NotificationType.ACCOUNT_PENDING_APPROVAL);
    entity.setStatus(status);
    entity.setRecipientEmail("it-" + correlationId + "@mail.com");
    entity.setSubject(subject);
    entity.setBody("Integration test body for " + correlationId);
    entity.setCorrelationId(correlationId);
    entity.setLocale("en");
    entity.setAttemptCount(0);
    entity.setCreatedAt(ts);
    entity.setUpdatedAt(ts);
    if (status == NotificationStatus.SENT) {
      entity.setSentAt(ts);
    }
    repository.saveAndFlush(entity);
    return id;
  }
}
