package io.github.alexistrejo11.pimienta.module.notification.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.module.notification.core.application.query.NotificationSearchCriteria;
import io.github.alexistrejo11.pimienta.module.notification.core.application.query.NotificationStatisticsCriteria;
import io.github.alexistrejo11.pimienta.module.notification.core.domain.Notification;
import io.github.alexistrejo11.pimienta.module.notification.core.port.input.NotificationQueryUseCases;
import io.github.alexistrejo11.pimienta.module.notification.infrastructure.adapter.inbound.web.doc.DocNotificationManagement;
import io.github.alexistrejo11.pimienta.module.notification.infrastructure.adapter.inbound.web.doc.DocNotificationManagementSearch;
import io.github.alexistrejo11.pimienta.module.notification.infrastructure.adapter.inbound.web.doc.DocNotificationManagementStatistics;
import io.github.alexistrejo11.pimienta.module.notification.infrastructure.adapter.inbound.web.dto.NotificationAdminSearchRequest;
import io.github.alexistrejo11.pimienta.module.notification.infrastructure.adapter.inbound.web.dto.NotificationResponse;
import io.github.alexistrejo11.pimienta.module.notification.infrastructure.adapter.inbound.web.dto.NotificationStatisticsRequest;
import io.github.alexistrejo11.pimienta.module.notification.infrastructure.adapter.inbound.web.dto.NotificationStatisticsResponse;
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
@RequestMapping(BASE + "/notifications/management")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocNotificationManagement
@PreAuthorize("hasRole('ADMIN')")
public class NotificationManagementController {

  private final NotificationQueryUseCases notificationQueryUseCases;

  public NotificationManagementController(NotificationQueryUseCases notificationQueryUseCases) {
    this.notificationQueryUseCases = notificationQueryUseCases;
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocNotificationManagementSearch
  public PagedResponse<NotificationResponse> search(
      @Valid @ModelAttribute NotificationAdminSearchRequest filter) {
    NotificationSearchCriteria criteria = NotificationWebMapper.toAdminCriteria(filter);
    Page<Notification> page =
        notificationQueryUseCases.searchAdmin(criteria, filter.toPageable());
    return PagedResponse.map(page, NotificationWebMapper::toResponse);
  }

  @GetMapping("/statistics")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocNotificationManagementStatistics
  public NotificationStatisticsResponse statistics(
      @Valid @ModelAttribute NotificationStatisticsRequest request) {
    NotificationStatisticsCriteria criteria = NotificationWebMapper.toStatisticsCriteria(request);
    return NotificationWebMapper.toStatisticsResponse(
        notificationQueryUseCases.statistics(criteria));
  }
}
