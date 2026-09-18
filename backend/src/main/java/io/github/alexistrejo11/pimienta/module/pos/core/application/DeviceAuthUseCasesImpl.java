package io.github.alexistrejo11.pimienta.module.pos.core.application;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.Headquarter;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.PosOperationalConfig;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.exception.HeadquarterNotFoundException;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterRepository;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.PosOperationalConfigRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.application.command.EnrollDeviceCommand;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosDevice;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosEnrollmentCode;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosDeviceStatus;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.exception.PosDeviceAlreadyEnrolledException;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.exception.PosDeviceNotFoundException;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.exception.PosDeviceRevokedException;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.exception.PosEnrollmentCodeConsumedException;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.exception.PosEnrollmentCodeExpiredException;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.exception.PosEnrollmentCodeInvalidException;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.DeviceAuthUseCases;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.DeviceTokenIssuer;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.DeviceTokenIssuer.DeviceIssuedTokens;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosDeviceRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosEnrollmentCodeRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceAuthUseCasesImpl implements DeviceAuthUseCases {

  private static final Map<String, Integer> DEFAULT_EVENT_SCHEMA_VERSIONS =
      Map.of(
          "SALE_CONFIRMED", 1,
          "SHIFT_CLOSED", 1,
          "SHIFT_OPENED", 1,
          "SALE_CANCELLED", 1,
          "WASTE_RECORDED", 1,
          "RESTOCK_RECORDED", 1,
          "CASH_WITHDRAWAL_RECORDED", 1,
          "CASH_COUNT_SUBMITTED", 1,
          "OVERRIDE_AUTHORIZED", 1);

  private final PosEnrollmentCodeRepository enrollmentCodeRepository;
  private final PosDeviceRepository deviceRepository;
  private final HeadquarterRepository headquarterRepository;
  private final PosOperationalConfigRepository posOperationalConfigRepository;
  private final DeviceTokenIssuer deviceTokenIssuer;

  public DeviceAuthUseCasesImpl(
      PosEnrollmentCodeRepository enrollmentCodeRepository,
      PosDeviceRepository deviceRepository,
      HeadquarterRepository headquarterRepository,
      PosOperationalConfigRepository posOperationalConfigRepository,
      DeviceTokenIssuer deviceTokenIssuer) {
    this.enrollmentCodeRepository = enrollmentCodeRepository;
    this.deviceRepository = deviceRepository;
    this.headquarterRepository = headquarterRepository;
    this.posOperationalConfigRepository = posOperationalConfigRepository;
    this.deviceTokenIssuer = deviceTokenIssuer;
  }

  @Override
  @Transactional
  public EnrollResult enroll(EnrollDeviceCommand command) {
    String enrollmentCode = normalizeEnrollmentCode(command.enrollmentCode());
    if (enrollmentCode == null) {
      throw new PosEnrollmentCodeInvalidException();
    }
    if (command.devicePublicId() == null) {
      throw new PosEnrollmentCodeInvalidException();
    }

    PosEnrollmentCode code =
        enrollmentCodeRepository
            .findByCode(enrollmentCode)
            .orElseThrow(PosEnrollmentCodeInvalidException::new);

    LocalDateTime now = LocalDateTime.now();
    if (code.isConsumed()) {
      throw new PosEnrollmentCodeConsumedException(code.getCode());
    }
    if (code.isExpired(now)) {
      throw new PosEnrollmentCodeExpiredException(code.getCode());
    }

    Headquarter hq =
        headquarterRepository
            .findById(code.getHeadquarterId())
            .orElseThrow(() -> new HeadquarterNotFoundException(code.getHeadquarterId()));

    PosDevice existing = deviceRepository.findById(command.devicePublicId()).orElse(null);
    if (existing != null && existing.isAuthorized()) {
      throw new PosDeviceAlreadyEnrolledException(command.devicePublicId());
    }

    String visibleCode = nextVisibleCode(code.getHeadquarterId());
    PosDevice device;
    if (existing != null) {
      existing.setDeviceName(command.deviceName());
      existing.setAppVersion(command.appVersion());
      existing.setVisibleCode(visibleCode);
      existing.authorize();
      device = deviceRepository.save(existing);
    } else {
      device =
          deviceRepository.save(
              PosDevice.builder()
                  .withId(command.devicePublicId())
                  .withHeadquarterId(code.getHeadquarterId())
                  .withVisibleCode(visibleCode)
                  .withDeviceName(command.deviceName())
                  .withAppVersion(command.appVersion())
                  .withStatus(PosDeviceStatus.AUTHORIZED)
                  .withMinAppVersion("1.0.0")
                  .register());
    }

    code.consume(device.getId(), now);
    enrollmentCodeRepository.save(code);

    DeviceIssuedTokens tokens = deviceTokenIssuer.issuePair(device);
    return new EnrollResult(device, tokens, toSiteSummary(hq));
  }

  private static String normalizeEnrollmentCode(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.replaceAll("\\s", "");
    return normalized.matches("\\d{6}") ? normalized : null;
  }

  @Override
  @Transactional
  public DeviceIssuedTokens refresh(String refreshToken) {
    return deviceTokenIssuer.refresh(refreshToken);
  }

  @Override
  @Transactional(readOnly = true)
  public DeviceMeResult me(UUID deviceId) {
    PosDevice device =
        deviceRepository.findById(deviceId).orElseThrow(() -> new PosDeviceNotFoundException(deviceId));
    if (device.isRevoked()) {
      throw new PosDeviceRevokedException(deviceId);
    }
    Headquarter hq =
        headquarterRepository
            .findById(device.getHeadquarterId())
            .orElseThrow(() -> new HeadquarterNotFoundException(device.getHeadquarterId()));
    return new DeviceMeResult(device, toSiteSummary(hq), new LinkedHashMap<>(DEFAULT_EVENT_SCHEMA_VERSIONS));
  }

  private SiteSummary toSiteSummary(Headquarter hq) {
    String currency =
        posOperationalConfigRepository
            .findByHeadquarterId(hq.getId())
            .map(PosOperationalConfig::getCurrency)
            .filter(c -> c != null && !c.isBlank())
            .orElse("MXN");
    return new SiteSummary(hq.getId(), hq.getName(), hq.getAddress(), currency);
  }

  private String nextVisibleCode(long headquarterId) {
    long count =
        deviceRepository.countByHeadquarterIdAndStatusNot(headquarterId, PosDeviceStatus.REVOKED);
    return "T" + (count + 1);
  }
}
