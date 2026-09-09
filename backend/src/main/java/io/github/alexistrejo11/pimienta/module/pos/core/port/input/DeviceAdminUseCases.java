package io.github.alexistrejo11.pimienta.module.pos.core.port.input;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosDevice;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DeviceAdminUseCases {

  Page<PosDevice> list(Long headquarterId, Pageable pageable);

  PosDevice revoke(UUID deviceId);
}
