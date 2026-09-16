package io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosDevice;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.exception.PosDeviceNotFoundException;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosDeviceRepository;
import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.dto.PosHealthSnapshotResponse;
import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.dto.PosTelemetryActivityResponse;
import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.dto.PosTelemetryDashboardResponse;
import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.dto.PosTelemetryDeviceResponse;
import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.output.persistence.PosHealthSnapshotRepository;
import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.output.persistence.entity.PosHealthSnapshotJpaEntity;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class TelemetryObservabilityService {
  // WorkManager reports health every 15 minutes; allow three missed reports.
  private static final Duration ONLINE_WINDOW = Duration.ofMinutes(45);
  private final PosHealthSnapshotRepository snapshotRepository;
  private final PosDeviceRepository deviceRepository;

  public TelemetryObservabilityService(PosHealthSnapshotRepository snapshotRepository,
      PosDeviceRepository deviceRepository) {
    this.snapshotRepository = snapshotRepository;
    this.deviceRepository = deviceRepository;
  }

  public PosTelemetryDeviceResponse device(UUID id, Pageable pageable) {
    PosDevice device = deviceRepository.findById(id).orElseThrow(() -> new PosDeviceNotFoundException(id));
    List<PosHealthSnapshotJpaEntity> rows = snapshotRepository.findByDeviceId(id, pageable);
    var items = rows.stream().map(TelemetryObservabilityService::response).toList();
    var page = new PageImpl<>(items, pageable, snapshotRepository.countByDeviceId(id));
    return new PosTelemetryDeviceResponse(device.getId(), device.getHeadquarterId(), device.getVisibleCode(),
        device.getDeviceName(), device.getStatus().name(), device.getAppVersion(),
        items.isEmpty() ? null : items.getFirst(), PagedResponse.of(page));
  }

  public PosTelemetryDashboardResponse dashboard(Long hq) {
    PosFleetSnapshot fleet = fleet(hq);
    return new PosTelemetryDashboardResponse(fleet.totalDevices(), fleet.onlineDevices(), fleet.degradedDevices(),
        fleet.offlineDevices(), fleet.devicesWithoutSnapshot(), fleet.pendingEvents(), fleet.openStaleDevices());
  }

  /** Bounded fleet totals for Prometheus gauges; never includes device or user identifiers. */
  public PosFleetSnapshot fleet(Long hq) {
    List<PosDevice> devices = hq == null
        ? deviceRepository.findAll(Pageable.ofSize(500)).getContent()
        : deviceRepository.findByHeadquarterId(hq, Pageable.ofSize(500)).getContent();
    var latest = snapshotRepository.findLatest(hq).stream().collect(java.util.stream.Collectors.toMap(
        io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.output.persistence.repository.PosHealthSnapshotSpringDataRepository.LatestHealthProjection::getDeviceId, x -> x));
    long online = 0, degraded = 0, offline = 0, pending = 0, maxAge = 0;
    Instant cutoff = Instant.now().minus(ONLINE_WINDOW);
    for (PosDevice device : devices) {
      var snapshot = latest.get(device.getId());
      if (snapshot == null || snapshot.getReceivedAt().isBefore(cutoff)) { offline++; continue; }
      pending += snapshot.getPendingEvents();
      maxAge = Math.max(maxAge, snapshot.getOldestPendingAgeSeconds());
      if ("ONLINE".equals(snapshot.getSyncState()) && snapshot.getPendingEvents() == 0) online++;
      else degraded++;
    }
    return new PosFleetSnapshot(devices.size(), online, degraded, offline,
        devices.size() - latest.size(), pending, offline, maxAge);
  }

  public record PosFleetSnapshot(long totalDevices, long onlineDevices, long degradedDevices, long offlineDevices,
      long devicesWithoutSnapshot, long pendingEvents, long openStaleDevices, long maxOldestPendingAgeSeconds) {}

  public PosTelemetryActivityResponse activity(Long hq, String cursor, int limit) {
    Cursor parsed = decode(cursor);
    int requested = Math.max(1, Math.min(limit, 100));
    List<PosHealthSnapshotJpaEntity> rows = snapshotRepository.findActivity(
        hq, parsed.at(), parsed.id(), Pageable.ofSize(requested + 1));
    boolean more = rows.size() > requested;
    List<PosHealthSnapshotJpaEntity> visible = more ? rows.subList(0, requested) : rows;
    String next = more ? encode(visible.getLast().getReceivedAt(), visible.getLast().getId()) : null;
    return new PosTelemetryActivityResponse(visible.stream().map(TelemetryObservabilityService::response).toList(), next, more);
  }

  private static PosHealthSnapshotResponse response(PosHealthSnapshotJpaEntity row) {
    return new PosHealthSnapshotResponse(row.getId(), row.getDeviceId(), row.getHeadquarterId(), row.getSyncState(),
        row.getPendingEvents(), row.getOldestPendingAgeSeconds(), row.getAppVersion(), row.getReceivedAt());
  }

  private static String encode(Instant at, UUID id) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString((at + "|" + id).getBytes(StandardCharsets.UTF_8));
  }

  private static Cursor decode(String value) {
    if (value == null || value.isBlank()) return new Cursor(null, null);
    try {
      String[] parts = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8).split("\\|", 2);
      return new Cursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
    } catch (RuntimeException ex) {
      throw new IllegalArgumentException("Invalid telemetry cursor");
    }
  }

  private record Cursor(Instant at, UUID id) {}
}
