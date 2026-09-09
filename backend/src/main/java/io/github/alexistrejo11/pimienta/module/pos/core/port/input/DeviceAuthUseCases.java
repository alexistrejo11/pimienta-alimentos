package io.github.alexistrejo11.pimienta.module.pos.core.port.input;

import io.github.alexistrejo11.pimienta.module.pos.core.application.command.EnrollDeviceCommand;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosDevice;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.DeviceTokenIssuer.DeviceIssuedTokens;
import java.util.Map;
import java.util.UUID;

public interface DeviceAuthUseCases {

  record EnrollResult(PosDevice device, DeviceIssuedTokens tokens, SiteSummary site) {}

  record SiteSummary(Long id, String name, String address, String currency) {}

  record DeviceMeResult(
      PosDevice device, SiteSummary site, Map<String, Integer> eventSchemaVersions) {}

  EnrollResult enroll(EnrollDeviceCommand command);

  DeviceIssuedTokens refresh(String refreshToken);

  DeviceMeResult me(UUID deviceId);
}
