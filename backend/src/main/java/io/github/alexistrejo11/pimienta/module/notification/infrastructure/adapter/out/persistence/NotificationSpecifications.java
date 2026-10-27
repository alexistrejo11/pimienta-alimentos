package io.github.alexistrejo11.pimienta.module.notification.infrastructure.adapter.out.persistence;

import io.github.alexistrejo11.pimienta.config.web.ClientTimeZones;
import io.github.alexistrejo11.pimienta.module.notification.core.application.query.NotificationSearchCriteria;
import io.github.alexistrejo11.pimienta.module.notification.core.application.query.NotificationSearchScope;
import io.github.alexistrejo11.pimienta.module.notification.core.application.query.NotificationStatisticsCriteria;
import io.github.alexistrejo11.pimienta.module.notification.core.domain.enums.NotificationChannel;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class NotificationSpecifications {

  private NotificationSpecifications() {}

  public static Specification<NotificationJpaEntity> notDeleted() {
    return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
  }

  public static Specification<NotificationJpaEntity> fromCriteria(NotificationSearchCriteria criteria) {
    return (root, query, cb) -> {
      List<Predicate> parts = new ArrayList<>();
      parts.add(cb.isNull(root.get("deletedAt")));

      NotificationSearchCriteria effective = criteria != null ? criteria : new NotificationSearchCriteria(
          null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
          NotificationSearchScope.ADMIN);

      if (effective.scope() == NotificationSearchScope.MANAGER_LOG) {
        parts.add(cb.equal(root.get("channel"), NotificationChannel.LOG));
        ZoneId zone = ClientTimeZones.current();
        LocalDate today = LocalDate.now(zone);
        Instant start = today.atStartOfDay(zone).toInstant();
        Instant end = today.atTime(LocalTime.MAX).atZone(zone).toInstant();
        // createdAt is the UTC LocalDateTime written by UtcLocalDateTimeConverter.
        parts.add(
            cb.between(
                root.get("createdAt"),
                LocalDateTime.ofInstant(start, ZoneOffset.UTC),
                LocalDateTime.ofInstant(end, ZoneOffset.UTC)));
      }

      if (effective.id() != null) {
        parts.add(cb.equal(root.get("id"), effective.id()));
      }
      if (effective.channel() != null && effective.scope() != NotificationSearchScope.MANAGER_LOG) {
        parts.add(cb.equal(root.get("channel"), effective.channel()));
      }
      if (effective.type() != null) {
        parts.add(cb.equal(root.get("type"), effective.type()));
      }
      if (effective.status() != null) {
        parts.add(cb.equal(root.get("status"), effective.status()));
      }
      if (effective.recipientUserId() != null) {
        parts.add(cb.equal(root.get("recipientUserId"), effective.recipientUserId()));
      }
      if (effective.relatedUserId() != null) {
        parts.add(cb.equal(root.get("relatedUserId"), effective.relatedUserId()));
      }
      if (effective.correlationId() != null && !effective.correlationId().isBlank()) {
        parts.add(
            cb.like(
                cb.lower(root.get("correlationId")),
                "%" + effective.correlationId().trim().toLowerCase() + "%"));
      }
      if (effective.recipientEmailContains() != null && !effective.recipientEmailContains().isBlank()) {
        parts.add(
            cb.like(
                cb.lower(root.get("recipientEmail")),
                "%" + effective.recipientEmailContains().trim().toLowerCase() + "%"));
      }
      if (effective.recipientPhoneContains() != null && !effective.recipientPhoneContains().isBlank()) {
        parts.add(
            cb.like(
                root.get("recipientPhone"),
                "%" + effective.recipientPhoneContains().trim() + "%"));
      }
      if (effective.subjectContains() != null && !effective.subjectContains().isBlank()) {
        parts.add(
            cb.like(
                cb.lower(root.get("subject")),
                "%" + effective.subjectContains().trim().toLowerCase() + "%"));
      }
      if (effective.bodyContains() != null && !effective.bodyContains().isBlank()) {
        parts.add(
            cb.like(
                cb.lower(root.get("body")),
                "%" + effective.bodyContains().trim().toLowerCase() + "%"));
      }
      if (effective.createdFrom() != null
          && effective.scope() != NotificationSearchScope.MANAGER_LOG) {
        parts.add(cb.greaterThanOrEqualTo(root.get("createdAt"), effective.createdFrom()));
      }
      if (effective.createdTo() != null && effective.scope() != NotificationSearchScope.MANAGER_LOG) {
        parts.add(cb.lessThanOrEqualTo(root.get("createdAt"), effective.createdTo()));
      }
      if (effective.sentFrom() != null) {
        parts.add(cb.greaterThanOrEqualTo(root.get("sentAt"), effective.sentFrom()));
      }
      if (effective.sentTo() != null) {
        parts.add(cb.lessThanOrEqualTo(root.get("sentAt"), effective.sentTo()));
      }

      return cb.and(parts.toArray(Predicate[]::new));
    };
  }

  public static Specification<NotificationJpaEntity> forStatistics(NotificationStatisticsCriteria criteria) {
    return (root, query, cb) -> {
      List<Predicate> parts = new ArrayList<>();
      parts.add(cb.isNull(root.get("deletedAt")));
      if (criteria != null) {
        if (criteria.from() != null) {
          parts.add(cb.greaterThanOrEqualTo(root.get("createdAt"), criteria.from()));
        }
        if (criteria.to() != null) {
          parts.add(cb.lessThanOrEqualTo(root.get("createdAt"), criteria.to()));
        }
      }
      return cb.and(parts.toArray(Predicate[]::new));
    };
  }
}
