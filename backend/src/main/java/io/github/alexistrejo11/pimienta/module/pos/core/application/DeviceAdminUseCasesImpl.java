package io.github.alexistrejo11.pimienta.module.pos.core.application;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosDevice;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.exception.PosDeviceNotFoundException;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.DeviceAdminUseCases;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.DeviceTokenIssuer;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosDeviceRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceAdminUseCasesImpl implements DeviceAdminUseCases {

  private final PosDeviceRepository deviceRepository;
  private final DeviceTokenIssuer deviceTokenIssuer;

  public DeviceAdminUseCasesImpl(
      PosDeviceRepository deviceRepository, DeviceTokenIssuer deviceTokenIssuer) {
    this.deviceRepository = deviceRepository;
    this.deviceTokenIssuer = deviceTokenIssuer;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<PosDevice> list(Long headquarterId, Pageable pageable) {
    if (headquarterId != null) {
      return deviceRepository.findByHeadquarterId(headquarterId, pageable);
    }
    return deviceRepository.findAll(pageable);
  }

  @Override
  @Transactional
  public PosDevice revoke(UUID deviceId) {
    PosDevice device =
        deviceRepository.findById(deviceId).orElseThrow(() -> new PosDeviceNotFoundException(deviceId));
    device.revoke();
    PosDevice saved = deviceRepository.save(device);
    deviceTokenIssuer.revokeRefreshForDevice(deviceId);
    return saved;
  }
}
