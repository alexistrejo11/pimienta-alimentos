package io.github.alexistrejo11.pimienta.module.notification.infrastructure.adapter.inbound.web;

import io.github.alexistrejo11.pimienta.module.notification.core.application.query.NotificationSearchCriteria;
import io.github.alexistrejo11.pimienta.module.notification.core.domain.Notification;
import io.github.alexistrejo11.pimienta.module.notification.core.port.input.NotificationQueryUseCases;
import io.github.alexistrejo11.pimienta.module.notification.infrastructure.adapter.inbound.web.doc.DocNotificationLogs;
import io.github.alexistrejo11.pimienta.module.notification.infrastructure.adapter.inbound.web.doc.DocNotificationLogsSearch;
import io.github.alexistrejo11.pimienta.module.notification.infrastructure.adapter.inbound.web.dto.NotificationManagerLogSearchRequest;
import io.github.alexistrejo11.pimienta.module.notification.infrastructure.adapter.inbound.web.dto.NotificationResponse;
import io.github.alexistrejo11.pimienta.module.notification.infrastructure.adapter.inbound.web.mapper.NotificationWebMapper;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications/logs")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocNotificationLogs
@PreAuthorize("hasRole('ADMIN')")
public class NotificationLogController {

  private final NotificationQueryUseCases notificationQueryUseCases;

  public NotificationLogController(NotificationQueryUseCases notificationQueryUseCases) {
    this.notificationQueryUseCases = notificationQueryUseCases;
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocNotificationLogsSearch
  public PagedResponse<NotificationResponse> searchTodayLogs(
      @Valid @ModelAttribute NotificationManagerLogSearchRequest filter) {
    NotificationSearchCriteria criteria = NotificationWebMapper.toManagerCriteria(filter);
    Page<Notification> page =
        notificationQueryUseCases.searchManagerLogs(criteria, filter.toPageable());
    return PagedResponse.map(page, NotificationWebMapper::toResponse);
  }
}
